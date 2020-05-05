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

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.Window
import android.view.WindowManager
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.matrix.android.api.session.call.EglUtils
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.riotx.core.utils.allGranted
import im.vector.riotx.core.utils.checkPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Parcelize
data class CallArgs(
//        val callId: String? = null,
        val roomId: String
) : Parcelable

class VectorCallActivity : VectorBaseActivity(), WebRtcPeerConnectionManager.Listener {

    override fun getLayoutRes() = R.layout.activity_call

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    private val callViewModel: VectorCallViewModel by viewModel()

    @Inject lateinit var peerConnectionManager: WebRtcPeerConnectionManager

    @Inject lateinit var viewModelFactory: VectorCallViewModel.Factory

    @BindView(R.id.pip_video_view)
    lateinit var pipRenderer: SurfaceViewRenderer

    @BindView(R.id.fullscreen_video_view)
    lateinit var fullscreenRenderer: SurfaceViewRenderer

    private var rootEglBase: EglBase? = null

//    private var peerConnectionFactory: PeerConnectionFactory? = null

    //private var peerConnection: PeerConnection? = null

//    private var remoteVideoTrack: VideoTrack? = null

    private val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()

    override fun doBeforeSetContentView() {
        // Set window styles for fullscreen-window size. Needs to be done before adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(R.layout.activity_call)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootEglBase = EglUtils.rootEglBase ?: return Unit.also {
            finish()
        }

        callViewModel.viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleViewEvents(it)
                }
                .disposeOnDestroy()
//
//        if (isFirstCreation()) {
//
//        }

        if (checkPermissions(PERMISSIONS_FOR_VIDEO_IP_CALL, this, CAPTURE_PERMISSION_REQUEST_CODE, R.string.permissions_rationale_msg_camera_and_audio)) {
            start()
        }
        peerConnectionManager.listener = this
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE && allGranted(grantResults)) {
            start()
        } else {
            // TODO display something
            finish()
        }
    }

    private fun start(): Boolean {
        // Init Picture in Picture renderer
        pipRenderer.init(rootEglBase!!.eglBaseContext, null)
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        // Init Full Screen renderer
        fullscreenRenderer.init(rootEglBase!!.eglBaseContext, null)
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)


        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        //setSwappedFeeds(true /* isSwappedFeeds */);

        if (isFirstCreation()) {
            peerConnectionManager.createPeerConnectionFactory()

            val cameraIterator = if (Camera2Enumerator.isSupported(this)) Camera2Enumerator(this) else Camera1Enumerator(false)
            val frontCamera = cameraIterator.deviceNames
                    ?.firstOrNull { cameraIterator.isFrontFacing(it) }
                    ?: cameraIterator.deviceNames?.first()
                    ?: return true
            val videoCapturer = cameraIterator.createCapturer(frontCamera, null)

            val iceServers = ArrayList<PeerConnection.IceServer>().apply {
                listOf("turn:turn.matrix.org:3478?transport=udp", "turn:turn.matrix.org:3478?transport=tcp", "turns:turn.matrix.org:443?transport=tcp").forEach {
                    add(
                            PeerConnection.IceServer.builder(it)
                                    .setUsername("xxxxx")
                                    .setPassword("xxxxx")
                                    .createIceServer()
                    )
                }
            }

            peerConnectionManager.createPeerConnection(videoCapturer, iceServers)
            peerConnectionManager.startCall()
        }
//        PeerConnectionFactory.initialize(PeerConnectionFactory
//                .InitializationOptions.builder(applicationContext)
//                .createInitializationOptions()
//        )

//        val options = PeerConnectionFactory.Options()
//        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
//                rootEglBase!!.eglBaseContext,  /* enableIntelVp8Encoder */
//                true,  /* enableH264HighProfile */
//                true)
//        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase!!.eglBaseContext)
//
//        peerConnectionFactory = PeerConnectionFactory.builder()
//                .setOptions(options)
//                .setVideoEncoderFactory(defaultVideoEncoderFactory)
//                .setVideoDecoderFactory(defaultVideoDecoderFactory)
//                .createPeerConnectionFactory()

//        val cameraIterator = if (Camera2Enumerator.isSupported(this)) Camera2Enumerator(this) else Camera1Enumerator(false)
//        val frontCamera = cameraIterator.deviceNames
//                ?.firstOrNull { cameraIterator.isFrontFacing(it) }
//                ?: cameraIterator.deviceNames?.first()
//                ?: return true
//        val videoCapturer = cameraIterator.createCapturer(frontCamera, null)
//
//        // Following instruction here: https://stackoverflow.com/questions/55085726/webrtc-create-peerconnectionfactory-object
//        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
//
//        val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
//        videoCapturer.initialize(surfaceTextureHelper, this, videoSource!!.capturerObserver)
//        videoCapturer.startCapture(1280, 720, 30)
//
//
//        val localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
//
//        // create a local audio track
//        val audioSource = peerConnectionFactory?.createAudioSource(DEFAULT_AUDIO_CONSTRAINTS)
//        val audioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        pipRenderer.setMirror(true)
//        localVideoTrack?.addSink(pipRenderer)

        /*
                {
                "username": "1586847781:@valere35:matrix.org",
                 "password": "ZzbqbqfT9O2G3WpCpesdts2lyns=",
                  "ttl": 86400.0,
                   "uris": ["turn:turn.matrix.org:3478?transport=udp", "turn:turn.matrix.org:3478?transport=tcp", "turns:turn.matrix.org:443?transport=tcp"]
                   }
         */

