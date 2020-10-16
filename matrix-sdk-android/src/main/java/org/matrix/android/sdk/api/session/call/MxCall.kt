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

package org.matrix.android.sdk.api.session.call

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface MxCallDetail {
    val callId: String
    val isOutgoing: Boolean
    val roomId: String
    val otherUserId: String
    val isVideoCall: Boolean
}

/**
 * Define both an incoming call and on outgoing call
 */
interface MxCall : MxCallDetail {

    var state: CallState

    /**
     * Pick Up the incoming call
     * It has no effect on outgoing call
     */
    fun accept(sdp: SessionDescription)

    /**
     * Reject an incoming call
     * It's an alias to hangUp
     */
    fun reject() = hangUp()

    /**
     * End the call
     */
    fun hangUp()

    /**
     * Start a call
     * Send offer SDP to the other participant.
     */
    fun offerSdp(sdp: SessionDescription)

    /**
     * Send Ice candidate to the other participant.
     */
    fun sendLocalIceCandidates(candidates: List<IceCandidate>)

    /**
     * Send removed ICE candidates to the other participant.
     */
    fun sendLocalIceCandidateRemovals(candidates: List<IceCandidate>)

    fun addListener(listener: StateListener)
    fun removeListener(listener: StateListener)

    interface StateListener {
        fun onStateUpdate(call: MxCall)
    }
}
