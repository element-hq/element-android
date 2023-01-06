/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.active

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomPollsListBinding
import im.vector.app.features.roomprofile.polls.PollSummary
import im.vector.app.features.roomprofile.polls.RoomPollsAction
import im.vector.app.features.roomprofile.polls.RoomPollsFilterType
import im.vector.app.features.roomprofile.polls.RoomPollsViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RoomActivePollsFragment :
        VectorBaseFragment<FragmentRoomPollsListBinding>(),
        RoomActivePollsController.Listener {

    @Inject
    lateinit var roomActivePollsController: RoomActivePollsController

    private val viewModel: RoomPollsViewModel by parentFragmentViewModel(RoomPollsViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPollsListBinding {
        return FragmentRoomPollsListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
    }

    private fun setupList() {
        roomActivePollsController.listener = this
        views.roomPollsList.configureWith(roomActivePollsController)
        views.roomPollsEmptyTitle.text = getString(R.string.room_polls_active_no_item)
    }

    override fun onDestroyView() {
        cleanUpList()
        super.onDestroyView()
    }

    private fun cleanUpList() {
        views.roomPollsList.cleanup()
        roomActivePollsController.listener = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.handle(RoomPollsAction.SetFilter(RoomPollsFilterType.ACTIVE))
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        renderList(viewState.polls.filterIsInstance(PollSummary.ActivePoll::class.java))
    }

    private fun renderList(polls: List<PollSummary.ActivePoll>) {
        roomActivePollsController.setData(polls)
        views.roomPollsEmptyTitle.isVisible = polls.isEmpty()
    }

    override fun onPollClicked(pollId: String) {
        // TODO navigate to details
        Timber.d("poll with id $pollId clicked")
    }
}
