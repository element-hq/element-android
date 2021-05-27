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

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.call.CallIdGenerator
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidate
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallCapabilities
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallReplacesContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallSignalingContent
import org.matrix.android.sdk.api.session.room.model.call.SdpType
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.call.DefaultCallSignalingService
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import timber.log.Timber
import java.math.BigDecimal

internal class MxCallImpl(
        override val callId: String,
        override val isOutgoing: Boolean,
        override val roomId: String,
        private val userId: String,
        override val isVideoCall: Boolean,
        override val ourPartyId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
        private val matrixConfiguration: MatrixConfiguration,
        private val getProfileInfoTask: GetProfileInfoTask
) : MxCall {

    override var opponentPartyId: Optional<String>? = null
    override var opponentVersion: Int = MxCall.VOIP_PROTO_VERSION
    override lateinit var opponentUserId: String
    override var capabilities: CallCapabilities? = null

    fun updateOpponentData(userId: String, content: CallSignalingContent, callCapabilities: CallCapabilities?) {
        opponentPartyId = Optional.from(content.partyId)
        opponentVersion = content.version?.let { BigDecimal(it).intValueExact() } ?: MxCall.VOIP_PROTO_VERSION
        opponentUserId = userId
        capabilities = callCapabilities ?: CallCapabilities()
    }

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

    override fun offerSdp(sdpString: String) {
        if (!isOutgoing) return
        Timber.v("## VOIP offerSdp $callId")
        state = CallState.Dialing
        CallInviteContent(
                callId = callId,
                partyId = ourPartyId,
                lifetime = DefaultCallSignalingService.CALL_TIMEOUT_MS,
                offer = CallInviteContent.Offer(sdp = sdpString),
                version = MxCall.VOIP_PROTO_VERSION.toString(),
                capabilities = buildCapabilities()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_INVITE, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun sendLocalCallCandidates(candidates: List<CallCandidate>) {
        Timber.v("Send local call canditates $callId: $candidates")
        CallCandidatesContent(
                callId = callId,
                partyId = ourPartyId,
                candidates = candidates,
                version = MxCall.VOIP_PROTO_VERSION.toString()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_CANDIDATES, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun sendLocalIceCandidateRemovals(candidates: List<CallCandidate>) {
        // For now we don't support this flow
    }

    override fun reject() {
        if (opponentVersion < 1) {
            Timber.v("Opponent version is less than 1 ($opponentVersion): sending hangup instead of reject")
            hangUp()
            return
        }
        Timber.v("## VOIP reject $callId")
        CallRejectContent(
                callId = callId,
                partyId = ourPartyId,
                version = MxCall.VOIP_PROTO_VERSION.toString()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_REJECT, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
        state = CallState.Terminated
    }

    override fun hangUp(reason: CallHangupContent.Reason?) {
        Timber.v("## VOIP hangup $callId")
        CallHangupContent(
                callId = callId,
                partyId = ourPartyId,
                reason = reason ?: CallHangupContent.Reason.USER_HANGUP,
                version = MxCall.VOIP_PROTO_VERSION.toString()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_HANGUP, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
        state = CallState.Terminated
    }

    override fun accept(sdpString: String) {
        Timber.v("## VOIP accept $callId")
        if (isOutgoing) return
        state = CallState.Answering
        CallAnswerContent(
                callId = callId,
                partyId = ourPartyId,
                answer = CallAnswerContent.Answer(sdp = sdpString),
                version = MxCall.VOIP_PROTO_VERSION.toString(),
                capabilities = buildCapabilities()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_ANSWER, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun negotiate(sdpString: String, type: SdpType) {
        Timber.v("## VOIP negotiate $callId")
        CallNegotiateContent(
                callId = callId,
                partyId = ourPartyId,
                lifetime = DefaultCallSignalingService.CALL_TIMEOUT_MS,
                description = CallNegotiateContent.Description(sdp = sdpString, type = type),
                version = MxCall.VOIP_PROTO_VERSION.toString()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_NEGOTIATE, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override fun selectAnswer() {
        Timber.v("## VOIP select answer $callId")
        if (isOutgoing) return
        state = CallState.Answering
        CallSelectAnswerContent(
                callId = callId,
                partyId = ourPartyId,
                selectedPartyId = opponentPartyId?.getOrNull(),
                version = MxCall.VOIP_PROTO_VERSION.toString()
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_SELECT_ANSWER, roomId = roomId, content = it.toContent()) }
                .also { eventSenderProcessor.postEvent(it) }
    }

    override suspend fun transfer(targetUserId: String,
                                  targetRoomId: String?,
                                  createCallId: String?,
                                  awaitCallId: String?) {
        val profileInfoParams = GetProfileInfoTask.Params(targetUserId)
        val profileInfo = try {
            getProfileInfoTask.execute(profileInfoParams)
        } catch (failure: Throwable) {
            Timber.v("Fail fetching profile info of $targetUserId while transferring call")
            null
        }
        CallReplacesContent(
                callId = callId,
                partyId = ourPartyId,
                replacementId = CallIdGenerator.generate(),
                version = MxCall.VOIP_PROTO_VERSION.toString(),
                targetUser = CallReplacesContent.TargetUser(
                        id = targetUserId,
                        displayName = profileInfo?.get(ProfileService.DISPLAY_NAME_KEY) as? String,
                        avatarUrl = profileInfo?.get(ProfileService.AVATAR_URL_KEY) as? String
                ),
                targetRoomId = targetRoomId,
                awaitCall = awaitCallId,
                createCall = createCallId
        )
                .let { createEventAndLocalEcho(type = EventType.CALL_REPLACES, roomId = roomId, content = it.toContent()) }
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

    private fun buildCapabilities(): CallCapabilities? {
        return if (matrixConfiguration.supportsCallTransfer) {
            CallCapabilities(true)
        } else {
            null
        }
    }
}
