/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

/**
 * Bottom sheet fragment that shows a message preview with list of contextual actions.
 */
@AndroidEntryPoint
class MessageActionsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>(),
        MessageActionsEpoxyController.MessageActionsEpoxyControllerListener {

    @Inject lateinit var messageActionsEpoxyController: MessageActionsEpoxyController

    private val viewModel: MessageActionsViewModel by fragmentViewModel(MessageActionsViewModel::class)

    override val showExpanded = true

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(messageActionsEpoxyController, hasFixedSize = false, disableItemAnimation = true)
        messageActionsEpoxyController.listener = this
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onUrlClicked(url: String, title: String): Boolean {
        sharedActionViewModel.post(EventSharedAction.OnUrlClicked(url, title))
        // Always consume
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        sharedActionViewModel.post(EventSharedAction.OnUrlLongClicked(url))
        // Always consume
        return true
    }

    override fun didSelectMenuAction(eventAction: EventSharedAction) {
        if (eventAction is EventSharedAction.ReportContent) {
            // Toggle report menu
            // Enable item animation
            if (views.bottomSheetRecyclerView.itemAnimator == null) {
                views.bottomSheetRecyclerView.itemAnimator = MessageActionsAnimator()
            }
            viewModel.handle(MessageActionsAction.ToggleReportMenu)
        } else {
            sharedActionViewModel.post(eventAction)
            dismiss()
        }
    }

    override fun invalidate() = withState(viewModel) {
        messageActionsEpoxyController.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData, isFromThreadTimeline: Boolean): MessageActionsBottomSheet {
            return MessageActionsBottomSheet().apply {
                setArguments(
                        TimelineEventFragmentArgs(
                                informationData.eventId,
                                roomId,
                                informationData,
                                isFromThreadTimeline
                        )
                )
            }
        }
    }
}
