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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.webrtc.PeerConnection
import java.util.Timer
import java.util.TimerTask

data class VectorCallViewState(
        val callId: String? = null,
        val roomId: String = "",
        val isVideoCall: Boolean,
        val isAudioMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isVideoCaptureInError: Boolean = false,
        val isHD: Boolean = false,
        val isFrontCamera: Boolean = true,
        val canSwitchCamera: Boolean = true,
        val soundDevice: CallAudioManager.SoundDevice = CallAudioManager.SoundDevice.PHONE,
        val availableSoundDevices: List<CallAudioManager.SoundDevice> = emptyList(),
        val otherUserMatrixItem: Async<MatrixItem> = Uninitialized,
        val callState: Async<CallState> = Uninitialized
) : MvRxState

sealed class VectorCallViewActions : VectorViewModelAction {
    object EndCall : VectorCallViewActions()
    object AcceptCall : VectorCallViewActions()
    object DeclineCall : VectorCallViewActions()
    object ToggleMute : VectorCallViewActions()
    object ToggleVideo : VectorCallViewActions()
    data class ChangeAudioDevice(val device: CallAudioManager.SoundDevice) : VectorCallViewActions()
    object SwitchSoundDevice : VectorCallViewActions()
    object HeadSetButtonPressed : VectorCallViewActions()
    object ToggleCamera : VectorCallViewActions()
    object ToggleHDSD : VectorCallViewActions()
}

sealed class VectorCallViewEvents : VectorViewEvents {

    object DismissNoCall : VectorCallViewEvents()
    data class ConnectionTimeout(val turn: TurnServerResponse?) : VectorCallViewEvents()
    data class ShowSoundDeviceChooser(
            val available: List<CallAudioManager.SoundDevice>,
            val current: CallAudioManager.SoundDevice
    ) : VectorCallViewEvents()
//    data class CallAnswered(val content: CallAnswerContent) : VectorCallViewEvents()
//    data class CallHangup(val content: CallHangupContent) : VectorCallViewEvents()
//    object CallAccepted : VectorCallViewEvents()
}

