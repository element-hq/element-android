/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.app.features.settings.devices

import android.os.Bundle
import android.os.Parcelable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.bottom_sheet_generic_list_with_title.*
import javax.inject.Inject

@Parcelize
data class DeviceVerificationInfoArgs(
        val userId: String,
        val deviceId: String
) : Parcelable

class DeviceVerificationInfoBottomSheet : VectorBaseBottomSheetDialogFragment(), DeviceVerificationInfoBottomSheetController.Callback {

    private val viewModel: DeviceVerificationInfoBottomSheetViewModel by fragmentViewModel(DeviceVerificationInfoBottomSheetViewModel::class)

    private val sharedViewModel: DevicesViewModel by parentFragmentViewModel(DevicesViewModel::class)

    @Inject lateinit var deviceVerificationInfoViewModelFactory: DeviceVerificationInfoBottomSheetViewModel.Factory

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    @Inject lateinit var controller: DeviceVerificationInfoBottomSheetController

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list_with_title

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.configureWith(
                controller,
                showDivider = false,
                hasFixedSize = false)
        controller.callback = this
        bottomSheetTitle.isVisible = false
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(userId: String, deviceId: String): DeviceVerificationInfoBottomSheet {
            val args = Bundle()
            val parcelableArgs = DeviceVerificationInfoArgs(userId, deviceId)
            args.putParcelable(MvRx.KEY_ARG, parcelableArgs)
            return DeviceVerificationInfoBottomSheet().apply { arguments = args }
        }
    }

    override fun onAction(action: DevicesAction) {
        dismiss()
        sharedViewModel.handle(action)
    }
}
