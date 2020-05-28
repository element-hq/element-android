/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.call.model

import im.vector.matrix.android.api.session.call.MxCall
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.call.CallAnswerContent
import im.vector.matrix.android.api.session.room.model.call.CallCandidatesContent
import im.vector.matrix.android.api.session.room.model.call.CallHangupContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.matrix.android.internal.session.call.DefaultCallService
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.RoomEventSender
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

internal class MxCallImpl(
        val callId: String,
        override val isOutgoing: Boolean,
        override val roomId: String,
        private val userId: String,
        override val otherUserId: String,
        override val isVideoCall: Boolean,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val roomEventSender: RoomEventSender
) : MxCall {

    override fun offerSdp(sdp: SessionDescription) {
        if (!isOutgoing) return

        CallInviteContent(
                callId = callId,
                lifetime = DefaultCallService.CALL_TIMEOUT_MS,
                offer = CallInviteContent.Offer(sdp = sdp.description)
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = it.toContent()) }
                .also { roomEventSender.sendEvent(it) }
    }

    override fun sendLocalIceCandidates(candidates: List<IceCandidate>) {
        CallCandidatesContent(
                callId = callId,
                candidates = candidates.map {
                    CallCandidatesContent.Candidate(
                            sdpMid = it.sdpMid,
                            sdpMLineIndex = it.sdpMLineIndex.toString(),
                            candidate = it.sdp
                    )
                }
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_CANDIDATES, roomId = roomId, content = it.toContent()) }
                .also { roomEventSender.sendEvent(it) }
    }

    override fun sendLocalIceCandidateRemovals(candidates: List<IceCandidate>) {
    }

    override fun hangUp() {
        CallHangupContent(
                callId = callId
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_HANGUP, roomId = roomId, content = it.toContent()) }
                .also { roomEventSender.sendEvent(it) }
    }

    override fun accept(sdp: SessionDescription) {
        if (isOutgoing) return

        CallAnswerContent(
                callId = callId,
                answer = CallAnswerContent.Answer(sdp = sdp.description)
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = it.toContent()) }
                .also { roomEventSender.sendEvent(it) }
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
