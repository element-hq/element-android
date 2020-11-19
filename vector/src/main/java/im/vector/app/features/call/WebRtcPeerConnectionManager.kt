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

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.services.BluetoothHeadsetReceiver
import im.vector.app.core.services.CallService
import im.vector.app.core.services.WiredHeadsetStateReceiver
import im.vector.app.features.call.utils.awaitCreateAnswer
import im.vector.app.features.call.utils.awaitCreateOffer
import im.vector.app.features.call.utils.awaitSetLocalDescription
import im.vector.app.features.call.utils.awaitSetRemoteDescription
import im.vector.app.push.fcm.FcmHelper
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.EglUtils
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.SdpType
import org.matrix.android.sdk.api.session.room.model.call.asWebRTC
import org.matrix.android.sdk.internal.util.awaitCallback
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
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
        private val activeSessionDataSource: ActiveSessionDataSource
) : CallListener, LifecycleObserver {

    private val currentSession: Session?
        get() = activeSessionDataSource.currentValue?.orNull()

    interface CurrentCallListener {
        fun onCurrentCallChange(call: MxCall?)
        fun onCaptureStateChanged() {}
        fun onAudioDevicesChange() {}
        fun onCameraChange() {}
    }

    private val currentCallsListeners = emptyList<CurrentCallListener>().toMutableList()
    fun addCurrentCallListener(listener: CurrentCallListener) {
        currentCallsListeners.add(listener)
    }

    fun removeCurrentCallListener(listener: CurrentCallListener) {
        currentCallsListeners.remove(listener)
    }

    val callAudioManager = CallAudioManager(context.applicationContext) {
        currentCallsListeners.forEach {
            tryOrNull { it.onAudioDevicesChange() }
        }
    }

    class CallContext(val mxCall: MxCall) {

        var peerConnection: PeerConnection? = null
        var localAudioSource: AudioSource? = null
        var localAudioTrack: AudioTrack? = null
        var localVideoSource: VideoSource? = null
        var localVideoTrack: VideoTrack? = null
        var remoteVideoTrack: VideoTrack? = null

        // Perfect negotiation state: https://www.w3.org/TR/webrtc/#perfect-negotiation-example
        var makingOffer: Boolean = false
        var ignoreOffer: Boolean = false

        // Mute status
        var micMuted = false
        var videoMuted = false
        var remoteOnHold = false

        var offerSdp: CallInviteContent.Offer? = null

        val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()
        private val iceCandidateDisposable = iceCandidateSource
                .buffer(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    // omit empty :/
                    if (it.isNotEmpty()) {
                        Timber.v("## Sending local ice candidates to call")
                        // it.forEach { peerConnection?.addIceCandidate(it) }
                        mxCall.sendLocalIceCandidates(it)
                    }
                }

        var remoteCandidateSource: ReplaySubject<IceCandidate>? = null
        var remoteIceCandidateDisposable: Disposable? = null

        // We register an availability callback if we loose access to camera
        var cameraAvailabilityCallback: CameraRestarter? = null

        fun release() {
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
        }
    }

//    var localMediaStream: MediaStream? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()

    private val rootEglBase by lazy { EglUtils.rootEglBase }

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var videoCapturer: CameraVideoCapturer? = null

    private val availableCamera = ArrayList<CameraProxy>()
    private var cameraInUse: CameraProxy? = null

    private var currentCaptureMode: CaptureFormat = CaptureFormat.HD

    private var isInBackground: Boolean = true

    var capturerIsInError = false
        set(value) {
            field = value
            currentCallsListeners.forEach {
                tryOrNull { it.onCaptureStateChanged() }
            }
        }

    var localSurfaceRenderers: MutableList<WeakReference<SurfaceViewRenderer>> = ArrayList()
    var remoteSurfaceRenderers: MutableList<WeakReference<SurfaceViewRenderer>> = ArrayList()

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

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
        isInBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        isInBackground = true
    }

    var currentCall: CallContext? = null
        set(value) {
            field = value
            currentCallsListeners.forEach {
                tryOrNull { it.onCurrentCallChange(value?.mxCall) }
            }
        }

    fun headSetButtonTapped() {
        Timber.v("## VOIP headSetButtonTapped")
        val call = currentCall?.mxCall ?: return
        if (call.state is CallState.LocalRinging) {
            // accept call
            acceptIncomingCall()
        }
        if (call.state is CallState.Connected) {
            // end call?
            endCall()
        }
    }

    private fun createPeerConnectionFactory() {
        if (peerConnectionFactory != null) return
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

        // attachViewRenderersInternal()
    }

    private fun createPeerConnection(callContext: CallContext, turnServerResponse: TurnServerResponse?) {
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
        val rtcConfig = RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        callContext.peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, StreamObserver(callContext))
    }

    private fun CoroutineScope.sendSdpOffer(callContext: CallContext) = launch(dispatcher) {
        val constraints = MediaConstraints()
        // These are deprecated options
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCall?.mxCall?.isVideoCall == true) "true" else "false"))

        val call = callContext.mxCall
        val peerConnection = callContext.peerConnection ?: return@launch
        Timber.v("## VOIP creating offer...")
        callContext.makingOffer = true
        try {
            val sessionDescription = peerConnection.awaitCreateOffer(constraints) ?: return@launch
            peerConnection.awaitSetLocalDescription(sessionDescription)
            if (peerConnection.iceGatheringState() == PeerConnection.IceGatheringState.GATHERING) {
                // Allow a short time for initial candidates to be gathered
                delay(200)
            }
            if (call.state == CallState.Terminated) {
                return@launch
            }
            if (call.state == CallState.CreateOffer) {
                // send offer to peer
                call.offerSdp(sessionDescription)
            } else {
                call.negotiate(sessionDescription)
            }
        } catch (failure: Throwable) {
            // Need to handle error properly.
            Timber.v("Failure while creating offer")
        } finally {
            callContext.makingOffer = false
        }
    }

    private suspend fun getTurnServer(): TurnServerResponse? {
        return tryOrNull {
            awaitCallback<TurnServerResponse?> {
                currentSession?.callSignalingService()?.getTurnServer(it)
            }
        }
    }

    fun attachViewRenderers(localViewRenderer: SurfaceViewRenderer?, remoteViewRenderer: SurfaceViewRenderer, mode: String?) {
        Timber.v("## VOIP attachViewRenderers localRendeder $localViewRenderer / $remoteViewRenderer")
//        this.localSurfaceRenderer =  WeakReference(localViewRenderer)
//        this.remoteSurfaceRenderer = WeakReference(remoteViewRenderer)
        localSurfaceRenderers.addIfNeeded(localViewRenderer)
        remoteSurfaceRenderers.addIfNeeded(remoteViewRenderer)

        // The call is going to resume from background, we can reduce notif
        currentCall?.mxCall
                ?.takeIf { it.state is CallState.Connected }
                ?.let { mxCall ->
                    val name = currentSession?.getUser(mxCall.opponentUserId)?.getBestName()
                            ?: mxCall.roomId
                    // Start background service with notification
                    CallService.onPendingCall(
                            context = context,
                            isVideo = mxCall.isVideoCall,
                            roomName = name,
                            roomId = mxCall.roomId,
                            matrixId = currentSession?.myUserId ?: "",
                            callId = mxCall.callId)
                }

        GlobalScope.launch(dispatcher) {
            val turnServer = getTurnServer()
            val call = currentCall ?: return@launch
            when (mode) {
                VectorCallActivity.INCOMING_ACCEPT -> {
                    internalAcceptIncomingCall(call, turnServer)
                }
                VectorCallActivity.INCOMING_RINGING -> {
                    // wait until accepted to create peer connection
                    // TODO eventually we could already display local stream in PIP?
                }
                VectorCallActivity.OUTGOING_CREATED -> {
                    internalSetupOutgoingCall(call, turnServer)
                }
                else                                -> {
                    // sink existing tracks (configuration change, e.g screen rotation)
                    attachViewRenderersInternal(call)
                }
            }
        }
    }

    private suspend fun internalSetupOutgoingCall(call: CallContext, turnServer: TurnServerResponse?) {
        call.mxCall.state = CallState.CreateOffer
        // 1. Create RTCPeerConnection
        createPeerConnection(call, turnServer)

        // 2. Access camera (if video call) + microphone, create local stream
        createLocalStream(call)

        attachViewRenderersInternal(call)

        Timber.v("## VOIP remoteCandidateSource ${call.remoteCandidateSource}")
        call.remoteIceCandidateDisposable = call.remoteCandidateSource?.subscribe({
            Timber.v("## VOIP adding remote ice candidate $it")
            call.peerConnection?.addIceCandidate(it)
        }, {
            Timber.v("## VOIP failed to add remote ice candidate $it")
        })
        // Now wait for negotiation callback
    }

    private suspend fun internalAcceptIncomingCall(callContext: CallContext, turnServerResponse: TurnServerResponse?) {
        val mxCall = callContext.mxCall
        // Update service state
        withContext(Dispatchers.Main) {
            val name = currentSession?.getUser(mxCall.opponentUserId)?.getBestName()
                    ?: mxCall.roomId
            CallService.onPendingCall(
                    context = context,
                    isVideo = mxCall.isVideoCall,
                    roomName = name,
                    roomId = mxCall.roomId,
                    matrixId = currentSession?.myUserId ?: "",
                    callId = mxCall.callId
            )
        }
        // 1) create peer connection
        createPeerConnection(callContext, turnServerResponse)

        // create sdp using offer, and set remote description
        // the offer has beed stored when invite was received
        val offerSdp = callContext.offerSdp?.sdp?.let {
            SessionDescription(SessionDescription.Type.OFFER, it)
        }
        if (offerSdp == null) {
            Timber.v("We don't have any offer to process")
            return
        }
        Timber.v("Offer sdp for invite: ${offerSdp.description}")
        try {
            callContext.peerConnection?.awaitSetRemoteDescription(offerSdp)
        } catch (failure: Throwable) {
            Timber.v("Failure putting remote description")
            return
        }
        // 2) Access camera + microphone, create local stream
        createLocalStream(callContext)

        attachViewRenderersInternal(callContext)

        // create a answer, set local description and send via signaling
        createAnswer(callContext)?.also {
            callContext.mxCall.accept(it)
        }
        Timber.v("## VOIP remoteCandidateSource ${callContext.remoteCandidateSource}")
        callContext.remoteIceCandidateDisposable = callContext.remoteCandidateSource?.subscribe({
            Timber.v("## VOIP adding remote ice candidate $it")
            callContext.peerConnection?.addIceCandidate(it)
        }, {
            Timber.v("## VOIP failed to add remote ice candidate $it")
        })
    }

    private fun createLocalStream(callContext: CallContext) {
        if (peerConnectionFactory == null) {
            Timber.e("## VOIP peerConnectionFactory is null")
            return
        }
        Timber.v("Create local stream for call ${callContext.mxCall.callId}")
        val audioSource = peerConnectionFactory!!.createAudioSource(DEFAULT_AUDIO_CONSTRAINTS)
        val audioTrack = peerConnectionFactory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack.setEnabled(true)
        Timber.v("Add audio track $AUDIO_TRACK_ID to call ${callContext.mxCall.callId}")
        callContext.apply {
            peerConnection?.addTrack(audioTrack, listOf(STREAM_ID))
            localAudioSource = audioSource
            localAudioTrack = audioTrack
        }
        // add video track if needed
        if (callContext.mxCall.isVideoCall) {
            availableCamera.clear()

            val cameraIterator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(false)

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
                        capturerIsInError = false
                    }

                    override fun onCameraClosed() {
                        // This could happen if you open the camera app in chat
                        // We then register in order to restart capture as soon as the camera is available again
                        Timber.v("## VOIP onCameraClosed")
                        this@WebRtcPeerConnectionManager.capturerIsInError = true
                        val restarter = CameraRestarter(cameraInUse?.name ?: "", callContext.mxCall.callId)
                        callContext.cameraAvailabilityCallback = restarter
                        val cameraManager = context.getSystemService<CameraManager>()!!
                        cameraManager.registerAvailabilityCallback(restarter, null)
                    }
                })

                val videoSource = peerConnectionFactory!!.createVideoSource(videoCapturer.isScreencast)
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
                Timber.v("## VOIP Local video source created")

                videoCapturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource!!.capturerObserver)
                // HD
                videoCapturer.startCapture(currentCaptureMode.width, currentCaptureMode.height, currentCaptureMode.fps)
                this.videoCapturer = videoCapturer

                val videoTrack = peerConnectionFactory!!.createVideoTrack(VIDEO_TRACK_ID, videoSource)
                Timber.v("Add video track $VIDEO_TRACK_ID to call ${callContext.mxCall.callId}")
                videoTrack.setEnabled(true)
                callContext.apply {
                    peerConnection?.addTrack(videoTrack, listOf(STREAM_ID))
                    localVideoSource = videoSource
                    localVideoTrack = videoTrack
                }
            }
        }
        updateMuteStatus(callContext)
    }

    private fun attachViewRenderersInternal(call: CallContext) {
        // render local video in pip view
        localSurfaceRenderers.forEach { renderer ->
            renderer.get()?.let { pipSurface ->
                pipSurface.setMirror(this.cameraInUse?.type == CameraType.FRONT)
                // no need to check if already added, addSink is checking that
                call.localVideoTrack?.addSink(pipSurface)
            }
        }

        // If remote track exists, then sink it to surface
        remoteSurfaceRenderers.forEach { renderer ->
            renderer.get()?.let { participantSurface ->
                call.remoteVideoTrack?.addSink(participantSurface)
            }
        }
    }

    fun acceptIncomingCall() {
        GlobalScope.launch(dispatcher) {
            Timber.v("## VOIP acceptIncomingCall from state ${currentCall?.mxCall?.state}")
            val mxCall = currentCall?.mxCall
            if (mxCall?.state == CallState.LocalRinging) {
                val turnServer = getTurnServer()
                internalAcceptIncomingCall(currentCall!!, turnServer)
            }
        }
    }

    fun detachRenderers(renderers: List<SurfaceViewRenderer>?) {
        Timber.v("## VOIP detachRenderers")
        // currentCall?.localMediaStream?.let { currentCall?.peerConnection?.removeStream(it) }
        if (renderers.isNullOrEmpty()) {
            // remove all sinks
            localSurfaceRenderers.forEach {
                if (it.get() != null) currentCall?.localVideoTrack?.removeSink(it.get())
            }
            remoteSurfaceRenderers.forEach {
                if (it.get() != null) currentCall?.remoteVideoTrack?.removeSink(it.get())
            }
            localSurfaceRenderers.clear()
            remoteSurfaceRenderers.clear()
        } else {
            renderers.forEach {
                localSurfaceRenderers.removeIfNeeded(it)
                remoteSurfaceRenderers.removeIfNeeded(it)
                // no need to check if it's in the track, removeSink is doing it
                currentCall?.localVideoTrack?.removeSink(it)
                currentCall?.remoteVideoTrack?.removeSink(it)
            }
        }

        if (remoteSurfaceRenderers.isEmpty()) {
            // The call is going to continue in background, so ensure notification is visible
            currentCall?.mxCall
                    ?.takeIf { it.state is CallState.Connected }
                    ?.let { mxCall ->
                        // Start background service with notification

                        val name = currentSession?.getUser(mxCall.opponentUserId)?.getBestName()
                                ?: mxCall.opponentUserId
                        CallService.onOnGoingCallBackground(
                                context = context,
                                isVideo = mxCall.isVideoCall,
                                roomName = name,
                                roomId = mxCall.roomId,
                                matrixId = currentSession?.myUserId ?: "",
                                callId = mxCall.callId
                        )
                    }
        }
    }

    fun close() {
        Timber.v("## VOIP WebRtcPeerConnectionManager close() >")
        CallService.onNoActiveCall(context)
        callAudioManager.stop()
        val callToEnd = currentCall
        currentCall = null
        // This must be done in this thread
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        executor.execute {
            callToEnd?.release()

            if (currentCall == null) {
                Timber.v("## VOIP Dispose peerConnectionFactory as there is no need to keep one")
                peerConnectionFactory?.dispose()
                peerConnectionFactory = null
            }

            Timber.v("## VOIP WebRtcPeerConnectionManager close() executor done")
        }
    }

    companion object {

        private const val STREAM_ID = "ARDAMS"
        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"

        private val DEFAULT_AUDIO_CONSTRAINTS = MediaConstraints().apply {
            // add all existing audio filters to avoid having echos
//            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation2", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"))
//
//            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
//
//            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "true"))
//
//            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression2", "true"))
//
//            mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
//            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
    }

    fun startOutgoingCall(signalingRoomId: String, otherUserId: String, isVideoCall: Boolean) {
        executor.execute {
            if (peerConnectionFactory == null) {
                createPeerConnectionFactory()
            }
        }

        Timber.v("## VOIP startOutgoingCall in room $signalingRoomId to $otherUserId isVideo $isVideoCall")
        val createdCall = currentSession?.callSignalingService()?.createOutgoingCall(signalingRoomId, otherUserId, isVideoCall) ?: return
        val callContext = CallContext(createdCall)

        callAudioManager.startForCall(createdCall)
        currentCall = callContext

        val name = currentSession?.getUser(createdCall.opponentUserId)?.getBestName()
                ?: createdCall.opponentUserId
        CallService.onOutgoingCallRinging(
                context = context.applicationContext,
                isVideo = createdCall.isVideoCall,
                roomName = name,
                roomId = createdCall.roomId,
                matrixId = currentSession?.myUserId ?: "",
                callId = createdCall.callId)

        executor.execute {
            callContext.remoteCandidateSource = ReplaySubject.create()
        }

        // start the activity now
        context.applicationContext.startActivity(VectorCallActivity.newIntent(context, createdCall))
    }

    override fun onCallIceCandidateReceived(mxCall: MxCall, iceCandidatesContent: CallCandidatesContent) {
        Timber.v("## VOIP onCallIceCandidateReceived for call ${mxCall.callId}")
        if (currentCall?.mxCall?.callId != mxCall.callId) return Unit.also {
            Timber.w("## VOIP ignore ice candidates from other call")
        }
        val callContext = currentCall ?: return

        executor.execute {
            iceCandidatesContent.candidates.forEach {
                Timber.v("## VOIP onCallIceCandidateReceived for call ${mxCall.callId} sdp: ${it.candidate}")
                val iceCandidate = IceCandidate(it.sdpMid, it.sdpMLineIndex, it.candidate)
                callContext.remoteCandidateSource?.onNext(iceCandidate)
            }
        }
    }

    override fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent) {
        Timber.v("## VOIP onCallInviteReceived callId ${mxCall.callId}")
        // to simplify we only treat one call at a time, and ignore others
        if (currentCall != null) {
            Timber.w("## VOIP receiving incoming call while already in call?")
            // Just ignore, maybe we could answer from other session?
            return
        }
        executor.execute {
            if (peerConnectionFactory == null) {
                createPeerConnectionFactory()
            }
        }

        val callContext = CallContext(mxCall)
        currentCall = callContext
        callAudioManager.startForCall(mxCall)
        executor.execute {
            callContext.remoteCandidateSource = ReplaySubject.create()
        }

        // Start background service with notification
        val name = currentSession?.getUser(mxCall.opponentUserId)?.getBestName()
                ?: mxCall.opponentUserId
        CallService.onIncomingCallRinging(
                context = context,
                isVideo = mxCall.isVideoCall,
                roomName = name,
                roomId = mxCall.roomId,
                matrixId = currentSession?.myUserId ?: "",
                callId = mxCall.callId
        )

        callContext.offerSdp = callInviteContent.offer

        // If this is received while in background, the app will not sync,
        // and thus won't be able to received events. For example if the call is
        // accepted on an other session this device will continue ringing
        if (isInBackground) {
            if (FcmHelper.isPushSupported()) {
                // only for push version as fdroid version is already doing it?
                currentSession?.startAutomaticBackgroundSync(30, 0)
            } else {
                // Maybe increase sync freq? but how to set back to default values?
            }
        }
    }

    private suspend fun createAnswer(call: CallContext): SessionDescription? {
        Timber.w("## VOIP createAnswer")
        val peerConnection = call.peerConnection ?: return null
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (call.mxCall.isVideoCall) "true" else "false"))
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

    fun muteCall(muted: Boolean) {
        val call = currentCall ?: return
        call.micMuted = muted
        updateMuteStatus(call)
    }

    fun enableVideo(enabled: Boolean) {
        val call = currentCall ?: return
        call.videoMuted = !enabled
        updateMuteStatus(call)
    }

    fun switchCamera() {
        Timber.v("## VOIP switchCamera")
        if (!canSwitchCamera()) return
        if (currentCall != null && currentCall?.mxCall?.state is CallState.Connected && currentCall?.mxCall?.isVideoCall == true) {
            videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                // Invoked on success. |isFrontCamera| is true if the new camera is front facing.
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    Timber.v("## VOIP onCameraSwitchDone isFront $isFrontCamera")
                    cameraInUse = availableCamera.first { if (isFrontCamera) it.type == CameraType.FRONT else it.type == CameraType.BACK }
                    localSurfaceRenderers.forEach {
                        it.get()?.setMirror(isFrontCamera)
                    }

                    currentCallsListeners.forEach {
                        tryOrNull { it.onCameraChange() }
                    }
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Timber.v("## VOIP onCameraSwitchError isFront $errorDescription")
                }
            })
        }
    }

    fun canSwitchCamera(): Boolean {
        return availableCamera.size > 0
    }

    fun currentCameraType(): CameraType? {
        return cameraInUse?.type
    }

    fun setCaptureFormat(format: CaptureFormat) {
        Timber.v("## VOIP setCaptureFormat $format")
        currentCall ?: return
        executor.execute {
            // videoCapturer?.stopCapture()
            videoCapturer?.changeCaptureFormat(format.width, format.height, format.fps)
            currentCaptureMode = format
            currentCallsListeners.forEach { tryOrNull { it.onCaptureStateChanged() } }
        }
    }

    fun currentCaptureFormat(): CaptureFormat {
        return currentCaptureMode
    }

    fun endCall(originatedByMe: Boolean = true) {
        // Update service state
        CallService.onNoActiveCall(context)
        // close tracks ASAP
        currentCall?.localVideoTrack?.setEnabled(false)
        currentCall?.localVideoTrack?.setEnabled(false)

        currentCall?.cameraAvailabilityCallback?.let { cameraAvailabilityCallback ->
            val cameraManager = context.getSystemService<CameraManager>()!!
            cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback)
        }

        if (originatedByMe) {
            // send hang up event
            currentCall?.mxCall?.hangUp()
        }
        close()
    }

    fun onWiredDeviceEvent(event: WiredHeadsetStateReceiver.HeadsetPlugEvent) {
        Timber.v("## VOIP onWiredDeviceEvent $event")
        currentCall ?: return
        // sometimes we received un-wanted unplugged...
        callAudioManager.wiredStateChange(event)
    }

    fun onWirelessDeviceEvent(event: BluetoothHeadsetReceiver.BTHeadsetPlugEvent) {
        Timber.v("## VOIP onWirelessDeviceEvent $event")
        callAudioManager.bluetoothStateChange(event.plugged)
    }

    override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) {
        val call = currentCall ?: return
        if (call.mxCall.callId != callAnswerContent.callId) return Unit.also {
            Timber.w("onCallAnswerReceived for non active call? ${callAnswerContent.callId}")
        }
        val mxCall = call.mxCall
        // Update service state
        val name = currentSession?.getUser(mxCall.opponentUserId)?.getBestName()
                ?: mxCall.opponentUserId
        CallService.onPendingCall(
                context = context,
                isVideo = mxCall.isVideoCall,
                roomName = name,
                roomId = mxCall.roomId,
                matrixId = currentSession?.myUserId ?: "",
                callId = mxCall.callId
        )
        GlobalScope.launch(dispatcher) {
            Timber.v("## VOIP onCallAnswerReceived ${callAnswerContent.callId}")
            val sdp = SessionDescription(SessionDescription.Type.ANSWER, callAnswerContent.answer.sdp)
            try {
                call.peerConnection?.awaitSetRemoteDescription(sdp)
            } catch (failure: Throwable) {
                return@launch
            }
            if (call.mxCall.opponentPartyId?.hasValue().orFalse()) {
                call.mxCall.selectAnswer()
            }
        }
    }

    override fun onCallHangupReceived(callHangupContent: CallHangupContent) {
        val call = currentCall ?: return
        // Remote echos are filtered, so it's only remote hangups that i will get here
        if (call.mxCall.callId != callHangupContent.callId) return Unit.also {
            Timber.w("onCallHangupReceived for non active call? ${callHangupContent.callId}")
        }
        call.mxCall.state = CallState.Terminated
        endCall(false)
    }

    override fun onCallRejectReceived(callRejectContent: CallRejectContent) {
        val call = currentCall ?: return
        // Remote echos are filtered, so it's only remote hangups that i will get here
        if (call.mxCall.callId != callRejectContent.callId) return Unit.also {
            Timber.w("onCallRejected for non active call? ${callRejectContent.callId}")
        }
        call.mxCall.state = CallState.Terminated
        endCall(false)
    }

    override fun onCallSelectAnswerReceived(callSelectAnswerContent: CallSelectAnswerContent) {
        val call = currentCall ?: return
        if (call.mxCall.callId != callSelectAnswerContent.callId) return Unit.also {
            Timber.w("onCallSelectAnswerReceived for non active call? ${callSelectAnswerContent.callId}")
        }
        val selectedPartyId = callSelectAnswerContent.selectedPartyId
        if (selectedPartyId != call.mxCall.ourPartyId) {
            Timber.i("Got select_answer for party ID ${selectedPartyId}: we are party ID ${call.mxCall.ourPartyId}.");
            // The other party has picked somebody else's answer
            call.mxCall.state = CallState.Terminated
            endCall(false)
        }
    }

    override fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent) {
        val call = currentCall ?: return
        if (call.mxCall.callId != callNegotiateContent.callId) return Unit.also {
            Timber.w("onCallNegotiateReceived for non active call? ${callNegotiateContent.callId}")
        }
        val description = callNegotiateContent.description
        val type = description?.type
        val sdpText = description?.sdp
        if (type == null || sdpText == null) {
            Timber.i("Ignoring invalid m.call.negotiate event");
            return;
        }
        val peerConnection = call.peerConnection ?: return
        // Politeness always follows the direction of the call: in a glare situation,
        // we pick either the inbound or outbound call, so one side will always be
        // inbound and one outbound
        val polite = !call.mxCall.isOutgoing
        // Here we follow the perfect negotiation logic from
        // https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Perfect_negotiation
        val offerCollision = description.type == SdpType.OFFER
                && (call.makingOffer || peerConnection.signalingState() != PeerConnection.SignalingState.STABLE)

        call.ignoreOffer = !polite && offerCollision
        if (call.ignoreOffer) {
            Timber.i("Ignoring colliding negotiate event because we're impolite")
            return
        }

        GlobalScope.launch(dispatcher) {
            try {
                val sdp = SessionDescription(type.asWebRTC(), sdpText)
                peerConnection.awaitSetRemoteDescription(sdp)
                if (type == SdpType.OFFER) {
                    createAnswer(call)?.also {
                        call.mxCall.negotiate(it)
                    }
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to complete negotiation")
            }
        }
    }

    override fun onCallManagedByOtherSession(callId: String) {
        Timber.v("## VOIP onCallManagedByOtherSession: $callId")
        currentCall = null
        CallService.onNoActiveCall(context)

        // did we start background sync? so we should stop it
        if (isInBackground) {
            if (FcmHelper.isPushSupported()) {
                currentSession?.stopAnyBackgroundSync()
            } else {
                // for fdroid we should not stop, it should continue syncing
                // maybe we should restore default timeout/delay though?
            }
        }
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
        val call = currentCall ?: return false
        if (call.mxCall.state !is CallState.Connected) return false
        var callOnHold = true
        // We consider a call to be on hold only if *all* the tracks are on hold
        // (is this the right thing to do?)
        for (transceiver in call.peerConnection?.transceivers ?: emptyList()) {
            val trackOnHold = transceiver.currentDirection == RtpTransceiver.RtpTransceiverDirection.INACTIVE
                    || transceiver.currentDirection == RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
            if (!trackOnHold) callOnHold = false;
        }
        return callOnHold;
    }

    fun isRemoteOnHold(): Boolean {
        val call = currentCall ?: return false
        return call.remoteOnHold;
    }

    fun setRemoteOnHold(onHold: Boolean) {
        val call = currentCall ?: return
        if (call.remoteOnHold == onHold) return
        call.remoteOnHold = onHold
        val direction = if (onHold) {
            RtpTransceiver.RtpTransceiverDirection.INACTIVE
        } else {
            RtpTransceiver.RtpTransceiverDirection.SEND_RECV
        }
        for (transceiver in call.peerConnection?.transceivers ?: emptyList()) {
            transceiver.direction = direction
        }
        updateMuteStatus(call)
    }

    private fun updateMuteStatus(call: CallContext) {
        val micShouldBeMuted = call.micMuted || call.remoteOnHold
        call.localAudioTrack?.setEnabled(!micShouldBeMuted)
        val vidShouldBeMuted = call.videoMuted || call.remoteOnHold
        call.localVideoTrack?.setEnabled(!vidShouldBeMuted)
    }

    private inner class StreamObserver(val callContext: CallContext) : PeerConnection.Observer {

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            Timber.v("## VOIP StreamObserver onConnectionChange: $newState")
            when (newState) {
                /**
                 * Every ICE transport used by the connection is either in use (state "connected" or "completed")
                 * or is closed (state "closed"); in addition, at least one transport is either "connected" or "completed"
                 */
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    callContext.mxCall.state = CallState.Connected(newState)
                    callAudioManager.onCallConnected(callContext.mxCall)
                }
                /**
                 * One or more of the ICE transports on the connection is in the "failed" state.
                 */
                PeerConnection.PeerConnectionState.FAILED -> {
                    // This can be temporary, e.g when other ice not yet received...
                    // callContext.mxCall.state = CallState.ERROR
                    callContext.mxCall.state = CallState.Connected(newState)
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
                    callContext.mxCall.state = CallState.Connected(PeerConnection.PeerConnectionState.CONNECTING)
                }
                /**
                 * The RTCPeerConnection is closed.
                 * This value was in the RTCSignalingState enum (and therefore found by reading the value of the signalingState)
                 * property until the May 13, 2016 draft of the specification.
                 */
                PeerConnection.PeerConnectionState.CLOSED,
                    /**
                     * At least one of the ICE transports for the connection is in the "disconnected" state and none of
                     * the other transports are in the state "failed", "connecting", or "checking".
                     */
                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    callContext.mxCall.state = CallState.Connected(newState)
                }
                null -> {
                }
            }
        }

        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Timber.v("## VOIP StreamObserver onIceCandidate: $iceCandidate")
            callContext.iceCandidateSource.onNext(iceCandidate)
        }

        override fun onDataChannel(dc: DataChannel) {
            Timber.v("## VOIP StreamObserver onDataChannel: ${dc.state()}")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Timber.v("## VOIP StreamObserver onIceConnectionReceivingChange: $receiving")
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            Timber.v("## VOIP StreamObserver onIceConnectionChange IceConnectionState:$newState")
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
                    // I should not hangup here..
                    // because new candidates could arrive
                    // callContext.mxCall.hangUp()
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
            Timber.v("## VOIP StreamObserver onAddStream: $stream")
            executor.execute {
                // reportError("Weird-looking stream: " + stream);
                if (stream.audioTracks.size > 1 || stream.videoTracks.size > 1) {
                    Timber.e("## VOIP StreamObserver weird looking stream: $stream")
                    // TODO maybe do something more??
                    callContext.mxCall.hangUp()
                    return@execute
                }

                if (stream.videoTracks.size == 1) {
                    val remoteVideoTrack = stream.videoTracks.first()
                    remoteVideoTrack.setEnabled(true)
                    callContext.remoteVideoTrack = remoteVideoTrack
                    // sink to renderer if attached
                    remoteSurfaceRenderers.forEach { it.get()?.let { remoteVideoTrack.addSink(it) } }
                }
            }
        }

        override fun onRemoveStream(stream: MediaStream) {
            Timber.v("## VOIP StreamObserver onRemoveStream")
            executor.execute {
                //                remoteSurfaceRenderer?.get()?.let {
//                    callContext.remoteVideoTrack?.removeSink(it)
//                }
                remoteSurfaceRenderers
                        .mapNotNull { it.get() }
                        .forEach { callContext.remoteVideoTrack?.removeSink(it) }
                callContext.remoteVideoTrack = null
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
            val call = currentCall ?: return
            if (call.mxCall.state != CallState.CreateOffer && call.mxCall.opponentVersion == 0) {
                Timber.v("Opponent does not support renegotiation: ignoring onRenegotiationNeeded event")
                return
            }
            GlobalScope.sendSdpOffer(callContext)
        }

        /**
         * This happens when a new track of any kind is added to the media stream.
         * This event is fired when the browser adds a track to the stream
         * (such as when a RTCPeerConnection is renegotiated or a stream being captured using HTMLMediaElement.captureStream()
         * gets a new set of tracks because the media element being captured loaded a new source.
         */
        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Timber.v("## VOIP StreamObserver onAddTrack")
        }
    }

    inner class CameraRestarter(val cameraId: String, val callId: String) : CameraManager.AvailabilityCallback() {

        override fun onCameraAvailable(cameraId: String) {
            if (this.cameraId == cameraId && currentCall?.mxCall?.callId == callId) {
                // re-start the capture
                // TODO notify that video is enabled
                videoCapturer?.startCapture(currentCaptureMode.width, currentCaptureMode.height, currentCaptureMode.fps)
                context.getSystemService<CameraManager>()?.unregisterAvailabilityCallback(this)
            }
        }
    }
}
