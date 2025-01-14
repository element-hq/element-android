/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.webrtc

import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxPeerConnectionState
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import timber.log.Timber

private val loggerTag = LoggerTag("PeerConnectionObserver", LoggerTag.VOIP)

class PeerConnectionObserver(private val webRtcCall: WebRtcCall) : PeerConnection.Observer {

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        Timber.tag(loggerTag.value).v("StreamObserver onConnectionChange: $newState")
        when (newState) {
            /**
             * Every ICE transport used by the connection is either in use (state "connected" or "completed")
             * or is closed (state "closed"); in addition, at least one transport is either "connected" or "completed"
             */
            PeerConnection.PeerConnectionState.CONNECTED -> {
                webRtcCall.mxCall.state = CallState.Connected(MxPeerConnectionState.CONNECTED)
            }
            /**
             * One or more of the ICE transports on the connection is in the "failed" state.
             */
            PeerConnection.PeerConnectionState.FAILED -> {
                // This can be temporary, e.g when other ice not yet received...
                // webRtcCall.mxCall.state = CallState.ERROR
                webRtcCall.mxCall.state = CallState.Connected(MxPeerConnectionState.FAILED)
            }
            /**
             * At least one of the connection's ICE transports (RTCIceTransports or RTCDtlsTransports) are in the "new" state,
             * and none of them are in one of the following states: "connecting", "checking", "failed", or "disconnected",
             * or all of the connection's transports are in the "closed" state.
             */
            PeerConnection.PeerConnectionState.NEW,
                /**
                 * One or more of the ICE transports are currently in the process of establishing a connection;
                 * that is, their RTCIceConnectionState is either "checking" or "connected", and no transports are in the "failed" state
                 */
            PeerConnection.PeerConnectionState.CONNECTING -> {
                webRtcCall.mxCall.state = CallState.Connected(MxPeerConnectionState.CONNECTING)
            }
            /**
             * The RTCPeerConnection is closed.
             * This value was in the RTCSignalingState enum (and therefore found by reading the value of the signalingState)
             * property until the May 13, 2016 draft of the specification.
             */
            PeerConnection.PeerConnectionState.CLOSED -> {
                webRtcCall.mxCall.state = CallState.Connected(MxPeerConnectionState.CLOSED)
            }
            /**
             * At least one of the ICE transports for the connection is in the "disconnected" state and none of
             * the other transports are in the state "failed", "connecting", or "checking".
             */
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                webRtcCall.mxCall.state = CallState.Connected(MxPeerConnectionState.DISCONNECTED)
            }
            null -> {
            }
        }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        Timber.tag(loggerTag.value).v("StreamObserver onIceCandidate: $iceCandidate")
        webRtcCall.onIceCandidate(iceCandidate)
    }

    override fun onDataChannel(dc: DataChannel) {
        Timber.tag(loggerTag.value).v("StreamObserver onDataChannel: ${dc.state()}")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Timber.tag(loggerTag.value).v("StreamObserver onIceConnectionReceivingChange: $receiving")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
        Timber.tag(loggerTag.value).v("StreamObserver onIceConnectionChange IceConnectionState:$newState")
        when (newState) {
            /**
             * the ICE agent is gathering addresses or is waiting to be given remote candidates through
             * calls to RTCPeerConnection.addIceCandidate() (or both).
             */
            PeerConnection.IceConnectionState.NEW -> {
            }
            /**
             * The ICE agent has been given one or more remote candidates and is checking pairs of local and remote candidates
             * against one another to try to find a compatible match, but has not yet found a pair which will allow
             * the peer connection to be made. It's possible that gathering of candidates is also still underway.
             */
            PeerConnection.IceConnectionState.CHECKING -> {
            }

            /**
             * A usable pairing of local and remote candidates has been found for all components of the connection,
             * and the connection has been established.
             * It's possible that gathering is still underway, and it's also possible that the ICE agent is still checking
             * candidates against one another looking for a better connection to use.
             */
            PeerConnection.IceConnectionState.CONNECTED -> {
            }
            /**
             * Checks to ensure that components are still connected failed for at least one component of the RTCPeerConnection.
             * This is a less stringent test than "failed" and may trigger intermittently and resolve just as spontaneously on less reliable networks,
             * or during temporary disconnections. When the problem resolves, the connection may return to the "connected" state.
             */
            PeerConnection.IceConnectionState.DISCONNECTED -> {
            }
            /**
             * The ICE candidate has checked all candidates pairs against one another and has failed to find
             * compatible matches for all components of the connection.
             * It is, however, possible that the ICE agent did find compatible connections for some components.
             */
            PeerConnection.IceConnectionState.FAILED -> {
                webRtcCall.onRenegotiationNeeded(restartIce = true)
            }
            /**
             *  The ICE agent has finished gathering candidates, has checked all pairs against one another, and has found a connection for all components.
             */
            PeerConnection.IceConnectionState.COMPLETED -> {
            }
            /**
             * The ICE agent for this RTCPeerConnection has shut down and is no longer handling requests.
             */
            PeerConnection.IceConnectionState.CLOSED -> {
            }
        }
    }

    override fun onAddStream(stream: MediaStream) {
        Timber.tag(loggerTag.value).v("StreamObserver onAddStream: $stream")
        webRtcCall.onAddStream(stream)
    }

    override fun onRemoveStream(stream: MediaStream) {
        Timber.tag(loggerTag.value).v("StreamObserver onRemoveStream")
        webRtcCall.onRemoveStream()
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
        Timber.tag(loggerTag.value).v("StreamObserver onIceGatheringChange: $newState")
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState) {
        Timber.tag(loggerTag.value).v("StreamObserver onSignalingChange: $newState")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {
        Timber.tag(loggerTag.value).v("StreamObserver onIceCandidatesRemoved: ${candidates.contentToString()}")
    }

    override fun onRenegotiationNeeded() {
        Timber.tag(loggerTag.value).v("StreamObserver onRenegotiationNeeded")
        webRtcCall.onRenegotiationNeeded(restartIce = false)
    }

    /**
     * This happens when a new track of any kind is added to the media stream.
     * This event is fired when the browser adds a track to the stream
     * (such as when a RTCPeerConnection is renegotiated or a stream being captured using HTMLMediaElement.captureStream()
     * gets a new set of tracks because the media element being captured loaded a new source.
     */
    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Timber.tag(loggerTag.value).v("StreamObserver onAddTrack")
    }
}
