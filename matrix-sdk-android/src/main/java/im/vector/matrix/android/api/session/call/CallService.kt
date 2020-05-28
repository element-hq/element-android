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

package im.vector.matrix.android.api.session.call

import im.vector.matrix.android.api.MatrixCallback
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface CallService {

    fun getTurnServer(callback: MatrixCallback<TurnServer?>)

    /**
     * Send offer SDP to the other participant.
     */
    fun sendOfferSdp(callId: String, roomId: String, sdp: SessionDescription, callback: MatrixCallback<String>)

    /**
     * Send answer SDP to the other participant.
     */
    fun sendAnswerSdp(callId: String, roomId: String, sdp: SessionDescription, callback: MatrixCallback<String>)

    /**
     * Send Ice candidate to the other participant.
     */
    fun sendLocalIceCandidates(callId: String, roomId: String, candidates: List<IceCandidate>)

    /**
     * Send removed ICE candidates to the other participant.
     */
    fun sendLocalIceCandidateRemovals(callId: String, roomId: String, candidates: List<IceCandidate>)

    /**
     * Send a hangup event
     */
    fun sendHangup(callId: String, roomId: String)

    fun addCallListener(listener: CallsListener)

    fun removeCallListener(listener: CallsListener)
}
