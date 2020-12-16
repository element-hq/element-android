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

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.call.webrtc.WebRtcCallManager
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.MxPeerConnectionState
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import java.util.Timer
import java.util.TimerTask

class VectorCallViewModel @AssistedInject constructor(
        @Assisted initialState: VectorCallViewState,
        val session: Session,
        val callManager: WebRtcCallManager,
        val proximityManager: CallProximityManager
) : VectorViewModel<VectorCallViewState, VectorCallViewActions, VectorCallViewEvents>(initialState) {

    private var call: WebRtcCall? = null

    private var connectionTimeoutTimer: Timer? = null
    private var hasBeenConnectedOnce = false

    private val callListener = object : WebRtcCall.Listener {

        override fun onHoldUnhold() {
            setState {
                copy(
                        isLocalOnHold = call?.isLocalOnHold ?: false,
                        isRemoteOnHold = call?.remoteOnHold ?: false
                )
            }
        }

        override fun onCaptureStateChanged() {
            setState {
                copy(
                        isVideoCaptureInError = call?.videoCapturerIsInError ?: false,
                        isHD = call?.currentCaptureFormat() is CaptureFormat.HD
                )
            }
        }

        override fun onCameraChanged() {
            setState {
                copy(
                        canSwitchCamera = call?.canSwitchCamera() ?: false,
                        isFrontCamera = call?.currentCameraType() == CameraType.FRONT
                )
            }
        }

        override fun onTick(formattedDuration: String) {
            setState {
                copy(formattedDuration = formattedDuration)
            }
        }

        override fun onStateUpdate(call: MxCall) {
            val callState = call.state
            if (callState is CallState.Connected && callState.iceConnectionState == MxPeerConnectionState.CONNECTED) {
                hasBeenConnectedOnce = true
                connectionTimeoutTimer?.cancel()
                connectionTimeoutTimer = null
            } else {
                // do we reset as long as it's moving?
                connectionTimeoutTimer?.cancel()
                if (hasBeenConnectedOnce) {
                    connectionTimeoutTimer = Timer().apply {
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

    private val currentCallListener = object : WebRtcCallManager.CurrentCallListener {

        override fun onCurrentCallChange(call: WebRtcCall?) {
            if (call == null) {
                _viewEvents.post(VectorCallViewEvents.DismissNoCall)
            } else {
                updateOtherKnownCall(call)
            }
        }

        override fun onAudioDevicesChange() {
            val currentSoundDevice = callManager.callAudioManager.getCurrentSoundDevice()
            if (currentSoundDevice == CallAudioManager.SoundDevice.PHONE) {
                proximityManager.start()
            } else {
                proximityManager.stop()
            }

            setState {
                copy(
                        availableSoundDevices = callManager.callAudioManager.getAvailableSoundDevices(),
                        soundDevice = currentSoundDevice
                )
            }
        }
    }

    private fun updateOtherKnownCall(currentCall: WebRtcCall) {
        val otherCall = callManager.getCalls().firstOrNull {
            it.callId != currentCall.callId && it.mxCall.state is CallState.Connected
        }
        setState {
            if (otherCall == null) {
                copy(otherKnownCallInfo = null)
            } else {
                val otherUserItem: MatrixItem? = session.getUser(otherCall.mxCall.opponentUserId)?.toMatrixItem()
                copy(otherKnownCallInfo = VectorCallViewState.CallInfo(otherCall.callId, otherUserItem))
            }
        }
    }

    init {
        val webRtcCall = callManager.getCallById(initialState.callId)
        if (webRtcCall == null) {
            setState {
                copy(callState = Fail(IllegalArgumentException("No call")))
            }
        } else {
            call = webRtcCall
            callManager.addCurrentCallListener(currentCallListener)
            val item: MatrixItem? = session.getUser(webRtcCall.mxCall.opponentUserId)?.toMatrixItem()
            webRtcCall.addListener(callListener)
            val currentSoundDevice = callManager.callAudioManager.getCurrentSoundDevice()
            if (currentSoundDevice == CallAudioManager.SoundDevice.PHONE) {
                proximityManager.start()
            }
            setState {
                copy(
                        isVideoCall = webRtcCall.mxCall.isVideoCall,
                        callState = Success(webRtcCall.mxCall.state),
                        callInfo = VectorCallViewState.CallInfo(callId, item),
                        soundDevice = currentSoundDevice,
                        isLocalOnHold = webRtcCall.isLocalOnHold,
                        isRemoteOnHold = webRtcCall.remoteOnHold,
                        availableSoundDevices = callManager.callAudioManager.getAvailableSoundDevices(),
                        isFrontCamera = webRtcCall.currentCameraType() == CameraType.FRONT,
                        canSwitchCamera = webRtcCall.canSwitchCamera(),
                        formattedDuration = webRtcCall.formattedDuration(),
                        isHD = webRtcCall.mxCall.isVideoCall && webRtcCall.currentCaptureFormat() is CaptureFormat.HD
                )
            }
            updateOtherKnownCall(webRtcCall)
        }
    }

    override fun onCleared() {
        callManager.removeCurrentCallListener(currentCallListener)
        call?.removeListener(callListener)
        proximityManager.stop()
        super.onCleared()
    }

    override fun handle(action: VectorCallViewActions) = withState { state ->
        when (action) {
            VectorCallViewActions.EndCall -> call?.endCall()
            VectorCallViewActions.AcceptCall -> {
                setState {
                    copy(callState = Loading())
                }
                call?.acceptIncomingCall()
            }
            VectorCallViewActions.DeclineCall -> {
                setState {
                    copy(callState = Loading())
                }
                call?.endCall()
            }
            VectorCallViewActions.ToggleMute -> {
                val muted = state.isAudioMuted
                call?.muteCall(!muted)
                setState {
                    copy(isAudioMuted = !muted)
                }
            }
            VectorCallViewActions.ToggleVideo -> {
                if (state.isVideoCall) {
                    val videoEnabled = state.isVideoEnabled
                    call?.enableVideo(!videoEnabled)
                    setState {
                        copy(isVideoEnabled = !videoEnabled)
                    }
                }
                Unit
            }
            VectorCallViewActions.ToggleHoldResume -> {
                val isRemoteOnHold = state.isRemoteOnHold
                call?.updateRemoteOnHold(!isRemoteOnHold)
            }
            is VectorCallViewActions.ChangeAudioDevice -> {
                callManager.callAudioManager.setCurrentSoundDevice(action.device)
                setState {
                    copy(
                            soundDevice = callManager.callAudioManager.getCurrentSoundDevice()
                    )
                }
            }
            VectorCallViewActions.SwitchSoundDevice -> {
                _viewEvents.post(
                        VectorCallViewEvents.ShowSoundDeviceChooser(state.availableSoundDevices, state.soundDevice)
                )
            }
            VectorCallViewActions.HeadSetButtonPressed -> {
                if (state.callState.invoke() is CallState.LocalRinging) {
                    // accept call
                    call?.acceptIncomingCall()
                }
                if (state.callState.invoke() is CallState.Connected) {
                    // end call?
                    call?.endCall()
                }
                Unit
            }
            VectorCallViewActions.ToggleCamera -> {
                call?.switchCamera()
            }
            VectorCallViewActions.ToggleHDSD -> {
                if (!state.isVideoCall) return@withState
                call?.setCaptureFormat(if (state.isHD) CaptureFormat.SD else CaptureFormat.HD)
            }
        }.exhaustive
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VectorCallViewState): VectorCallViewModel
    }

    companion object : MvRxViewModelFactory<VectorCallViewModel, VectorCallViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: VectorCallViewState): VectorCallViewModel {
            val callActivity: VectorCallActivity = viewModelContext.activity()
            return callActivity.viewModelFactory.create(state)
        }
    }
}
