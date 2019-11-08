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
package im.vector.riotx.features.home.room.detail.timeline.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
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

    private lateinit var messageActionsStore: MessageActionsDispatcher

    override fun injectWith(screenComponent: ScreenComponent) {
        screenComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_generic_list, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        messageActionsStore = ViewModelProviders.of(requireActivity()).get(MessageActionsDispatcher::class.java)
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recyclerView.adapter = messageActionsEpoxyController.adapter
        // Disable item animation
        recyclerView.itemAnimator = null
        messageActionsEpoxyController.listener = this
    }

    override fun didSelectMenuAction(eventAction: EventSharedAction) {
        if (eventAction is EventSharedAction.ReportContent) {
            // Toggle report menu
            viewModel.handle(MessageActionsAction.ToggleReportMenu)
        } else {
            messageActionsStore.post(eventAction)
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