class VectorCallViewModel @AssistedInject constructor(
        @Assisted initialState: VectorCallViewState,
        @Assisted val args: CallArgs,
        val session: Session,
        val webRtcPeerConnectionManager: WebRtcPeerConnectionManager,
        val proximityManager: CallProximityManager
) : VectorViewModel<VectorCallViewState, VectorCallViewActions, VectorCallViewEvents>(initialState) {

    var call: MxCall? = null

    var connectionTimoutTimer: Timer? = null
    var hasBeenConnectedOnce = false

    private val callStateListener = object : MxCall.StateListener {
        override fun onStateUpdate(call: MxCall) {
            val callState = call.state
            if (callState is CallState.Connected && callState.iceConnectionState == PeerConnection.PeerConnectionState.CONNECTED) {
                hasBeenConnectedOnce = true
                connectionTimoutTimer?.cancel()
                connectionTimoutTimer = null
            } else {
                // do we reset as long as it's moving?
                connectionTimoutTimer?.cancel()
                if (hasBeenConnectedOnce) {
                    connectionTimoutTimer = Timer().apply {
                        schedule(object : TimerTask() {
                            override fun run() {
                                session.callSignalingService().getTurnServer(object : MatrixCallback<TurnServerResponse> {
                                    override fun onFailure(failure: Throwable) {
                                        _viewEvents.post(VectorCallViewEvents.ConnectionTimeout(null))
                                    }

                                    override fun onSuccess(data: TurnServerResponse) {
                                        _viewEvents.post(VectorCallViewEvents.ConnectionTimeout(data))
                                    }
                                })
                            }
                        }, 30_000)
                    }
                }
            }
            setState {
                copy(
                        callState = Success(callState)
                )
            }
        }
    }

    private val currentCallListener = object : WebRtcPeerConnectionManager.CurrentCallListener {
        override fun onCurrentCallChange(call: MxCall?) {
        }

        override fun onCaptureStateChanged(mgr: WebRtcPeerConnectionManager) {
            setState {
                copy(
                        isVideoCaptureInError = mgr.capturerIsInError,
                        isHD = mgr.currentCaptureFormat() is CaptureFormat.HD
                )
            }
        }

        override fun onAudioDevicesChange(mgr: WebRtcPeerConnectionManager) {
            val currentSoundDevice = mgr.audioManager.getCurrentSoundDevice()
            if (currentSoundDevice == CallAudioManager.SoundDevice.PHONE) {
                proximityManager.start()
            } else {
                proximityManager.stop()
            }

            setState {
                copy(
                        availableSoundDevices = mgr.audioManager.getAvailableSoundDevices(),
                        soundDevice = currentSoundDevice
                )
            }
        }

        override fun onCameraChange(mgr: WebRtcPeerConnectionManager) {
            setState {
                copy(
                        canSwitchCamera = mgr.canSwitchCamera(),
                        isFrontCamera = mgr.currentCameraType() == CameraType.FRONT
                )
            }
        }
    }

    init {
        initialState.callId?.let {
            webRtcPeerConnectionManager.addCurrentCallListener(currentCallListener)

            session.callSignalingService().getCallWithId(it)?.let { mxCall ->
                this.call = mxCall
                mxCall.otherUserId
                val item: MatrixItem? = session.getUser(mxCall.otherUserId)?.toMatrixItem()

                mxCall.addListener(callStateListener)

                val currentSoundDevice = webRtcPeerConnectionManager.audioManager.getCurrentSoundDevice()
                if (currentSoundDevice == CallAudioManager.SoundDevice.PHONE) {
                    proximityManager.start()
                }

                setState {
                    copy(
                            isVideoCall = mxCall.isVideoCall,
                            callState = Success(mxCall.state),
                            otherUserMatrixItem = item?.let { Success(it) } ?: Uninitialized,
                            soundDevice = currentSoundDevice,
                            availableSoundDevices = webRtcPeerConnectionManager.audioManager.getAvailableSoundDevices(),
                            isFrontCamera = webRtcPeerConnectionManager.currentCameraType() == CameraType.FRONT,
                            canSwitchCamera = webRtcPeerConnectionManager.canSwitchCamera(),
                            isHD = mxCall.isVideoCall && webRtcPeerConnectionManager.currentCaptureFormat() is CaptureFormat.HD
                    )
                }
            } ?: run {
                setState {
                    copy(
                            callState = Fail(IllegalArgumentException("No call"))
                    )
                }
            }
        }
    }

    override fun onCleared() {
        // session.callService().removeCallListener(callServiceListener)
        webRtcPeerConnectionManager.removeCurrentCallListener(currentCallListener)
        this.call?.removeListener(callStateListener)
        proximityManager.stop()
        super.onCleared()
    }

    override fun handle(action: VectorCallViewActions) = withState { state ->
        when (action) {
            VectorCallViewActions.EndCall              -> webRtcPeerConnectionManager.endCall()
            VectorCallViewActions.AcceptCall           -> {
                setState {
                    copy(callState = Loading())
                }
                webRtcPeerConnectionManager.acceptIncomingCall()
            }
            VectorCallViewActions.DeclineCall          -> {
                setState {
                    copy(callState = Loading())
                }
                webRtcPeerConnectionManager.endCall()
            }
            VectorCallViewActions.ToggleMute           -> {
                val muted = state.isAudioMuted
                webRtcPeerConnectionManager.muteCall(!muted)
                setState {
                    copy(isAudioMuted = !muted)
                }
            }
            VectorCallViewActions.ToggleVideo          -> {
                if (state.isVideoCall) {
                    val videoEnabled = state.isVideoEnabled
                    webRtcPeerConnectionManager.enableVideo(!videoEnabled)
                    setState {
                        copy(isVideoEnabled = !videoEnabled)
                    }
                }
                Unit
            }
            is VectorCallViewActions.ChangeAudioDevice -> {
                webRtcPeerConnectionManager.audioManager.setCurrentSoundDevice(action.device)
                setState {
                    copy(
                            soundDevice = webRtcPeerConnectionManager.audioManager.getCurrentSoundDevice()
                    )
                }
            }
            VectorCallViewActions.SwitchSoundDevice    -> {
                _viewEvents.post(
                        VectorCallViewEvents.ShowSoundDeviceChooser(state.availableSoundDevices, state.soundDevice)
                )
            }
            VectorCallViewActions.HeadSetButtonPressed -> {
                if (state.callState.invoke() is CallState.LocalRinging) {
                    // accept call
                    webRtcPeerConnectionManager.acceptIncomingCall()
                }
                if (state.callState.invoke() is CallState.Connected) {
                    // end call?
                    webRtcPeerConnectionManager.endCall()
                }
                Unit
            }
            VectorCallViewActions.ToggleCamera         -> {
                webRtcPeerConnectionManager.switchCamera()
            }
            VectorCallViewActions.ToggleHDSD           -> {
                if (!state.isVideoCall) return@withState
                webRtcPeerConnectionManager.setCaptureFormat(if (state.isHD) CaptureFormat.SD else CaptureFormat.HD)
            }
        }.exhaustive
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VectorCallViewState, args: CallArgs): VectorCallViewModel
    }

    companion object : MvRxViewModelFactory<VectorCallViewModel, VectorCallViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: VectorCallViewState): VectorCallViewModel? {
            val callActivity: VectorCallActivity = viewModelContext.activity()
            val callArgs: CallArgs = viewModelContext.args()
            return callActivity.viewModelFactory.create(state, callArgs)
        }

        override fun initialState(viewModelContext: ViewModelContext): VectorCallViewState? {
            val args: CallArgs = viewModelContext.args()
            return VectorCallViewState(
                    callId = args.callId,
                    roomId = args.roomId,
                    isVideoCall = args.isVideoCall
            )
        }
    }
}
