/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.call

import android.os.SystemClock
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.CallsListener
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.call.model.MxCallImpl
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.RoomEventSender
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@SessionScope
internal class DefaultCallSignalingService @Inject constructor(
        @UserId
        private val userId: String,
        private val activeCallHandler: ActiveCallHandler,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val roomEventSender: RoomEventSender,
        private val taskExecutor: TaskExecutor,
        private val turnServerTask: GetTurnServerTask
) : CallSignalingService {

    private val callListeners = mutableSetOf<CallsListener>()

    private val cachedTurnServerResponse = object {
        // Keep one minute safe to avoid considering the data is valid and then actually it is not when effectively using it.
        private val MIN_TTL = 60

        private val now = { SystemClock.elapsedRealtime() / 1000 }

        private var expiresAt: Long = 0

        var data: TurnServerResponse? = null
            get() = if (expiresAt > now()) field else null
            set(value) {
                expiresAt = now() + (value?.ttl ?: 0) - MIN_TTL
                field = value
            }
    }

    override fun getTurnServer(callback: MatrixCallback<TurnServerResponse>): Cancelable {
        if (cachedTurnServerResponse.data != null) {
            cachedTurnServerResponse.data?.let { callback.onSuccess(it) }
            return NoOpCancellable
        }
        return turnServerTask
                .configureWith(GetTurnServerTask.Params) {
                    this.callback = object : MatrixCallback<TurnServerResponse> {
                        override fun onSuccess(data: TurnServerResponse) {
                            cachedTurnServerResponse.data = data
                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun createOutgoingCall(roomId: String, otherUserId: String, isVideoCall: Boolean): MxCall {
        val call = MxCallImpl(
                callId = UUID.randomUUID().toString(),
                isOutgoing = true,
                roomId = roomId,
                userId = userId,
                otherUserId = otherUserId,
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                roomEventSender = roomEventSender
        )
        activeCallHandler.addCall(call).also {
            return call
        }
    }

    override fun addCallListener(listener: CallsListener) {
        callListeners.add(listener)
    }

    override fun removeCallListener(listener: CallsListener) {
        callListeners.remove(listener)
    }

    override fun getCallWithId(callId: String): MxCall? {
        Timber.v("## VOIP getCallWithId $callId all calls ${activeCallHandler.getActiveCallsLiveData().value?.map { it.callId }}")
        return activeCallHandler.getCallWithId(callId)
    }

    override fun isThereAnyActiveCall(): Boolean {
        return activeCallHandler.getActiveCallsLiveData().value?.isNotEmpty() == true
    }

    internal fun onCallEvent(event: Event) {
        when (event.getClearType()) {
            EventType.CALL_ANSWER     -> {
                event.getClearContent().toModel<CallAnswerContent>()?.let {
                    if (event.senderId == userId) {
                        // ok it's an answer from me.. is it remote echo or other session
                        val knownCall = getCallWithId(it.callId)
                        if (knownCall == null) {
                            Timber.d("## VOIP onCallEvent ${event.getClearType()} id ${it.callId} send by me")
                        } else if (!knownCall.isOutgoing) {
                            // incoming call
                            // if it was anwsered by this session, the call state would be in Answering(or connected) state
                            if (knownCall.state == CallState.LocalRinging) {
                                // discard current call, it's answered by another of my session
                                onCallManageByOtherSession(it.callId)
                            }
                        }
                        return
                    }

                    onCallAnswer(it)
                }
            }
            EventType.CALL_INVITE     -> {
                if (event.senderId == userId) {
                    // Always ignore local echos of invite
                    return
                }

                event.getClearContent().toModel<CallInviteContent>()?.let { content ->
                    val incomingCall = MxCallImpl(
                            callId = content.callId ?: return@let,
                            isOutgoing = false,
                            roomId = event.roomId ?: return@let,
                            userId = userId,
                            otherUserId = event.senderId ?: return@let,
                            isVideoCall = content.isVideo(),
                            localEchoEventFactory = localEchoEventFactory,
                            roomEventSender = roomEventSender
                    )
                    activeCallHandler.addCall(incomingCall)
                    onCallInvite(incomingCall, content)
                }
            }
            EventType.CALL_HANGUP     -> {
                event.getClearContent().toModel<CallHangupContent>()?.let { content ->

                    if (event.senderId == userId) {
                        // ok it's an answer from me.. is it remote echo or other session
                        val knownCall = getCallWithId(content.callId)
                        if (knownCall == null) {
                            Timber.d("## VOIP onCallEvent ${event.getClearType()} id ${content.callId} send by me")
                        } else if (!knownCall.isOutgoing) {
                            // incoming call
                            if (knownCall.state == CallState.LocalRinging) {
                                // discard current call, it's answered by another of my session
                                onCallManageByOtherSession(content.callId)
                            }
                        }
                        return
                    }

                    activeCallHandler.removeCall(content.callId)
                    onCallHangup(content)
                }
            }
            EventType.CALL_CANDIDATES -> {
                if (event.senderId == userId) {
                    // Always ignore local echos of invite
                    return
                }
                event.getClearContent().toModel<CallCandidatesContent>()?.let { content ->
                    activeCallHandler.getCallWithId(content.callId)?.let {
                        onCallIceCandidate(it, content)
                    }
                }
            }
        }
    }

    private fun onCallHangup(hangup: CallHangupContent) {
        callListeners.toList().forEach {
            tryOrNull {
                it.onCallHangupReceived(hangup)
            }
        }
    }

    private fun onCallAnswer(answer: CallAnswerContent) {
        callListeners.toList().forEach {
            tryOrNull {
                it.onCallAnswerReceived(answer)
            }
        }
    }

    private fun onCallManageByOtherSession(callId: String) {
        callListeners.toList().forEach {
            tryOrNull {
                it.onCallManagedByOtherSession(callId)
            }
        }
    }

    private fun onCallInvite(incomingCall: MxCall, invite: CallInviteContent) {
        // Ignore the invitation from current user
        if (incomingCall.otherUserId == userId) return

        callListeners.toList().forEach {
            tryOrNull {
                it.onCallInviteReceived(incomingCall, invite)
            }
        }
    }

    private fun onCallIceCandidate(incomingCall: MxCall, candidates: CallCandidatesContent) {
        callListeners.toList().forEach {
            tryOrNull {
                it.onCallIceCandidateReceived(incomingCall, candidates)
            }
        }
    }

    companion object {
        const val CALL_TIMEOUT_MS = 120_000
    }
}
