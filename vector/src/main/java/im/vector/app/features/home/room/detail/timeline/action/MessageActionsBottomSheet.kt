/*
 * Copyright 2019 New Vector Ltd
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
 */
package im.vector.app.features.home.room.detail.timeline.action

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

/**
 * Bottom sheet fragment that shows a message preview with list of contextual actions
 */
class MessageActionsBottomSheet : VectorBaseBottomSheetDialogFragment(), MessageActionsEpoxyController.MessageActionsEpoxyControllerListener {

    @Inject lateinit var messageActionViewModelFactory: MessageActionsViewModel.Factory
    @Inject lateinit var messageActionsEpoxyController: MessageActionsEpoxyController

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    private val viewModel: MessageActionsViewModel by fragmentViewModel(MessageActionsViewModel::class)

    override val showExpanded = true

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        recyclerView.configureWith(messageActionsEpoxyController, hasFixedSize = false, disableItemAnimation = true)
        messageActionsEpoxyController.listener = this
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
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
            if (recyclerView.itemAnimator == null) {
                recyclerView.itemAnimator = MessageActionsAnimator()
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
        fun newInstance(roomId: String, informationData: MessageInformationData): MessageActionsBottomSheet {
            return MessageActionsBottomSheet().apply {
                setArguments(
                        TimelineEventFragmentArgs(
                                informationData.eventId,
                                roomId,
                                informationData
                        )
                )
            }
        }
    }
}
