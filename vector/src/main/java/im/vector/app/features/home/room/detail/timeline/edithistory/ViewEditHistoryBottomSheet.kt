/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.edithistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListWithTitleBinding
import im.vector.app.features.home.room.detail.timeline.action.TimelineEventFragmentArgs
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

/**
 * Bottom sheet displaying list of edits for a given event ordered by timestamp.
 */
@AndroidEntryPoint
class ViewEditHistoryBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListWithTitleBinding>() {

    private val viewModel: ViewEditHistoryViewModel by fragmentViewModel(ViewEditHistoryViewModel::class)

    @Inject lateinit var epoxyController: ViewEditHistoryEpoxyController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListWithTitleBinding {
        return BottomSheetGenericListWithTitleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.bottomSheetRecyclerView.configureWith(
                epoxyController,
                dividerDrawable = R.drawable.divider_horizontal_on_secondary,
                hasFixedSize = false
        )
        views.bottomSheetTitle.text = context?.getString(CommonStrings.message_edits)
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): ViewEditHistoryBottomSheet {
            return ViewEditHistoryBottomSheet().apply {
                setArguments(
                        TimelineEventFragmentArgs(
                                eventId = informationData.eventId,
                                roomId = roomId,
                                informationData = informationData
                        )
                )
            }
        }
    }
}
