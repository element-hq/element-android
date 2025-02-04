/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.historyvisibility

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.ui.bottomsheet.BottomSheetGeneric
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

@Parcelize
data class RoomHistoryVisibilityBottomSheetArgs(
        val currentRoomHistoryVisibility: RoomHistoryVisibility
) : Parcelable

@AndroidEntryPoint
class RoomHistoryVisibilityBottomSheet : BottomSheetGeneric<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction>() {

    private lateinit var roomHistoryVisibilitySharedActionViewModel: RoomHistoryVisibilitySharedActionViewModel
    @Inject lateinit var controller: RoomHistoryVisibilityController
    private val viewModel: RoomHistoryVisibilityViewModel by fragmentViewModel(RoomHistoryVisibilityViewModel::class)

    override fun getController(): BottomSheetGenericController<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction> = controller

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomHistoryVisibilitySharedActionViewModel = activityViewModelProvider.get(RoomHistoryVisibilitySharedActionViewModel::class.java)
    }

    override fun didSelectAction(action: RoomHistoryVisibilityRadioAction) {
        roomHistoryVisibilitySharedActionViewModel.post(action)
        dismiss()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(currentRoomHistoryVisibility: RoomHistoryVisibility): RoomHistoryVisibilityBottomSheet {
            return RoomHistoryVisibilityBottomSheet().apply {
                setArguments(RoomHistoryVisibilityBottomSheetArgs(currentRoomHistoryVisibility))
            }
        }
    }
}
