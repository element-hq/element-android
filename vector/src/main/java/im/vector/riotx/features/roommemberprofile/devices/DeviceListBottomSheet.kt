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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.core.utils.DimensionConverter
import kotlinx.android.synthetic.main.bottom_sheet_generic_list_with_title.*
import javax.inject.Inject

class DeviceListBottomSheet : VectorBaseBottomSheetDialogFragment(), DeviceListEpoxyController.InteractionListener {

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list_with_title

    private val viewModel: DeviceListBottomSheetViewModel by fragmentViewModel(DeviceListBottomSheetViewModel::class)

    @Inject lateinit var viewModelFactory: DeviceListBottomSheetViewModel.Factory

    @Inject lateinit var dimensionConverter: DimensionConverter

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    @Inject lateinit var epoxyController: DeviceListEpoxyController

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.setPadding(0, dimensionConverter.dpToPx(16 ),0,  dimensionConverter.dpToPx(16 ))
        recyclerView.configureWith(
                epoxyController,
                showDivider = false,
                hasFixedSize = false)
        epoxyController.interactionListener = this
        bottomSheetTitle.isVisible = false
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
        // TODO
    }

    companion object {
        fun newInstance(userId: String): DeviceListBottomSheet {
            val args = Bundle()
            args.putString(MvRx.KEY_ARG, userId)
            return DeviceListBottomSheet().apply { arguments = args }
        }
    }
}
