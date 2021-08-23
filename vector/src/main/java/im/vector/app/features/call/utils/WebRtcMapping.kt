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

package im.vector.app.features.call.utils

import org.matrix.android.sdk.api.session.room.model.call.CallCandidate
import org.matrix.android.sdk.api.session.room.model.call.SdpType
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

fun List<IceCandidate>.mapToCallCandidate() = map {
    CallCandidate(
            sdpMid = it.sdpMid,
            sdpMLineIndex = it.sdpMLineIndex,
            candidate = it.sdp
    )
}

fun SdpType.asWebRTC(): SessionDescription.Type {
    return if (this == SdpType.OFFER) {
        SessionDescription.Type.OFFER
    } else {
        SessionDescription.Type.ANSWER
    }
}
