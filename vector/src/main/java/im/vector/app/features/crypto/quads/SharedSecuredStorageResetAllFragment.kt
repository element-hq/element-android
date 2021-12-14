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

package im.vector.app.features.crypto.quads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSsssResetAllBinding
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import javax.inject.Inject

class SharedSecuredStorageResetAllFragment @Inject constructor() :
    VectorBaseFragment<FragmentSsssResetAllBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSsssResetAllBinding {
        return FragmentSsssResetAllBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: SharedSecureStorageViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.ssssResetButtonReset.debouncedClicks {
            sharedViewModel.handle(SharedSecureStorageAction.DoResetAll)
        }

        views.ssssResetButtonCancel.debouncedClicks {
            sharedViewModel.handle(SharedSecureStorageAction.Back)
        }

        views.ssssResetOtherDevices.debouncedClicks {
            withState(sharedViewModel) {
                DeviceListBottomSheet.newInstance(it.userId, false).show(childFragmentManager, "DEV_LIST")
            }
        }

        sharedViewModel.onEach { state ->
            views.ssssResetOtherDevices.setTextOrHide(
                    state.activeDeviceCount
                            .takeIf { it > 0 }
                            ?.let { resources.getQuantityString(R.plurals.secure_backup_reset_devices_you_can_verify, it, it) }
            )
        }
    }
}
