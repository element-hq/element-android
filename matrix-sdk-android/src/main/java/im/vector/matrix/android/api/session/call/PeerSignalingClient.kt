// /*
// * Copyright (c) 2020 New Vector Ltd
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package im.vector.matrix.android.api.session.call
//
// import im.vector.matrix.android.api.MatrixCallback
// import org.webrtc.IceCandidate
// import org.webrtc.SessionDescription
//
// interface PeerSignalingClient {
//
//    val callID: String
//
//    fun addListener(listener: SignalingListener)
//
//    /**
//     * Send offer SDP to the other participant.
//     */
//    fun sendOfferSdp(sdp: SessionDescription, callback: MatrixCallback<String>)
//
//    /**
//     * Send answer SDP to the other participant.
//     */
//    fun sendAnswerSdp(sdp: SessionDescription, callback: MatrixCallback<String>)
//
//    /**
//     * Send Ice candidate to the other participant.
//     */
//    fun sendLocalIceCandidates(candidates: List<IceCandidate>)
//
//    /**
//     * Send removed ICE candidates to the other participant.
//     */
//    fun sendLocalIceCandidateRemovals(candidates: List<IceCandidate>)
//
//
//    interface SignalingListener {
//        /**
//         * Callback fired once remote SDP is received.
//         */
//        fun onRemoteDescription(sdp: SessionDescription)
//
//        /**
//         * Callback fired once remote Ice candidate is received.
//         */
//        fun onRemoteIceCandidate(candidate: IceCandidate)
//
//        /**
//         * Callback fired once remote Ice candidate removals are received.
//         */
//        fun onRemoteIceCandidatesRemoved(candidates: List<IceCandidate>)
//    }
// }
