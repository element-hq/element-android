/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentOtherSessionsBinding
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.DevicesViewModel
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterBottomSheet
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType

@AndroidEntryPoint
class OtherSessionsFragment : VectorBaseFragment<FragmentOtherSessionsBinding>(), VectorBaseBottomSheetDialogFragment.ResultListener {

    private val viewModel: DevicesViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOtherSessionsBinding {
        return FragmentOtherSessionsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.otherSessionsToolbar).allowBack()
        initFilterView()
    }

    private fun initFilterView() {
        views.otherSessionsFilterFrameLayout.debouncedClicks {
            DeviceManagerFilterBottomSheet
                    .newInstance(this)
                    .show(requireActivity().supportFragmentManager, "SHOW_DEVICE_MANAGER_FILTER_BOTTOM_SHEET")
        }
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == RESULT_OK && data != null) {
            Toast.makeText(requireContext(), data.toString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.devices is Success) {
            with(state) {
                val devices = state.devices()
                        ?.filter { it.deviceInfo.deviceId != state.currentSessionCrossSigningInfo.deviceId }
                        ?.filteredDevices()
                renderDevices(devices, state.currentFilter)
            }
        }
    }

    private fun renderDevices(devices: List<DeviceFullInfo>?, currentFilter: DeviceManagerFilterType) {
        views.otherSessionsFilterBadgeImageView.isVisible = currentFilter != DeviceManagerFilterType.ALL_SESSIONS

        if (devices.isNullOrEmpty()) {
            // TODO. Render empty state
        } else {
            views.deviceListOtherSessions.render(devices, devices.size)
        }
    }
}
