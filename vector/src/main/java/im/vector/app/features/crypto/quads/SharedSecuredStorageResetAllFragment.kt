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
import android.view.View
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import kotlinx.android.synthetic.main.fragment_ssss_reset_all.*
import javax.inject.Inject

class SharedSecuredStorageResetAllFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_ssss_reset_all

    val sharedViewModel: SharedSecureStorageViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ssss_reset_button_reset.debouncedClicks {
            sharedViewModel.handle(SharedSecureStorageAction.DoResetAll)
        }

        ssss_reset_button_cancel.debouncedClicks {
            sharedViewModel.handle(SharedSecureStorageAction.Back)
        }

        ssss_reset_other_devices.debouncedClicks {
            withState(sharedViewModel) {
                DeviceListBottomSheet.newInstance(it.userId, false).show(childFragmentManager, "DEV_LIST")
            }
        }

        sharedViewModel.subscribe(this) { state ->
            ssss_reset_other_devices.setTextOrHide(
                    state.activeDeviceCount
                            .takeIf { it > 0 }
                            ?.let { resources.getQuantityString(R.plurals.secure_backup_reset_devices_you_can_verify, it, it) }
            )
        }
    }
}
