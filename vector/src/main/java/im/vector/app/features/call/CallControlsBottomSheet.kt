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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetCallControlsBinding
import im.vector.app.features.call.audio.CallAudioManager

import me.gujun.android.span.span

class CallControlsBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetCallControlsBinding>() {
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCallControlsBinding {
        return BottomSheetCallControlsBinding.inflate(inflater, container, false)
    }

    private val callViewModel: VectorCallViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel.subscribe(this) {
            renderState(it)
        }

        views.callControlsSoundDevice.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.SwitchSoundDevice)
        }

        views.callControlsSwitchCamera.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleCamera)
            dismiss()
        }

        views.callControlsToggleSDHD.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleHDSD)
            dismiss()
        }

        views.callControlsToggleHoldResume.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleHoldResume)
            dismiss()
        }

        views.callControlsOpenDialPad.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.OpenDialPad)
        }

        views.callControlsTransfer.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.InitiateCallTransfer)
            dismiss()
        }

        callViewModel.observeViewEvents {
            when (it) {
                is VectorCallViewEvents.ShowSoundDeviceChooser -> {
                    showSoundDeviceChooser(it.available, it.current)
                }
                else                                           -> {
                }
            }
        }
    }

    private fun showSoundDeviceChooser(available: Set<CallAudioManager.Device>, current: CallAudioManager.Device) {
        val soundDevices = available.map {
            when (it) {
                CallAudioManager.Device.WIRELESS_HEADSET -> span {
                    text = getString(R.string.sound_device_wireless_headset)
                    textStyle = if (current == it) "bold" else "normal"
                }
                CallAudioManager.Device.PHONE            -> span {
                    text = getString(R.string.sound_device_phone)
                    textStyle = if (current == it) "bold" else "normal"
                }
                CallAudioManager.Device.SPEAKER          -> span {
                    text = getString(R.string.sound_device_speaker)
                    textStyle = if (current == it) "bold" else "normal"
                }
                CallAudioManager.Device.HEADSET          -> span {
                    text = getString(R.string.sound_device_headset)
                    textStyle = if (current == it) "bold" else "normal"
                }
            }
        }
        MaterialAlertDialogBuilder(requireContext())
                .setItems(soundDevices.toTypedArray()) { d, n ->
                    d.cancel()
                    when (soundDevices[n].toString()) {
                        // TODO Make an adapter and handle multiple Bluetooth headsets. Also do not use translations.
                        getString(R.string.sound_device_phone) -> {
                            callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.Device.PHONE))
                        }
                        getString(R.string.sound_device_speaker) -> {
                            callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.Device.SPEAKER))
                        }
                        getString(R.string.sound_device_headset) -> {
                            callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.Device.HEADSET))
                        }
                        getString(R.string.sound_device_wireless_headset) -> {
                            callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.Device.WIRELESS_HEADSET))
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun renderState(state: VectorCallViewState) {
        views.callControlsSoundDevice.title = getString(R.string.call_select_sound_device)
        views.callControlsSoundDevice.subTitle = when (state.device) {
            CallAudioManager.Device.PHONE            -> getString(R.string.sound_device_phone)
            CallAudioManager.Device.SPEAKER          -> getString(R.string.sound_device_speaker)
            CallAudioManager.Device.HEADSET          -> getString(R.string.sound_device_headset)
            CallAudioManager.Device.WIRELESS_HEADSET -> getString(R.string.sound_device_wireless_headset)
        }

        views.callControlsSwitchCamera.isVisible = state.isVideoCall && state.canSwitchCamera
        views.callControlsSwitchCamera.subTitle = getString(if (state.isFrontCamera) R.string.call_camera_front else R.string.call_camera_back)

        if (state.isVideoCall) {
            views.callControlsToggleSDHD.isVisible = true
            if (state.isHD) {
                views.callControlsToggleSDHD.title = getString(R.string.call_format_turn_hd_off)
                views.callControlsToggleSDHD.subTitle = null
                views.callControlsToggleSDHD.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_hd_disabled)
            } else {
                views.callControlsToggleSDHD.title = getString(R.string.call_format_turn_hd_on)
                views.callControlsToggleSDHD.subTitle = null
                views.callControlsToggleSDHD.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_hd)
            }
        } else {
            views.callControlsToggleSDHD.isVisible = false
        }
        if (state.isRemoteOnHold) {
            views.callControlsToggleHoldResume.title = getString(R.string.call_resume_action)
            views.callControlsToggleHoldResume.subTitle = null
            views.callControlsToggleHoldResume.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_call_resume_action)
        } else {
            views.callControlsToggleHoldResume.title = getString(R.string.call_hold_action)
            views.callControlsToggleHoldResume.subTitle = null
            views.callControlsToggleHoldResume.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_call_hold_action)
        }
        views.callControlsTransfer.isVisible = state.canOpponentBeTransferred
    }
}