//        val iceServers = ArrayList<PeerConnection.IceServer>().apply {
//            listOf("turn:turn.matrix.org:3478?transport=udp", "turn:turn.matrix.org:3478?transport=tcp", "turns:turn.matrix.org:443?transport=tcp").forEach {
//                add(
//                        PeerConnection.IceServer.builder(it)
//                                .setUsername("1586847781:@valere35:matrix.org")
//                                .setPassword("ZzbqbqfT9O2G3WpCpesdts2lyns=")
//                                .createIceServer()
//                )
//            }
//        }
//
//        val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()
//
//        iceCandidateSource
//                .buffer(400, TimeUnit.MILLISECONDS)
//                .subscribe {
//                    // omit empty :/
//                    if (it.isNotEmpty()) {
//                        callViewModel.handle(VectorCallViewActions.AddLocalIceCandidate(it))
//                    }
//                }
//                .disposeOnDestroy()
//
//        peerConnection = peerConnectionFactory?.createPeerConnection(
//                iceServers,
//                object : PeerConnectionObserverAdapter() {
//                    override fun onIceCandidate(p0: IceCandidate?) {
//                        p0?.let {
//                            iceCandidateSource.onNext(it)
//                        }
//                    }
//
//                    override fun onAddStream(mediaStream: MediaStream?) {
//                        runOnUiThread {
//                            mediaStream?.videoTracks?.firstOrNull()?.let { videoTrack ->
//                                remoteVideoTrack = videoTrack
//                                remoteVideoTrack?.setEnabled(true)
//                                remoteVideoTrack?.addSink(fullscreenRenderer)
//                            }
//                        }
//                    }
//
//                    override fun onRemoveStream(mediaStream: MediaStream?) {
//                        remoteVideoTrack = null
//                    }
//
//                    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
//                        if (p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
//                            // TODO prompt something?
//                            finish()
//                        }
//                    }
//                }
//        )
//
//        val localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS") // magic value?
//        localMediaStream?.addTrack(localVideoTrack)
//        localMediaStream?.addTrack(audioTrack)
//
//        val constraints = MediaConstraints()
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
//
//        peerConnection?.addStream(localMediaStream)
//
//        peerConnection?.createOffer(object : SdpObserver {
//            override fun onSetFailure(p0: String?) {
//                Timber.v("## VOIP onSetFailure $p0")
//            }
//
//            override fun onSetSuccess() {
//                Timber.v("## VOIP onSetSuccess")
//            }
//
//            override fun onCreateSuccess(sessionDescription: SessionDescription) {
//                Timber.v("## VOIP onCreateSuccess $sessionDescription")
//                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
//                    override fun onSetSuccess() {
//                        callViewModel.handle(VectorCallViewActions.SendOffer(sessionDescription))
//                    }
//                }, sessionDescription)
//            }
//
//            override fun onCreateFailure(p0: String?) {
//                Timber.v("## VOIP onCreateFailure $p0")
//            }
//        }, constraints)
        iceCandidateSource
                .buffer(400, TimeUnit.MILLISECONDS)
                .subscribe {
                    // omit empty :/
                    if (it.isNotEmpty()) {
                        callViewModel.handle(VectorCallViewActions.AddLocalIceCandidate(it))
                    }
                }
                .disposeOnDestroy()

        peerConnectionManager.attachViewRenderers(pipRenderer, fullscreenRenderer)
        return false
    }

    override fun onDestroy() {
        peerConnectionManager.detachRenderers()
        peerConnectionManager.listener = this
        super.onDestroy()
    }

    private fun handleViewEvents(event: VectorCallViewEvents?) {
        when (event) {
            is VectorCallViewEvents.CallAnswered -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, event.content.answer.sdp)
                peerConnectionManager.answerReceived("", sdp)
//                peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, sdp)
            }
        }
    }

//    @TargetApi(17)
//    private fun getDisplayMetrics(): DisplayMetrics? {
//        val displayMetrics = DisplayMetrics()
//        val windowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
//        return displayMetrics
//    }

//    @TargetApi(21)
//    private fun startScreenCapture() {
//        val mediaProjectionManager: MediaProjectionManager = application.getSystemService(
//                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        startActivityForResult(
//                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
////        mediaProjectionPermissionResultCode = resultCode;
////        mediaProjectionPermissionResultData = data;
////        startCall();
//    }

    companion object {

        private const val CAPTURE_PERMISSION_REQUEST_CODE = 1

//        private val DEFAULT_AUDIO_CONSTRAINTS = MediaConstraints().apply {
//            // add all existing audio filters to avoid having echos
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
//        }

        fun newIntent(context: Context, signalingRoomId: String): Intent {
            return Intent(context, VectorCallActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, CallArgs(roomId = signalingRoomId))
            }
        }
    }

    override fun addLocalIceCandidate(candidates: IceCandidate) {
        iceCandidateSource.onNext(candidates)
    }

    override fun addRemoteVideoTrack(videoTrack: VideoTrack) {
        runOnUiThread {
            videoTrack.setEnabled(true)
            videoTrack.addSink(fullscreenRenderer)
        }
    }

    override fun addLocalVideoTrack(videoTrack: VideoTrack) {
        runOnUiThread {
            videoTrack.addSink(pipRenderer)
        }
    }

    override fun removeRemoteVideoStream(mediaStream: MediaStream) {
    }

    override fun onDisconnect() {
    }

    override fun sendOffer(sessionDescription: SessionDescription) {
        callViewModel.handle(VectorCallViewActions.SendOffer(sessionDescription))
    }
}
