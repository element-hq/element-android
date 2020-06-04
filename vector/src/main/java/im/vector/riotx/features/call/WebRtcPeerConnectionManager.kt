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

package im.vector.riotx.features.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.extensions.tryThis
import im.vector.matrix.android.api.session.call.CallsListener
import im.vector.matrix.android.api.session.call.EglUtils
import im.vector.matrix.android.api.session.call.MxCall
import im.vector.matrix.android.api.session.call.MxCallDetail
import im.vector.matrix.android.api.session.call.TurnServer
import im.vector.matrix.android.api.session.room.model.call.CallAnswerContent
import im.vector.matrix.android.api.session.room.model.call.CallHangupContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.call.service.CallHeadsUpService
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manage peerConnectionFactory & Peer connections outside of activity lifecycle to resist configuration changes
 * Use app context
 */
@Singleton
class WebRtcPeerConnectionManager @Inject constructor(
        private val context: Context,
        private val sessionHolder: ActiveSessionHolder
) : CallsListener {

    var localMediaStream: MediaStream? = null

    private val executor = Executors.newSingleThreadExecutor()

    private val rootEglBase by lazy { EglUtils.rootEglBase }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var localSdp: SessionDescription? = null
    private var sdpObserver = SdpObserver()
    private var streamObserver = StreamObserver()

    private var localViewRenderer: SurfaceViewRenderer? = null
    private var remoteViewRenderer: SurfaceViewRenderer? = null

    private var remoteVideoTrack: VideoTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var videoCapturer: VideoCapturer? = null

    var localSurfaceRenderer: WeakReference<SurfaceViewRenderer>? = null
    var remoteSurfaceRenderer: WeakReference<SurfaceViewRenderer>? = null

    private val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()
    private var iceCandidateDisposable: Disposable? = null

    var callHeadsUpService: CallHeadsUpService? = null

    private var currentCall: MxCall? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            callHeadsUpService = (service as? CallHeadsUpService.CallHeadsUpServiceBinder)?.getService()
        }
    }

    private fun createPeerConnectionFactory() {
        Timber.v("## VOIP createPeerConnectionFactory")
        val eglBaseContext = rootEglBase?.eglBaseContext ?: return Unit.also {
            Timber.e("## VOIP No EGL BASE")
        }

        Timber.v("## VOIP PeerConnectionFactory.initialize")
        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions.builder(context.applicationContext)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
                eglBaseContext,
                /* enableIntelVp8Encoder */
                true,
                /* enableH264HighProfile */
                true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        Timber.v("## VOIP PeerConnectionFactory.createPeerConnectionFactory ...")
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()

        attachViewRenderersInternal()
    }

    private fun createPeerConnection(turnServer: TurnServer?) {
        val iceServers = mutableListOf<PeerConnection.IceServer>().apply {
            turnServer?.let { server ->
                server.uris?.forEach { uri ->
                    add(
                            PeerConnection
                                    .IceServer
                                    .builder(uri)
                                    .setUsername(server.username)
                                    .setPassword(server.password)
                                    .createIceServer()
                    )
                }
            }
        }
        Timber.v("## VOIP creating peer connection... ")
        peerConnection = peerConnectionFactory?.createPeerConnection(iceServers, streamObserver)
    }

    private fun startCall(turnServer: TurnServer?) {
        iceCandidateDisposable = iceCandidateSource
                .buffer(400, TimeUnit.MILLISECONDS)
                .subscribe {
                    // omit empty :/
                    if (it.isNotEmpty()) {
                        Timber.v("## Sending local ice candidates to call")
                        it.forEach { peerConnection?.addIceCandidate(it) }
                        currentCall?.sendLocalIceCandidates(it)
                    }
                }

        executor.execute {
            createPeerConnectionFactory()
            createPeerConnection(turnServer)
        }
    }

    private fun sendSdpOffer() {
        executor.execute {
            val constraints = MediaConstraints()
            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCall?.isVideoCall == true) "true" else "false"))

            Timber.v("## VOIP creating offer...")
            peerConnection?.createOffer(sdpObserver, constraints)
        }
    }

    fun attachViewRenderers(localViewRenderer: SurfaceViewRenderer, remoteViewRenderer: SurfaceViewRenderer) {
        executor.execute {
            this.localViewRenderer = localViewRenderer
            this.remoteViewRenderer = remoteViewRenderer
            this.localSurfaceRenderer = WeakReference(localViewRenderer)
            this.remoteSurfaceRenderer = WeakReference(remoteViewRenderer)

            if (peerConnection != null) {
                attachViewRenderersInternal()
            }
        }
    }

    private fun attachViewRenderersInternal() {
        executor.execute {
            audioSource = peerConnectionFactory?.createAudioSource(DEFAULT_AUDIO_CONSTRAINTS)
            localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
            localAudioTrack?.setEnabled(true)

            localViewRenderer?.setMirror(true)
            localVideoTrack?.addSink(localViewRenderer)

            localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS") // magic value?

            if (currentCall?.isVideoCall == true) {
                val cameraIterator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(false)
                val frontCamera = cameraIterator.deviceNames
                        ?.firstOrNull { cameraIterator.isFrontFacing(it) }
                        ?: cameraIterator.deviceNames?.first()

                val videoCapturer = cameraIterator.createCapturer(frontCamera, null)

                videoSource = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
                Timber.v("## VOIP Local video source created")
                videoCapturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource!!.capturerObserver)
                videoCapturer.startCapture(1280, 720, 30)
                localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
                Timber.v("## VOIP Local video track created")
                localSurfaceRenderer?.get()?.let { surface ->
                    localVideoTrack?.addSink(surface)
                }
                localVideoTrack?.setEnabled(true)

                localVideoTrack?.addSink(localViewRenderer)
                localMediaStream?.addTrack(localVideoTrack)
                remoteVideoTrack?.let {
                    it.setEnabled(true)
                    it.addSink(remoteViewRenderer)
                }
            }

            localMediaStream?.addTrack(localAudioTrack)

            Timber.v("## VOIP add local stream to peer connection")
            peerConnection?.addStream(localMediaStream)
        }
    }

    fun detachRenderers() {
        localSurfaceRenderer?.get()?.let {
            localVideoTrack?.removeSink(it)
        }
        remoteSurfaceRenderer?.get()?.let {
            remoteVideoTrack?.removeSink(it)
        }
        localSurfaceRenderer = null
        remoteSurfaceRenderer = null
    }

    fun close() {
        executor.execute {
            // Do not dispose peer connection (https://bugs.chromium.org/p/webrtc/issues/detail?id=7543)
            tryThis { audioSource?.dispose() }
            tryThis { videoSource?.dispose() }
            tryThis { videoCapturer?.stopCapture() }
            tryThis { videoCapturer?.dispose() }
            localMediaStream?.let { peerConnection?.removeStream(it) }
            peerConnection?.close()
            peerConnection = null
            peerConnectionFactory?.stopAecDump()
            peerConnectionFactory = null
        }
        iceCandidateDisposable?.dispose()
        context.stopService(Intent(context, CallHeadsUpService::class.java))
    }

    companion object {

        private const val AUDIO_TRACK_ID = "ARDAMSa0"

        private val DEFAULT_AUDIO_CONSTRAINTS = MediaConstraints().apply {
            // add all existing audio filters to avoid having echos
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation2", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"))

            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))

            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "true"))

            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression2", "true"))

            mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
    }

    fun startOutgoingCall(context: Context, signalingRoomId: String, otherUserId: String, isVideoCall: Boolean) {
        val createdCall = sessionHolder.getSafeActiveSession()?.callService()?.createOutgoingCall(signalingRoomId, otherUserId, isVideoCall) ?: return
        currentCall = createdCall

        startHeadsUpService(createdCall)
        context.startActivity(VectorCallActivity.newIntent(context, createdCall))

        sessionHolder.getActiveSession().callService().getTurnServer(object : MatrixCallback<TurnServer?> {
            override fun onSuccess(data: TurnServer?) {
                startCall(data)
                sendSdpOffer()
            }
        })
    }

    override fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent) {
        // TODO What if a call is currently active?
        if (currentCall != null) {
            Timber.w("TODO: Automatically reject incoming call?")
            return
        }

        currentCall = mxCall

        startHeadsUpService(mxCall)

        sessionHolder.getActiveSession().callService().getTurnServer(object : MatrixCallback<TurnServer?> {
            override fun onSuccess(data: TurnServer?) {
                startCall(data)
                setInviteRemoteDescription(callInviteContent.offer?.sdp)
            }
        })
    }

    private fun setInviteRemoteDescription(description: String?) {
        executor.execute {
            val sdp = SessionDescription(SessionDescription.Type.OFFER, description)
            peerConnection?.setRemoteDescription(sdpObserver, sdp)
        }
    }

    private fun startHeadsUpService(mxCall: MxCallDetail) {
        val callHeadsUpServiceIntent = CallHeadsUpService.newInstance(context, mxCall)
        ContextCompat.startForegroundService(context, callHeadsUpServiceIntent)

        context.bindService(Intent(context, CallHeadsUpService::class.java), serviceConnection, 0)
    }

    fun answerCall() {
        if (currentCall != null) {
            executor.execute { createAnswer() }
        }
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCall?.isVideoCall == true) "true" else "false"))
        }

        peerConnection?.createAnswer(sdpObserver, constraints)
    }

    fun endCall() {
        currentCall?.hangUp()
        currentCall = null
        close()
    }

    override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) {
        executor.execute {
            Timber.v("## answerReceived")
            val sdp = SessionDescription(SessionDescription.Type.ANSWER, callAnswerContent.answer.sdp)
            peerConnection?.setRemoteDescription(sdpObserver, sdp)
        }
    }

    override fun onCallHangupReceived(callHangupContent: CallHangupContent) {
        currentCall = null
        close()
    }

    private inner class SdpObserver : org.webrtc.SdpObserver {

        override fun onCreateSuccess(origSdp: SessionDescription) {
            Timber.v("## VOIP SdpObserver onCreateSuccess")
            if (localSdp != null) return

            executor.execute {
                localSdp = SessionDescription(origSdp.type, origSdp.description)
                peerConnection?.setLocalDescription(sdpObserver, localSdp)
            }
        }

        override fun onSetSuccess() {
            Timber.v("## VOIP SdpObserver onSetSuccess")
            executor.execute {
                localSdp?.let {
                    if (currentCall?.isOutgoing == true) {
                        currentCall?.offerSdp(it)
                    } else {
                        currentCall?.accept(it)
                        currentCall?.let { context.startActivity(VectorCallActivity.newIntent(context, it)) }
                    }
                }
            }
        }

        override fun onCreateFailure(error: String) {
            Timber.v("## VOIP SdpObserver onCreateFailure: $error")
        }

        override fun onSetFailure(error: String) {
            Timber.v("## VOIP SdpObserver onSetFailure: $error")
        }
    }

    private inner class StreamObserver : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Timber.v("## VOIP StreamObserver onIceCandidate: $iceCandidate")
            iceCandidateSource.onNext(iceCandidate)
        }

        override fun onDataChannel(dc: DataChannel) {
            Timber.v("## VOIP StreamObserver onDataChannel: ${dc.state()}")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Timber.v("## VOIP StreamObserver onIceConnectionReceivingChange: $receiving")
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED    -> Timber.v("## VOIP StreamObserver onIceConnectionChange.CONNECTED")
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    Timber.v("## VOIP StreamObserver onIceConnectionChange.DISCONNECTED")
                    endCall()
                }
                PeerConnection.IceConnectionState.FAILED       -> Timber.v("## VOIP StreamObserver onIceConnectionChange.FAILED")
                else                                           -> Timber.v("## VOIP StreamObserver onIceConnectionChange.$newState")
            }
        }

        override fun onAddStream(stream: MediaStream) {
            Timber.v("## VOIP StreamObserver onAddStream: $stream")
            executor.execute {
                if (stream.audioTracks.size > 1 || stream.videoTracks.size > 1) return@execute

                if (stream.videoTracks.size == 1) {
                    remoteVideoTrack = stream.videoTracks.first()
                    remoteVideoTrack?.setEnabled(true)
                    remoteViewRenderer?.let { remoteVideoTrack?.addSink(it) }
                }
            }
        }

        override fun onRemoveStream(stream: MediaStream) {
            Timber.v("## VOIP StreamObserver onRemoveStream")
            executor.execute {
                remoteSurfaceRenderer?.get()?.let {
                    remoteVideoTrack?.removeSink(it)
                }
                remoteVideoTrack = null
            }
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
            Timber.v("## VOIP StreamObserver onIceGatheringChange: $newState")
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState) {
            Timber.v("## VOIP StreamObserver onSignalingChange: $newState")
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {
            Timber.v("## VOIP StreamObserver onIceCandidatesRemoved: ${candidates.contentToString()}")
        }

        override fun onRenegotiationNeeded() {
            Timber.v("## VOIP StreamObserver onRenegotiationNeeded")
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Timber.v("## VOIP StreamObserver onAddTrack")
        }
    }
}
