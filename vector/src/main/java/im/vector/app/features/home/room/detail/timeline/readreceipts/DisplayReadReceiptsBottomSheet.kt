/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.readreceipts

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListWithTitleBinding
import im.vector.app.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class DisplayReadReceiptArgs(
        val readReceipts: List<ReadReceiptData>
) : Parcelable

/**
 * Bottom sheet displaying list of read receipts for a given event ordered by descending timestamp.
 */
@AndroidEntryPoint
class DisplayReadReceiptsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListWithTitleBinding>(),
        DisplayReadReceiptsController.Listener {

    @Inject lateinit var epoxyController: DisplayReadReceiptsController

    private val displayReadReceiptArgs: DisplayReadReceiptArgs by args()

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListWithTitleBinding {
        return BottomSheetGenericListWithTitleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(epoxyController, hasFixedSize = false)
        views.bottomSheetTitle.text = getString(CommonStrings.seen_by)
        epoxyController.listener = this
        epoxyController.setData(displayReadReceiptArgs.readReceipts)
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun didSelectUser(userId: String) {
        sharedActionViewModel.post(EventSharedAction.OpenUserProfile(userId))
    }

    // we are not using state for this one as it's static, so no need to override invalidate()

    companion object {
        fun newInstance(readReceipts: List<ReadReceiptData>): DisplayReadReceiptsBottomSheet {
            return DisplayReadReceiptsBottomSheet().apply {
                setArguments(DisplayReadReceiptArgs(readReceipts = readReceipts))
            }
        }
    }
}
