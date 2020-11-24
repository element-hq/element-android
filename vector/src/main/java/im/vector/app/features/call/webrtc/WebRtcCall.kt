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

package im.vector.app.features.call.webrtc

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService
import im.vector.app.core.services.CallService
import im.vector.app.features.call.CallAudioManager
import im.vector.app.features.call.CameraEventsHandlerAdapter
import im.vector.app.features.call.CameraProxy
import im.vector.app.features.call.CameraType
import im.vector.app.features.call.CaptureFormat
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.utils.asWebRTC
import im.vector.app.features.call.utils.awaitCreateAnswer
import im.vector.app.features.call.utils.awaitCreateOffer
import im.vector.app.features.call.utils.awaitSetLocalDescription
import im.vector.app.features.call.utils.awaitSetRemoteDescription
import im.vector.app.features.call.utils.mapToCallCandidate
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.SdpType
import org.matrix.android.sdk.internal.util.awaitCallback
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

private const val STREAM_ID = "ARDAMS"
private const val AUDIO_TRACK_ID = "ARDAMSa0"
private const val VIDEO_TRACK_ID = "ARDAMSv0"
private val DEFAULT_AUDIO_CONSTRAINTS = MediaConstraints()

class WebRtcCall(val mxCall: MxCall,
                 private val callAudioManager: CallAudioManager,
                 private val rootEglBase: EglBase?,
                 private val context: Context,
                 private val dispatcher: CoroutineContext,
                 private val sessionProvider: Provider<Session?>,
                 private val peerConnectionFactoryProvider: Provider<PeerConnectionFactory?>,
                 private val onCallEnded: (WebRtcCall) -> Unit): MxCall.StateListener {

    interface Listener: MxCall.StateListener {
        fun onCaptureStateChanged() {}
        fun onCameraChange() {}
    }

    private val listeners = ArrayList<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    val callId = mxCall.callId

    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    // Perfect negotiation state: https://www.w3.org/TR/webrtc/#perfect-negotiation-example
    private var makingOffer: Boolean = false
    private var ignoreOffer: Boolean = false

    private var videoCapturer: CameraVideoCapturer? = null

    private val availableCamera = ArrayList<CameraProxy>()
    private var cameraInUse: CameraProxy? = null
    private var currentCaptureFormat: CaptureFormat = CaptureFormat.HD
    private var cameraAvailabilityCallback: CameraManager.AvailabilityCallback? = null

    // Mute status
    var micMuted = false
    var videoMuted = false
    var remoteOnHold = false

    var offerSdp: CallInviteContent.Offer? = null

    var videoCapturerIsInError = false
        set(value) {
            field = value
            listeners.forEach {
                tryOrNull { it.onCaptureStateChanged() }
            }
        }
    private var localSurfaceRenderers: MutableList<WeakReference<SurfaceViewRenderer>> = ArrayList()
    private var remoteSurfaceRenderers: MutableList<WeakReference<SurfaceViewRenderer>> = ArrayList()

    private val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()
    private val iceCandidateDisposable = iceCandidateSource
            .buffer(300, TimeUnit.MILLISECONDS)
            .subscribe {
                // omit empty :/
                if (it.isNotEmpty()) {
                    Timber.v("## Sending local ice candidates to call")
                    // it.forEach { peerConnection?.addIceCandidate(it) }
                    mxCall.sendLocalIceCandidates(it.mapToCallCandidate())
                }
            }

    private val remoteCandidateSource: ReplaySubject<IceCandidate> = ReplaySubject.create()
    private var remoteIceCandidateDisposable: Disposable? = null

    init {
        mxCall.addListener(this)
    }

    fun onIceCandidate(iceCandidate: IceCandidate) = iceCandidateSource.onNext(iceCandidate)

    fun onRenegationNeeded() {
        GlobalScope.launch(dispatcher) {
            if (mxCall.state != CallState.CreateOffer && mxCall.opponentVersion == 0) {
                Timber.v("Opponent does not support renegotiation: ignoring onRenegotiationNeeded event")
                return@launch
            }
            val constraints = MediaConstraints()
            // These are deprecated options
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCall?.mxCall?.isVideoCall == true) "true" else "false"))

            val peerConnection = peerConnection ?: return@launch
            Timber.v("## VOIP creating offer...")
            makingOffer = true
            try {
                val sessionDescription = peerConnection.awaitCreateOffer(constraints) ?: return@launch
                peerConnection.awaitSetLocalDescription(sessionDescription)
                if (peerConnection.iceGatheringState() == PeerConnection.IceGatheringState.GATHERING) {
                    // Allow a short time for initial candidates to be gathered
                    delay(200)
                }
                if (mxCall.state == CallState.Terminated) {
                    return@launch
                }
                if (mxCall.state == CallState.CreateOffer) {
                    // send offer to peer
                    mxCall.offerSdp(sessionDescription.description)
                } else {
                    mxCall.negotiate(sessionDescription.description)
                }
            } catch (failure: Throwable) {
                // Need to handle error properly.
                Timber.v("Failure while creating offer")
            } finally {
                makingOffer = false
            }
        }
    }

    private fun createPeerConnection(turnServerResponse: TurnServerResponse?) {
        val peerConnectionFactory = peerConnectionFactoryProvider.get() ?: return
        val iceServers = mutableListOf<PeerConnection.IceServer>().apply {
            turnServerResponse?.let { server ->
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
        Timber.v("## VOIP creating peer connection...with iceServers $iceServers ")
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, PeerConnectionObserver(this, callAudioManager))
    }

    fun attachViewRenderers(localViewRenderer: SurfaceViewRenderer?, remoteViewRenderer: SurfaceViewRenderer, mode: String?) {
        Timber.v("## VOIP attachViewRenderers localRendeder $localViewRenderer / $remoteViewRenderer")
//        this.localSurfaceRenderer =  WeakReference(localViewRenderer)
//        this.remoteSurfaceRenderer = WeakReference(remoteViewRenderer)
        localSurfaceRenderers.addIfNeeded(localViewRenderer)
        remoteSurfaceRenderers.addIfNeeded(remoteViewRenderer)

        // The call is going to resume from background, we can reduce notif
        mxCall
                .takeIf { it.state is CallState.Connected }
                ?.let { mxCall ->
                    val session = sessionProvider.get()
                    val name = session?.getUser(mxCall.opponentUserId)?.getBestName()
                            ?: mxCall.roomId
                    // Start background service with notification
                    CallService.onPendingCall(
                            context = context,
                            isVideo = mxCall.isVideoCall,
                            roomName = name,
                            roomId = mxCall.roomId,
                            matrixId = session?.myUserId ?:"",
                            callId = mxCall.callId)
                }

        GlobalScope.launch(dispatcher) {
            when (mode) {
                VectorCallActivity.INCOMING_ACCEPT -> {
                    internalAcceptIncomingCall()
                }
                VectorCallActivity.INCOMING_RINGING -> {
                    // wait until accepted to create peer connection
                    // TODO eventually we could already display local stream in PIP?
                }
                VectorCallActivity.OUTGOING_CREATED -> {
                    setupOutgoingCall()
                }
                else                                -> {
                    // sink existing tracks (configuration change, e.g screen rotation)
                    attachViewRenderersInternal()
                }
            }
        }
    }

    fun acceptIncomingCall() {
        GlobalScope.launch {
            Timber.v("## VOIP acceptIncomingCall from state ${mxCall.state}")
            if (mxCall.state == CallState.LocalRinging) {
                internalAcceptIncomingCall()
            }
        }
    }

    fun detachRenderers(renderers: List<SurfaceViewRenderer>?) {
        Timber.v("## VOIP detachRenderers")
        // currentCall?.localMediaStream?.let { currentCall?.peerConnection?.removeStream(it) }
        if (renderers.isNullOrEmpty()) {
            // remove all sinks
            localSurfaceRenderers.forEach {
                if (it.get() != null) localVideoTrack?.removeSink(it.get())
            }
            remoteSurfaceRenderers.forEach {
                if (it.get() != null) remoteVideoTrack?.removeSink(it.get())
            }
            localSurfaceRenderers.clear()
            remoteSurfaceRenderers.clear()
        } else {
            renderers.forEach {
                localSurfaceRenderers.removeIfNeeded(it)
                remoteSurfaceRenderers.removeIfNeeded(it)
                // no need to check if it's in the track, removeSink is doing it
                localVideoTrack?.removeSink(it)
                remoteVideoTrack?.removeSink(it)
            }
        }
        if (remoteSurfaceRenderers.isEmpty()) {
            // The call is going to continue in background, so ensure notification is visible
            mxCall
                    .takeIf { it.state is CallState.Connected }
                    ?.let { mxCall ->
                        // Start background service with notification
                        val session = sessionProvider.get()
                        val name = session?.getUser(mxCall.opponentUserId)?.getBestName()
                                ?: mxCall.opponentUserId
                        CallService.onOnGoingCallBackground(
                                context = context,
                                isVideo = mxCall.isVideoCall,
                                roomName = name,
                                roomId = mxCall.roomId,
                                matrixId = session?.myUserId ?: "",
                                callId = mxCall.callId
                        )
                    }
        }
    }

    private suspend fun setupOutgoingCall() = withContext(dispatcher) {
        val turnServer = getTurnServer()
        mxCall.state = CallState.CreateOffer
        // 1. Create RTCPeerConnection
        createPeerConnection(turnServer)
        // 2. Access camera (if video call) + microphone, create local stream
        createLocalStream()
        attachViewRenderersInternal()
        Timber.v("## VOIP remoteCandidateSource ${remoteCandidateSource}")
        remoteIceCandidateDisposable = remoteCandidateSource.subscribe({
            Timber.v("## VOIP adding remote ice candidate $it")
            peerConnection?.addIceCandidate(it)
        }, {
            Timber.v("## VOIP failed to add remote ice candidate $it")
        })
        // Now we wait for negotiation callback
    }

    private suspend fun internalAcceptIncomingCall() = withContext(dispatcher) {
        val turnServerResponse = getTurnServer()
        // Update service state
        withContext(Dispatchers.Main) {
            val session = sessionProvider.get()
            val name = session?.getUser(mxCall.opponentUserId)?.getBestName()
                    ?: mxCall.roomId
            CallService.onPendingCall(
                    context = context,
                    isVideo = mxCall.isVideoCall,
                    roomName = name,
                    roomId = mxCall.roomId,
                    matrixId = session?.myUserId ?: "",
                    callId = mxCall.callId
            )
        }
        // 1) create peer connection
        createPeerConnection(turnServerResponse)

        // create sdp using offer, and set remote description
        // the offer has beed stored when invite was received
        val offerSdp = offerSdp?.sdp?.let {
            SessionDescription(SessionDescription.Type.OFFER, it)
        }
        if (offerSdp == null) {
            Timber.v("We don't have any offer to process")
            return@withContext
        }
        Timber.v("Offer sdp for invite: ${offerSdp.description}")
        try {
            peerConnection?.awaitSetRemoteDescription(offerSdp)
        } catch (failure: Throwable) {
            Timber.v("Failure putting remote description")
            return@withContext
        }
        // 2) Access camera + microphone, create local stream
        createLocalStream()
        attachViewRenderersInternal()

        // create a answer, set local description and send via signaling
        createAnswer()?.also {
            mxCall.accept(it.description)
        }
        Timber.v("## VOIP remoteCandidateSource ${remoteCandidateSource}")
        remoteIceCandidateDisposable = remoteCandidateSource.subscribe({
            Timber.v("## VOIP adding remote ice candidate $it")
            peerConnection?.addIceCandidate(it)
        }, {
            Timber.v("## VOIP failed to add remote ice candidate $it")
        })
    }

    private fun attachViewRenderersInternal() {
        // render local video in pip view
        localSurfaceRenderers.forEach { renderer ->
            renderer.get()?.let { pipSurface ->
                pipSurface.setMirror(this.cameraInUse?.type == CameraType.FRONT)
                // no need to check if already added, addSink is checking that
                localVideoTrack?.addSink(pipSurface)
            }
        }

        // If remote track exists, then sink it to surface
        remoteSurfaceRenderers.forEach { renderer ->
            renderer.get()?.let { participantSurface ->
                remoteVideoTrack?.addSink(participantSurface)
            }
        }
    }

    private suspend fun getTurnServer(): TurnServerResponse? {
        return tryOrNull {
            awaitCallback {
                sessionProvider.get()?.callSignalingService()?.getTurnServer(it)
            }
        }
    }

    private fun createLocalStream() {
        val peerConnectionFactory = peerConnectionFactoryProvider.get() ?: return
        Timber.v("Create local stream for call ${mxCall.callId}")
        configureAudioTrack(peerConnectionFactory)
        // add video track if needed
        if (mxCall.isVideoCall) {
            configureVideoTrack(peerConnectionFactory)
        }
        updateMuteStatus()
    }

    private fun configureAudioTrack(peerConnectionFactory: PeerConnectionFactory) {
        val audioSource = peerConnectionFactory.createAudioSource(DEFAULT_AUDIO_CONSTRAINTS)
        val audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack.setEnabled(true)
        Timber.v("Add audio track $AUDIO_TRACK_ID to call ${mxCall.callId}")
        peerConnection?.addTrack(audioTrack, listOf(STREAM_ID))
        localAudioSource = audioSource
        localAudioTrack = audioTrack
    }

    private fun configureVideoTrack(peerConnectionFactory: PeerConnectionFactory) {
        val cameraIterator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(false)
        }
        // I don't realy know how that works if there are 2 front or 2 back cameras
        val frontCamera = cameraIterator.deviceNames
                ?.firstOrNull { cameraIterator.isFrontFacing(it) }
                ?.let {
                    CameraProxy(it, CameraType.FRONT).also { availableCamera.add(it) }
                }

        val backCamera = cameraIterator.deviceNames
                ?.firstOrNull { cameraIterator.isBackFacing(it) }
                ?.let {
                    CameraProxy(it, CameraType.BACK).also { availableCamera.add(it) }
                }

        val camera = frontCamera?.also { cameraInUse = frontCamera }
                ?: backCamera?.also { cameraInUse = backCamera }
                ?: null.also { cameraInUse = null }

        if (camera != null) {
            val videoCapturer = cameraIterator.createCapturer(camera.name, object : CameraEventsHandlerAdapter() {
                override fun onFirstFrameAvailable() {
                    super.onFirstFrameAvailable()
                    videoCapturerIsInError = false
                }

                override fun onCameraClosed() {
                    super.onCameraClosed()
                    // This could happen if you open the camera app in chat
                    // We then register in order to restart capture as soon as the camera is available again
                    videoCapturerIsInError = true
                    val cameraManager = context.getSystemService<CameraManager>()
                    cameraAvailabilityCallback = object : CameraManager.AvailabilityCallback() {
                        override fun onCameraAvailable(cameraId: String) {
                            if (cameraId == camera.name) {
                                videoCapturer?.startCapture(currentCaptureFormat.width, currentCaptureFormat.height, currentCaptureFormat.fps)
                                cameraManager?.unregisterAvailabilityCallback(this)
                            }
                        }
                    }
                    cameraManager?.registerAvailabilityCallback(cameraAvailabilityCallback!!, null)
                }
            })

            val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
            Timber.v("## VOIP Local video source created")

            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            // HD
            videoCapturer.startCapture(currentCaptureFormat.width, currentCaptureFormat.height, currentCaptureFormat.fps)
            this.videoCapturer = videoCapturer

            val videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
            Timber.v("Add video track $VIDEO_TRACK_ID to call ${mxCall.callId}")
            videoTrack.setEnabled(true)
            peerConnection?.addTrack(videoTrack, listOf(STREAM_ID))
            localVideoSource = videoSource
            localVideoTrack = videoTrack
        }
    }

    fun setCaptureFormat(format: CaptureFormat) {
        GlobalScope.launch(dispatcher) {
            Timber.v("## VOIP setCaptureFormat $format")
            videoCapturer?.changeCaptureFormat(format.width, format.height, format.fps)
            currentCaptureFormat = format
        }
    }

    private fun updateMuteStatus() {
        val micShouldBeMuted = micMuted || remoteOnHold
        localAudioTrack?.setEnabled(!micShouldBeMuted)
        val vidShouldBeMuted = videoMuted || remoteOnHold
        localVideoTrack?.setEnabled(!vidShouldBeMuted)
    }

    /**
     * Indicates whether we are 'on hold' to the remote party (ie. if true,
     * they cannot hear us). Note that this will return true when we put the
     * remote on hold too due to the way hold is implemented (since we don't
     * wish to play hold music when we put a call on hold, we use 'inactive'
     * rather than 'sendonly')
     * @returns true if the other party has put us on hold
     */
    fun isLocalOnHold(): Boolean {
        if (mxCall.state !is CallState.Connected) return false
        var callOnHold = true
        // We consider a call to be on hold only if *all* the tracks are on hold
        // (is this the right thing to do?)
        for (transceiver in peerConnection?.transceivers ?: emptyList()) {
            val trackOnHold = transceiver.currentDirection == RtpTransceiver.RtpTransceiverDirection.INACTIVE
                    || transceiver.currentDirection == RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
            if (!trackOnHold) callOnHold = false;
        }
        return callOnHold;
    }

    fun updateRemoteOnHold(onHold: Boolean) {
        if (remoteOnHold == onHold) return
        remoteOnHold = onHold
        val direction = if (onHold) {
            RtpTransceiver.RtpTransceiverDirection.INACTIVE
        } else {
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        }
        for (transceiver in peerConnection?.transceivers ?: emptyList()) {
            transceiver.direction = direction
        }
        updateMuteStatus()
    }

    fun muteCall(muted: Boolean) {
        micMuted = muted
        updateMuteStatus()
    }

    fun enableVideo(enabled: Boolean) {
        videoMuted = !enabled
        updateMuteStatus()
    }

    fun canSwitchCamera(): Boolean {
        return availableCamera.size > 0
    }

    fun switchCamera() {
        Timber.v("## VOIP switchCamera")
        if (!canSwitchCamera()) return
        if (mxCall.state is CallState.Connected && mxCall.isVideoCall) {
            videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                // Invoked on success. |isFrontCamera| is true if the new camera is front facing.
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    Timber.v("## VOIP onCameraSwitchDone isFront $isFrontCamera")
                    cameraInUse = availableCamera.first { if (isFrontCamera) it.type == CameraType.FRONT else it.type == CameraType.BACK }
                    localSurfaceRenderers.forEach {
                        it.get()?.setMirror(isFrontCamera)
                    }
                    listeners.forEach {
                        tryOrNull { it.onCameraChange() }
                    }

                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Timber.v("## VOIP onCameraSwitchError isFront $errorDescription")
                }
            })
        }
    }

    private suspend fun createAnswer(): SessionDescription? {
        Timber.w("## VOIP createAnswer")
        val peerConnection = peerConnection ?: return null
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (mxCall.isVideoCall) "true" else "false"))
        }
        return try {
            val localDescription = peerConnection.awaitCreateAnswer(constraints) ?: return null
            peerConnection.awaitSetLocalDescription(localDescription)
            localDescription
        } catch (failure: Throwable) {
            Timber.v("Fail to create answer")
            null
        }
    }

    fun currentCameraType(): CameraType? {
        return cameraInUse?.type
    }

    fun currentCaptureFormat(): CaptureFormat {
        return currentCaptureFormat
    }

    private fun release() {
        mxCall.removeListener(this)
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        remoteIceCandidateDisposable?.dispose()
        iceCandidateDisposable?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        localAudioSource?.dispose()
        localVideoSource?.dispose()
        localAudioSource = null
        localAudioTrack = null
        localVideoSource = null
        localVideoTrack = null
        cameraAvailabilityCallback = null
    }

    fun onAddStream(stream: MediaStream) {
        GlobalScope.launch(dispatcher) {
            // reportError("Weird-looking stream: " + stream);
            if (stream.audioTracks.size > 1 || stream.videoTracks.size > 1) {
                Timber.e("## VOIP StreamObserver weird looking stream: $stream")
                // TODO maybe do something more??
                mxCall.hangUp()
                return@launch
            }
            if (stream.videoTracks.size == 1) {
                val remoteVideoTrack = stream.videoTracks.first()
                remoteVideoTrack.setEnabled(true)
                this@WebRtcCall.remoteVideoTrack = remoteVideoTrack
                // sink to renderer if attached
                remoteSurfaceRenderers.forEach { it.get()?.let { remoteVideoTrack.addSink(it) } }
            }
        }
    }

    fun onRemoveStream() {
        GlobalScope.launch(dispatcher) {
            remoteSurfaceRenderers
                    .mapNotNull { it.get() }
                    .forEach { remoteVideoTrack?.removeSink(it) }
            remoteVideoTrack = null
        }
    }

    fun endCall(originatedByMe: Boolean = true, reason: CallHangupContent.Reason? = null) {
        mxCall.state = CallState.Terminated
        //Close tracks ASAP
        localVideoTrack?.setEnabled(false)
        localVideoTrack?.setEnabled(false)
        cameraAvailabilityCallback?.let { cameraAvailabilityCallback ->
            val cameraManager = context.getSystemService<CameraManager>()!!
            cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback)
        }
        release()
        onCallEnded(this)
        if (originatedByMe) {
            // send hang up event
            if (mxCall.state is CallState.Connected) {
                mxCall.hangUp(reason)
            } else {
                mxCall.reject()
            }
        }
    }

    // Call listener

    fun onCallIceCandidateReceived(iceCandidatesContent: CallCandidatesContent) {
        GlobalScope.launch(dispatcher) {
            iceCandidatesContent.candidates.forEach {
                Timber.v("## VOIP onCallIceCandidateReceived for call ${mxCall.callId} sdp: ${it.candidate}")
                val iceCandidate = IceCandidate(it.sdpMid, it.sdpMLineIndex, it.candidate)
                remoteCandidateSource.onNext(iceCandidate)
            }
        }
    }

    fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) {
        GlobalScope.launch(dispatcher) {
            Timber.v("## VOIP onCallAnswerReceived ${callAnswerContent.callId}")
            val sdp = SessionDescription(SessionDescription.Type.ANSWER, callAnswerContent.answer.sdp)
            try {
                peerConnection?.awaitSetRemoteDescription(sdp)
            } catch (failure: Throwable) {
                return@launch
            }
            if (mxCall.opponentPartyId?.hasValue().orFalse()) {
                mxCall.selectAnswer()
            }
        }
    }

    fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent) {
        GlobalScope.launch(dispatcher) {
            val description = callNegotiateContent.description
            val type = description?.type
            val sdpText = description?.sdp
            if (type == null || sdpText == null) {
                Timber.i("Ignoring invalid m.call.negotiate event");
                return@launch
            }
            val peerConnection = peerConnection ?: return@launch
            // Politeness always follows the direction of the call: in a glare situation,
            // we pick either the inbound or outbound call, so one side will always be
            // inbound and one outbound
            val polite = !mxCall.isOutgoing
            // Here we follow the perfect negotiation logic from
            // https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Perfect_negotiation
            val offerCollision = description.type == SdpType.OFFER
                    && (makingOffer || peerConnection.signalingState() != PeerConnection.SignalingState.STABLE)

            ignoreOffer = !polite && offerCollision
            if (ignoreOffer) {
                Timber.i("Ignoring colliding negotiate event because we're impolite")
                return@launch
            }
            try {
                val sdp = SessionDescription(type.asWebRTC(), sdpText)
                peerConnection.awaitSetRemoteDescription(sdp)
                if (type == SdpType.OFFER) {
                    createAnswer()
                    mxCall.negotiate(sdpText)
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to complete negotiation")
            }
        }
    }

    // MxCall.StateListener

    override fun onStateUpdate(call: MxCall) {
        listeners.forEach {
            tryOrNull { it.onStateUpdate(call) }
        }
    }
}

private fun MutableList<WeakReference<SurfaceViewRenderer>>.addIfNeeded(renderer: SurfaceViewRenderer?) {
    if (renderer == null) return
    val exists = any {
        it.get() == renderer
    }
    if (!exists) {
        add(WeakReference(renderer))
    }
}

private fun MutableList<WeakReference<SurfaceViewRenderer>>.removeIfNeeded(renderer: SurfaceViewRenderer?) {
    if (renderer == null) return
    removeAll {
        it.get() == renderer
    }
}
