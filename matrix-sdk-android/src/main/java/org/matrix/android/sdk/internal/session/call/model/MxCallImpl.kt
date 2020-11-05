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

package org.matrix.android.sdk.internal.session.call.model

import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.internal.session.call.DefaultCallSignalingService
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber

internal class MxCallImpl(
        override val callId: String,
        override val isOutgoing: Boolean,
        override val roomId: String,
        private val userId: String,
        override val otherUserId: String,
        override val isVideoCall: Boolean,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor
) : MxCall {

    override var state: CallState = CallState.Idle
        set(value) {
            field = value
            dispatchStateChange()
        }

    private val listeners = mutableListOf<MxCall.StateListener>()

    override fun addListener(listener: MxCall.StateListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MxCall.StateListener) {
        listeners.remove(listener)
    }

    private fun dispatchStateChange() {
        listeners.forEach {
            try {
                it.onStateUpdate(this)
            } catch (failure: Throwable) {
                Timber.d("dispatchStateChange failed for call $callId : ${failure.localizedMessage}")
            }
        }
    }

    init {
        if (isOutgoing) {
            state = CallState.Idle
        } else {
            // because it's created on reception of an offer
            state = CallState.LocalRinging
        }
    }

    override fun offerSdp(sdp: SessionDescription) {
        if (!isOutgoing) return
        Timber.v("## VOIP offerSdp $callId")
        state = CallState.Dialing
        CallInviteContent(
                callId = callId,
                lifetime = DefaultCallSignalingService.CALL_TIMEOUT_MS,
                offer = CallInviteContent.Offer(sdp = sdp.description)
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun sendLocalIceCandidates(candidates: List<IceCandidate>) {
        CallCandidatesContent(
                callId = callId,
                candidates = candidates.map {
                    CallCandidatesContent.Candidate(
                            sdpMid = it.sdpMid,
                            sdpMLineIndex = it.sdpMLineIndex,
                            candidate = it.sdp
                    )
                }
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_CANDIDATES, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun sendLocalIceCandidateRemovals(candidates: List<IceCandidate>) {
        // For now we don't support this flow
    }

    override fun hangUp() {
        Timber.v("## VOIP hangup $callId")
        CallHangupContent(
                callId = callId
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_HANGUP, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
        state = CallState.Terminated
    }

    override fun accept(sdp: SessionDescription) {
        Timber.v("## VOIP accept $callId")
        if (isOutgoing) return
        state = CallState.Answering
        CallAnswerContent(
                callId = callId,
                answer = CallAnswerContent.Answer(sdp = sdp.description)
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_ANSWER, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    private fun createEventAndLocalEcho(localId: String = LocalEcho.createLocalEchoId(), type: String, roomId: String, content: Content): Event {
        return Event(
                roomId = roomId,
                originServerTs = System.currentTimeMillis(),
                senderId = userId,
                eventId = localId,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        )
                .also { localEchoEventFactory.createLocalEcho(it) }
    }
}
