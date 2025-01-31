/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call

import org.matrix.android.sdk.api.session.room.model.call.CallCandidate
import org.matrix.android.sdk.api.session.room.model.call.CallCapabilities
import org.matrix.android.sdk.api.session.room.model.call.EndCallReason
import org.matrix.android.sdk.api.session.room.model.call.SdpType
import org.matrix.android.sdk.api.util.Optional

interface MxCallDetail {
    val callId: String
    val isOutgoing: Boolean
    val roomId: String
    val isVideoCall: Boolean
    val ourPartyId: String
    val opponentPartyId: Optional<String>?
    val opponentVersion: Int
    val opponentUserId: String
    val capabilities: CallCapabilities?
}

/**
 * Define both an incoming call and on outgoing call.
 */
interface MxCall : MxCallDetail {

    companion object {
        const val VOIP_PROTO_VERSION = 1
    }

    var state: CallState

    /**
     * Pick Up the incoming call.
     * It has no effect on outgoing call.
     */
    fun accept(sdpString: String)

    /**
     * SDP negotiation for media pause, hold/resume, ICE restarts and voice/video call up/downgrading.
     */
    fun negotiate(sdpString: String, type: SdpType)

    /**
     * This has to be sent by the caller's client once it has chosen an answer.
     */
    fun selectAnswer()

    /**
     * Reject an incoming call.
     */
    fun reject()

    /**
     * End the call.
     */
    fun hangUp(reason: EndCallReason? = null)

    /**
     * Start a call.
     * Send offer SDP to the other participant.
     */
    fun offerSdp(sdpString: String)

    /**
     * Send Call candidate to the other participant.
     */
    fun sendLocalCallCandidates(candidates: List<CallCandidate>)

    /**
     * Send removed ICE candidates to the other participant.
     */
    fun sendLocalIceCandidateRemovals(candidates: List<CallCandidate>)

    /**
     * Send a m.call.replaces event to initiate call transfer.
     * See [org.matrix.android.sdk.api.session.room.model.call.CallReplacesContent] for documentation about the parameters
     */
    suspend fun transfer(
            targetUserId: String,
            targetRoomId: String?,
            createCallId: String?,
            awaitCallId: String?
    )

    fun addListener(listener: StateListener)
    fun removeListener(listener: StateListener)

    interface StateListener {
        fun onStateUpdate(call: MxCall)
    }
}
