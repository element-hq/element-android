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
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.content.ContextCompat
import im.vector.matrix.android.api.session.call.CallsListener
import im.vector.matrix.android.api.session.call.EglUtils
import im.vector.matrix.android.api.session.room.model.call.CallAnswerContent
import im.vector.matrix.android.api.session.room.model.call.CallHangupContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manage peerConnectionFactory & Peer connections outside of activity lifecycle to resist configuration changes
 * Use app context
 */
@Singleton
class WebRtcPeerConnectionManager @Inject constructor(
        private val context: Context
) : CallsListener {

    interface Listener {
        fun addLocalIceCandidate(candidates: IceCandidate)
        fun addRemoteVideoTrack(videoTrack: VideoTrack)
        fun addLocalVideoTrack(videoTrack: VideoTrack)
        fun removeRemoteVideoStream(mediaStream: MediaStream)
        fun onDisconnect()
        fun sendOffer(sessionDescription: SessionDescription)
    }

    var phoneAccountHandle: PhoneAccountHandle? = null
    var localMediaStream: MediaStream? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val componentName = ComponentName(BuildConfig.APPLICATION_ID, VectorConnectionService::class.java.name)
            val appName = context.getString(R.string.app_name)
            phoneAccountHandle = PhoneAccountHandle(componentName, appName)
            val phoneAccount = PhoneAccount.Builder(phoneAccountHandle, BuildConfig.APPLICATION_ID)
                    .setIcon(Icon.createWithResource(context, R.drawable.riotx_logo))
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                    .setCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_SUBJECT)
                    .build()
            ContextCompat.getSystemService(context, TelecomManager::class.java)
                    ?.registerPhoneAccount(phoneAccount)
        } else {
            // ignore?
        }
    }

    var listener: Listener? = null

    // *Comments copied from webrtc demo app*
    // Executor thread is started once and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private val executor = Executors.newSingleThreadExecutor()

    private val rootEglBase by lazy { EglUtils.rootEglBase }

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var peerConnection: PeerConnection? = null

    private var remoteVideoTrack: VideoTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null

    private var videoCapturer: VideoCapturer? = null

    var localSurfaceRenderer: WeakReference<SurfaceViewRenderer>? = null
    var remoteSurfaceRenderer: WeakReference<SurfaceViewRenderer>? = null

    fun createPeerConnectionFactory() {
        executor.execute {
            if (peerConnectionFactory == null) {
                Timber.v("## VOIP createPeerConnectionFactory")
                val eglBaseContext = rootEglBase?.eglBaseContext ?: return@execute Unit.also {
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
            }
        }
    }

    fun createPeerConnection(videoCapturer: VideoCapturer, iceServers: List<PeerConnection.IceServer>) {
        executor.execute {
            Timber.v("## VOIP PeerConnectionFactory.createPeerConnection $peerConnectionFactory...")
            // Following instruction here: https://stackoverflow.com/questions/55085726/webrtc-create-peerconnectionfactory-object
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)

            videoSource = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
            Timber.v("## VOIP Local video source created")
            videoCapturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource!!.capturerObserver)
            videoCapturer.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)?.also {
                Timber.v("## VOIP Local video track created")
                listener?.addLocalVideoTrack(it)
//                localSurfaceRenderer?.get()?.let { surface ->
// //                    it.addSink(surface)
// //                }
            }

            // create a local audio track
            Timber.v("## VOIP create local audio track")
            audioSource = peerConnectionFactory?.createAudioSource(DEFAULT_AUDIO_CONSTRAINTS)
            audioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)

//            pipRenderer.setMirror(true)
//            localVideoTrack?.addSink(pipRenderer)
//

