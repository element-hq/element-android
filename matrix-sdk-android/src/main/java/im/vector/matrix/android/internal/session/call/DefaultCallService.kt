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

package im.vector.matrix.android.internal.session.call

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.extensions.tryThis
import im.vector.matrix.android.api.session.call.CallService
import im.vector.matrix.android.api.session.call.CallsListener
import im.vector.matrix.android.api.session.call.TurnServer
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.call.CallAnswerContent
import im.vector.matrix.android.api.session.room.model.call.CallCandidatesContent
import im.vector.matrix.android.api.session.room.model.call.CallHangupContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.RoomEventSender
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject

@SessionScope
internal class DefaultCallService @Inject constructor(
        @UserId
        private val userId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val roomEventSender: RoomEventSender
) : CallService {

    private val callListeners = mutableSetOf<CallsListener>()

    override fun getTurnServer(callback: MatrixCallback<TurnServer?>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun isCallSupportedInRoom(roomId: String): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun sendOfferSdp(callId: String, roomId: String, sdp: SessionDescription, callback: MatrixCallback<String>) {
        val eventContent = CallInviteContent(
                callId = callId,
                lifetime = CALL_TIMEOUT_MS,
                offer = CallInviteContent.Offer(sdp = sdp.description)
        )

        createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = eventContent.toContent()).let { event ->
            roomEventSender.sendEvent(event)
//            sendEventTask
//                    .configureWith(
//                            SendEventTask.Params(event = event, cryptoService = cryptoService)
//                    ) {
//                        this.callback = callback
//                    }.executeBy(taskExecutor)
        }
    }

    override fun sendAnswerSdp(callId: String, roomId: String, sdp: SessionDescription, callback: MatrixCallback<String>) {
        val eventContent = CallAnswerContent(
                callId = callId,
                answer = CallAnswerContent.Answer(sdp = sdp.description)
        )

        createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = eventContent.toContent()).let { event ->
            roomEventSender.sendEvent(event)
//            sendEventTask
//                    .configureWith(
//                            SendEventTask.Params(event = event, cryptoService = cryptoService)
//                    ) {
//                        this.callback = callback
//                    }.executeBy(taskExecutor)
        }
    }

    override fun sendLocalIceCandidates(callId: String, roomId: String, candidates: List<IceCandidate>) {
        val eventContent = CallCandidatesContent(
                callId = callId,
                candidates = candidates.map {
                    CallCandidatesContent.Candidate(
                            sdpMid = it.sdpMid,
                            sdpMLineIndex = it.sdpMLineIndex.toString(),
                            candidate = it.sdp
                    )
                }
        )
        createEventAndLocalEcho(type = EventType.CALL_CANDIDATES, roomId = roomId, content = eventContent.toContent()).let { event ->
            roomEventSender.sendEvent(event)
//            sendEventTask
//                    .configureWith(
//                            SendEventTask.Params(event = event, cryptoService = cryptoService)
//                    ) {
//                        this.callback = callback
//                    }.executeBy(taskExecutor)
        }
    }

    override fun sendLocalIceCandidateRemovals(callId: String, roomId: String, candidates: List<IceCandidate>) {
    }

    override fun sendHangup(callId: String, roomId: String) {
        val eventContent = CallHangupContent(callId = callId)
        createEventAndLocalEcho(type = EventType.CALL_HANGUP, roomId = roomId, content = eventContent.toContent()).let { event ->
            roomEventSender.sendEvent(event)
        }
    }

    override fun addCallListener(listener: CallsListener) {
        callListeners.add(listener)
    }

    override fun removeCallListener(listener: CallsListener) {
        callListeners.remove(listener)
    }

    internal fun onCallEvent(event: Event) {
        when (event.getClearType()) {
            EventType.CALL_ANSWER -> {
                event.getClearContent().toModel<CallAnswerContent>()?.let {
                    onCallAnswer(it)
                }
            }
            EventType.CALL_INVITE -> {
                event.getClearContent().toModel<CallInviteContent>()?.let {
                    onCallInvite(event.roomId ?: "", event.senderId ?: "", it)
                }
            }
            EventType.CALL_HANGUP -> {
                event.getClearContent().toModel<CallHangupContent>()?.let {
                    onCallHangup(it)
                }
            }
        }
    }

    private fun onCallHangup(hangup: CallHangupContent) {
        callListeners.toList().forEach {
            tryThis {
                it.onCallHangupReceived(hangup)
            }
        }
    }

    private fun onCallAnswer(answer: CallAnswerContent) {
        callListeners.toList().forEach {
            tryThis {
                it.onCallAnswerReceived(answer)
            }
        }
    }

    private fun onCallInvite(roomId: String, fromUserId: String, invite: CallInviteContent) {
        // Ignore the invitation from current user
        if (fromUserId == userId) return

        callListeners.toList().forEach {
            tryThis {
                it.onCallInviteReceived(roomId, fromUserId, invite)
            }
        }
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
        ).also {
            localEchoEventFactory.createLocalEcho(it)
        }
    }

    companion object {
        const val CALL_TIMEOUT_MS = 120_000
    }

//    internal class PeerSignalingClientFactory @Inject constructor(
//            @UserId private val userId: String,
//            private val localEchoEventFactory: LocalEchoEventFactory,
//            private val sendEventTask: SendEventTask,
//            private val taskExecutor: TaskExecutor,
//            private val cryptoService: CryptoService
//    ) {
//
//        fun create(roomId: String, callId: String): PeerSignalingClient {
//            return RoomPeerSignalingClient(
//                    callID = callId,
//                    roomId = roomId,
//                    userId = userId,
//                    localEchoEventFactory = localEchoEventFactory,
//                    sendEventTask = sendEventTask,
//                    taskExecutor = taskExecutor,
//                    cryptoService = cryptoService
//            )
//        }
//    }
}
