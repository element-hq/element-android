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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.activityViewModel
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottom_sheet_call_controls.*

class CallControlsBottomSheet : VectorBaseBottomSheetDialogFragment() {
    override fun getLayoutResId() = R.layout.bottom_sheet_call_controls

    private val callViewModel: VectorCallViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel.subscribe(this) {
            renderState(it)
        }

        callControlsSoundDevice.clickableView.debouncedClicks {
            val soundDevices = listOf(
                    getString(R.string.sound_device_phone),
                    getString(R.string.sound_device_speaker)
            )
            AlertDialog.Builder(requireContext())
                    .setItems(soundDevices.toTypedArray()) { d, n ->
                        d.cancel()
                        when (soundDevices[n]) {
                            getString(R.string.sound_device_phone) -> {
                                callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.SoundDevice.PHONE))
                            }
                            getString(R.string.sound_device_speaker) -> {
                                callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(CallAudioManager.SoundDevice.SPEAKER))
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return super.onCreateDialog(savedInstanceState).apply {
//            window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//            window?.decorView?.systemUiVisibility =
//                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
//                            View.SYSTEM_UI_FLAG_FULLSCREEN or
//                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        }
//    }

    private fun renderState(state: VectorCallViewState) {
        callControlsSoundDevice.title = getString(R.string.call_select_sound_device)
        callControlsSoundDevice.subTitle = when (state.soundDevice) {
            CallAudioManager.SoundDevice.PHONE   -> getString(R.string.sound_device_phone)
            CallAudioManager.SoundDevice.SPEAKER -> getString(R.string.sound_device_speaker)
        }
    }
}
