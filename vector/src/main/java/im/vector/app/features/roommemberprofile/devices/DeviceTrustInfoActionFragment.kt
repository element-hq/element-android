/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.app.features.roommemberprofile.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.BottomSheetGenericListBinding

import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import javax.inject.Inject

class DeviceTrustInfoActionFragment @Inject constructor(
        val dimensionConverter: DimensionConverter,
        val epoxyController: DeviceTrustInfoEpoxyController
) : VectorBaseFragment<BottomSheetGenericListBinding>(),
        DeviceTrustInfoEpoxyController.InteractionListener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    private val viewModel: DeviceListBottomSheetViewModel by parentFragmentViewModel(DeviceListBottomSheetViewModel::class)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.bottomSheetRecyclerView.setPadding(0, dimensionConverter.dpToPx(16), 0, dimensionConverter.dpToPx(16))
        views.bottomSheetRecyclerView.configureWith(
                epoxyController,
                hasFixedSize = false
        )
        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
    }

    override fun onVerifyManually(device: CryptoDeviceInfo) {
        viewModel.handle(DeviceListAction.ManuallyVerify(device.deviceId))
    }
}
