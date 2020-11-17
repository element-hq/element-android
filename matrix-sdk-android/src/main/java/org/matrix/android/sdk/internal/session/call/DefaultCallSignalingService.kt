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
import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallSignallingContent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.call.model.MxCallImpl
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@SessionScope
internal class DefaultCallSignalingService @Inject constructor(
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        private val activeCallHandler: ActiveCallHandler,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
        private val taskExecutor: TaskExecutor,
        private val turnServerTask: GetTurnServerTask
) : CallSignalingService {

    private val callListeners = mutableSetOf<CallListener>()
    private val callListenersDispatcher = CallListenersDispatcher(callListeners)

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
                ourPartyId = deviceId ?: "",
                opponentUserId = otherUserId,
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor
        )
        activeCallHandler.addCall(call).also {
            return call
        }
    }

    override fun addCallListener(listener: CallListener) {
        callListeners.add(listener)
    }

    override fun removeCallListener(listener: CallListener) {
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
            EventType.CALL_ANSWER -> {
                handleCallAnswerEvent(event)
            }
            EventType.CALL_INVITE -> {
                handleCallInviteEvent(event)
            }
            EventType.CALL_HANGUP -> {
                handleCallHangupEvent(event)
            }
            EventType.CALL_REJECT -> {
                handleCallRejectEvent(event)
            }
            EventType.CALL_CANDIDATES -> {
                handleCallCandidatesEvent(event)
            }
            EventType.CALL_SELECT_ANSWER -> {
                handleCallSelectAnswerEvent(event)
            }
            EventType.CALL_NEGOTIATE -> {
                handleCallNegotiateEvent(event)
            }
        }
    }

    private fun handleCallNegotiateEvent(event: Event) {
        val content = event.getClearContent().toModel<CallSelectAnswerContent>() ?: return
        val call = content.getCall() ?: return
        if (call.ourPartyId == content.partyId) {
            // Ignore remote echo
            return
        }
        callListenersDispatcher.onCallSelectAnswerReceived(content)
    }

    private fun handleCallSelectAnswerEvent(event: Event) {
        val content = event.getClearContent().toModel<CallSelectAnswerContent>() ?: return
        val call = content.getCall() ?: return
        if (call.ourPartyId == content.partyId) {
            // Ignore remote echo
            return
        }
        if (call.isOutgoing) {
            Timber.v("Got selectAnswer for an outbound call: ignoring")
            return
        }
        val selectedPartyId = content.selectedPartyId
        if (selectedPartyId == null) {
            Timber.w("Got nonsensical select_answer with null selected_party_id: ignoring")
            return
        }
        callListenersDispatcher.onCallSelectAnswerReceived(content)
    }

    private fun handleCallCandidatesEvent(event: Event) {
        val content = event.getClearContent().toModel<CallCandidatesContent>() ?: return
        val call = content.getCall() ?: return
        if (call.ourPartyId == content.partyId) {
            // Ignore remote echo
            return
        }
        if (call.opponentPartyId != Optional.from(content.partyId)) {
            Timber.v("Ignoring candidates from party ID ${content.partyId} we have chosen party ID ${call.opponentPartyId}")
            return
        }
        callListenersDispatcher.onCallIceCandidateReceived(call, content)
    }

    private fun handleCallRejectEvent(event: Event) {
        val content = event.getClearContent().toModel<CallRejectContent>() ?: return
        val call = content.getCall() ?: return
        activeCallHandler.removeCall(content.callId)
        // No need to check party_id for reject because if we'd received either
        // an answer or reject, we wouldn't be in state InviteSent
        if (call.state != CallState.Dialing) {
            return
        }
        callListenersDispatcher.onCallRejectReceived(content)
    }

    private fun handleCallHangupEvent(event: Event) {
        val content = event.getClearContent().toModel<CallHangupContent>() ?: return
        val call = content.getCall() ?: return
        if (call.state != CallState.Terminated) {
            // Need to check for party_id?
            activeCallHandler.removeCall(content.callId)
            callListenersDispatcher.onCallHangupReceived(content)
        }
    }

    private fun handleCallInviteEvent(event: Event) {
        val content = event.getClearContent().toModel<CallInviteContent>() ?: return
        if (content.partyId == deviceId) {
            // Ignore remote echo
            return
        }
        val incomingCall = MxCallImpl(
                callId = content.callId ?: return,
                isOutgoing = false,
                roomId = event.roomId ?: return,
                userId = userId,
                ourPartyId = deviceId ?: "",
                opponentUserId = event.senderId ?: return,
                isVideoCall = content.isVideo(),
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor
        ).apply {
            opponentPartyId = Optional.from(content.partyId)
            opponentVersion = content.version?.let { BigDecimal(it).intValueExact() } ?: MxCall.VOIP_PROTO_VERSION
        }
        activeCallHandler.addCall(incomingCall)
        callListenersDispatcher.onCallInviteReceived(incomingCall, content)
    }

    private fun handleCallAnswerEvent(event: Event) {
        val content = event.getClearContent().toModel<CallAnswerContent>() ?: return
        val call = content.getCall() ?: return
        if (call.ourPartyId == content.partyId) {
            // Ignore remote echo
            return
        }
        if (event.senderId == userId) {
            // discard current call, it's answered by another of my session
            callListenersDispatcher.onCallManagedByOtherSession(content.callId)
        } else {
            if (call.opponentPartyId != null) {
                Timber.v("Ignoring answer from party ID ${content.partyId} we already have an answer from ${call.opponentPartyId}")
                return
            }
            call.apply {
                opponentPartyId = Optional.from(content.partyId)
                opponentVersion = content.version?.let { BigDecimal(it).intValueExact() } ?: MxCall.VOIP_PROTO_VERSION
            }
            callListenersDispatcher.onCallAnswerReceived(content)
        }
    }

    private fun CallSignallingContent.getCall(): MxCall? {
        val currentCall = callId?.let {
            activeCallHandler.getCallWithId(it)
        }
        if (currentCall == null) {
            Timber.v("Call for content: $this is null")
        }
        return currentCall
    }

    companion object {
        const val CALL_TIMEOUT_MS = 120_000
    }
}
