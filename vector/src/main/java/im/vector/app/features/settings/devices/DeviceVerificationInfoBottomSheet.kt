/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.devices

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListWithTitleBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class DeviceVerificationInfoArgs(
        val userId: String,
        val deviceId: String
) : Parcelable

@AndroidEntryPoint
class DeviceVerificationInfoBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListWithTitleBinding>(),
        DeviceVerificationInfoBottomSheetController.Callback {

    private val viewModel: DeviceVerificationInfoBottomSheetViewModel by fragmentViewModel(DeviceVerificationInfoBottomSheetViewModel::class)

    private val sharedViewModel: DevicesViewModel by parentFragmentViewModel(DevicesViewModel::class)

    @Inject lateinit var controller: DeviceVerificationInfoBottomSheetController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListWithTitleBinding {
        return BottomSheetGenericListWithTitleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.bottomSheetRecyclerView.configureWith(
                controller,
                hasFixedSize = false
        )
        controller.callback = this
        views.bottomSheetTitle.isVisible = false
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(userId: String, deviceId: String): DeviceVerificationInfoBottomSheet {
            return DeviceVerificationInfoBottomSheet().apply {
                setArguments(DeviceVerificationInfoArgs(userId, deviceId))
            }
        }
    }

    override fun onAction(action: DevicesAction) {
        dismiss()
        sharedViewModel.handle(action)
    }
}
