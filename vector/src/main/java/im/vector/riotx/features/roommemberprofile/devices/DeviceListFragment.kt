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
package im.vector.riotx.features.roommemberprofile.devices

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.DimensionConverter
import javax.inject.Inject

class DeviceListFragment @Inject constructor(
        val dimensionConverter: DimensionConverter,
        val epoxyController: DeviceListEpoxyController
) : VectorBaseFragment(), DeviceListEpoxyController.InteractionListener {

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list

    private val viewModel: DeviceListBottomSheetViewModel by parentFragmentViewModel(DeviceListBottomSheetViewModel::class)

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.setPadding(0, dimensionConverter.dpToPx(16), 0, dimensionConverter.dpToPx(16))
        recyclerView.configureWith(
                epoxyController,
                showDivider = false,
                hasFixedSize = false)
        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
        super.invalidate()
    }

    override fun onDeviceSelected(device: CryptoDeviceInfo) {
        viewModel.selectDevice(device)
    }
}
