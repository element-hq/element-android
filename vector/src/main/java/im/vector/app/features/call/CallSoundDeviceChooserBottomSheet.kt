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
import com.airbnb.epoxy.SimpleEpoxyController
import com.airbnb.mvrx.activityViewModel
import im.vector.app.core.epoxy.bottomsheet.BottomSheetActionItem_
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import im.vector.app.features.call.audio.CallAudioManager
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet

class CallSoundDeviceChooserBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>() {
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    private val callViewModel: VectorCallViewModel by activityViewModel()
    private val controller = SimpleEpoxyController()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.bottomSheetRecyclerView.configureWith(controller, hasFixedSize = false)
        callViewModel.observeViewEvents {
            when (it) {
                is VectorCallViewEvents.ShowSoundDeviceChooser -> {
                    render(it.available, it.current)
                }
                else                                           -> {
                }
            }
        }
        callViewModel.handle(VectorCallViewActions.SwitchSoundDevice)
    }

    private fun render(available: Set<CallAudioManager.Device>, current: CallAudioManager.Device) {
        val models = available.map { device ->
            val title = when (device) {
                is CallAudioManager.Device.WirelessHeadset -> device.name ?: getString(device.titleRes)
                else                                       -> getString(device.titleRes)
            }
            BottomSheetActionItem_().apply {
                id(device.titleRes)
                text(title)
                iconRes(device.drawableRes)
                selected(current == device)
                listener {
                    callViewModel.handle(VectorCallViewActions.ChangeAudioDevice(device))
                    dismiss()
                }
            }
        }
        controller.setModels(models)
    }

    companion object {
        fun newInstance(): RoomListQuickActionsBottomSheet {
            return RoomListQuickActionsBottomSheet()
        }
    }
}
