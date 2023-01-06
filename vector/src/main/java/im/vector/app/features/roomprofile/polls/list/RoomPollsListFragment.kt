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

package im.vector.app.features.roomprofile.polls.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomPollsListBinding
import im.vector.app.features.roomprofile.polls.PollSummary
import im.vector.app.features.roomprofile.polls.RoomPollsType
import im.vector.app.features.roomprofile.polls.RoomPollsViewModel
import timber.log.Timber
import javax.inject.Inject

abstract class RoomPollsListFragment :
        VectorBaseFragment<FragmentRoomPollsListBinding>(),
        RoomPollsController.Listener {

    @Inject
    lateinit var roomPollsController: RoomPollsController

    private val viewModel: RoomPollsViewModel by parentFragmentViewModel(RoomPollsViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPollsListBinding {
        return FragmentRoomPollsListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
    }

    abstract fun getEmptyListTitle(): String

    abstract fun getRoomPollsType(): RoomPollsType

    private fun setupList() {
        roomPollsController.listener = this
        views.roomPollsList.configureWith(roomPollsController)
        views.roomPollsEmptyTitle.text = getEmptyListTitle()
    }

    override fun onDestroyView() {
        cleanUpList()
        super.onDestroyView()
    }

    private fun cleanUpList() {
        views.roomPollsList.cleanup()
        roomPollsController.listener = null
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        when (getRoomPollsType()) {
            RoomPollsType.ACTIVE -> renderList(viewState.polls.filterIsInstance(PollSummary.ActivePoll::class.java))
            RoomPollsType.ENDED -> renderList(viewState.polls.filterIsInstance(PollSummary.EndedPoll::class.java))
        }
    }

    private fun renderList(polls: List<PollSummary>) {
        roomPollsController.setData(polls)
        views.roomPollsEmptyTitle.isVisible = polls.isEmpty()
    }

    override fun onPollClicked(pollId: String) {
        // TODO navigate to details
        Timber.d("poll with id $pollId clicked")
    }
}