//            val iceCandidateSource: PublishSubject<IceCandidate> = PublishSubject.create()
//
//            iceCandidateSource
//                    .buffer(400, TimeUnit.MILLISECONDS)
//                    .subscribe {
//                        // omit empty :/
//                        if (it.isNotEmpty()) {
//                            listener.addLocalIceCandidate()
//                            callViewModel.handle(VectorCallViewActions.AddLocalIceCandidate(it))
//                        }
//                    }
//                    .disposeOnDestroy()

            Timber.v("## VOIP creating peer connection... ")
            peerConnection = peerConnectionFactory?.createPeerConnection(
                    iceServers,
                    object : PeerConnectionObserverAdapter() {
                        override fun onIceCandidate(p0: IceCandidate?) {
                            Timber.v("## VOIP onIceCandidate local $p0")
                            p0?.let {
                                //                                iceCandidateSource.onNext(it)
                                listener?.addLocalIceCandidate(it)
                            }
                        }

                        override fun onAddStream(mediaStream: MediaStream?) {
                            Timber.v("## VOIP onAddStream remote $mediaStream")
                            mediaStream?.videoTracks?.firstOrNull()?.let {
                                listener?.addRemoteVideoTrack(it)
                                remoteVideoTrack = it
//                                remoteSurfaceRenderer?.get()?.let { surface ->
//                                    it.setEnabled(true)
//                                    it.addSink(surface)
//                                }
                            }
//                            runOnUiThread {
//                                mediaStream?.videoTracks?.firstOrNull()?.let { videoTrack ->
//                                    remoteVideoTrack = videoTrack
//                                    remoteVideoTrack?.setEnabled(true)
//                                    remoteVideoTrack?.addSink(fullscreenRenderer)
//                                }
//                            }
                        }

                        override fun onRemoveStream(mediaStream: MediaStream?) {
                            mediaStream?.let {
                                listener?.removeRemoteVideoStream(it)
                            }
                            remoteSurfaceRenderer?.get()?.let {
                                remoteVideoTrack?.removeSink(it)
                            }
                            remoteVideoTrack = null
                        }

                        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                            Timber.v("## VOIP onIceConnectionChange $p0")
                            if (p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
                                listener?.onDisconnect()
                            }
                        }
                    }
            )

            localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS") // magic value?
            localMediaStream?.addTrack(localVideoTrack)
            localMediaStream?.addTrack(audioTrack)

//            val constraints = MediaConstraints()
//            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

            Timber.v("## VOIP add local stream to peer connection")
            peerConnection?.addStream(localMediaStream)
        }
    }

    fun answerReceived(callId: String, answerSdp: SessionDescription) {
        executor.execute {
            Timber.v("## answerReceived $callId")
            peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, answerSdp)
        }
    }

    fun startCall() {
        executor.execute {
            val constraints = MediaConstraints()
            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

            Timber.v("## VOIP creating offer...")
            peerConnection?.createOffer(object : SdpObserver {
                override fun onSetFailure(p0: String?) {
                    Timber.v("## VOIP onSetFailure $p0")
                }

                override fun onSetSuccess() {
                    Timber.v("## VOIP onSetSuccess")
                }

                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    Timber.v("## VOIP onCreateSuccess $sessionDescription")
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                        override fun onSetSuccess() {
                            listener?.sendOffer(sessionDescription)
                            // callViewModel.handle(VectorCallViewActions.SendOffer(sessionDescription))
                        }
                    }, sessionDescription)
                }

                override fun onCreateFailure(p0: String?) {
                    Timber.v("## VOIP onCreateFailure $p0")
                }
            }, constraints)
        }
    }

    fun attachViewRenderers(localViewRenderer: SurfaceViewRenderer, remoteViewRenderer: SurfaceViewRenderer) {
        localVideoTrack?.addSink(localViewRenderer)
        remoteVideoTrack?.let {
            it.setEnabled(true)
            it.addSink(remoteViewRenderer)
        }
        localSurfaceRenderer = WeakReference(localViewRenderer)
        remoteSurfaceRenderer = WeakReference(remoteViewRenderer)
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
            peerConnection?.close()
            peerConnection?.removeStream(localMediaStream)
            peerConnection = null
            audioSource?.dispose()
            videoSource?.dispose()
            videoCapturer?.dispose()
            peerConnectionFactory?.stopAecDump()
            peerConnectionFactory = null
        }
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

    override fun onCallInviteReceived(signalingRoomId: String, callInviteContent: CallInviteContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.getSystemService(context, TelecomManager::class.java)?.let { telecomManager ->
                phoneAccountHandle?.let { phoneAccountHandle ->
                    telecomManager.addNewIncomingCall(
                            phoneAccountHandle,
                            Bundle().apply {
                                putString("MX_CALL_ROOM_ID", signalingRoomId)
                                putString("MX_CALL_CALL_ID", callInviteContent.callId)
                                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                                putInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
                                putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
                            }
                    )
                }
            }
        }
    }

    override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) {
    }

    override fun onCallHangupReceived(callHangupContent: CallHangupContent) {
        close()
    }
}
