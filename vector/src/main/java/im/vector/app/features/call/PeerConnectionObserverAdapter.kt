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

package im.vector.app.features.call

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import timber.log.Timber

abstract class PeerConnectionObserverAdapter : PeerConnection.Observer {
    override fun onIceCandidate(p0: IceCandidate?) {
        Timber.v("## VOIP onIceCandidate $p0")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Timber.v("## VOIP onDataChannel $p0")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Timber.v("## VOIP onIceConnectionReceivingChange $p0")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Timber.v("## VOIP onIceConnectionChange $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Timber.v("## VOIP onIceConnectionChange $p0")
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        Timber.v("## VOIP onAddStream $mediaStream")
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Timber.v("## VOIP onSignalingChange $p0")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Timber.v("## VOIP onIceCandidatesRemoved $p0")
    }

    override fun onRemoveStream(mediaStream: MediaStream?) {
        Timber.v("## VOIP onRemoveStream $mediaStream")
    }

    override fun onRenegotiationNeeded() {
        Timber.v("## VOIP onRenegotiationNeeded")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Timber.v("## VOIP onAddTrack $p0 / out: $p1")
    }
}
