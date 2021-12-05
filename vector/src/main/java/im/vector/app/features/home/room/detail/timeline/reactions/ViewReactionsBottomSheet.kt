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

package im.vector.app.features.home.room.detail.timeline.reactions

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
import im.vector.app.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.action.TimelineEventFragmentArgs
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

/**
 * Bottom sheet displaying list of reactions for a given event ordered by timestamp
 */
@AndroidEntryPoint
class ViewReactionsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListWithTitleBinding>(),
        ViewReactionsEpoxyController.Listener {

    private val viewModel: ViewReactionsViewModel by fragmentViewModel(ViewReactionsViewModel::class)

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel

    @Inject lateinit var epoxyController: ViewReactionsEpoxyController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListWithTitleBinding {
        return BottomSheetGenericListWithTitleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(
                epoxyController,
                hasFixedSize = false,
                dividerDrawable = R.drawable.divider_horizontal_on_secondary
        )
        views.bottomSheetTitle.text = context?.getString(R.string.reactions)
        epoxyController.listener = this
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun didSelectUser(userId: String) {
        sharedActionViewModel.post(EventSharedAction.OpenUserProfile(userId))
    }

    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): ViewReactionsBottomSheet {
            return ViewReactionsBottomSheet().apply {
                setArguments(TimelineEventFragmentArgs(
                        eventId = informationData.eventId,
                        roomId = roomId,
                        informationData = informationData
                ))
            }
        }
    }
}
