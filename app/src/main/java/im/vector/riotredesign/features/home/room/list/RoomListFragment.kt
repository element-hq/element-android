/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.room.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.features.home.HomeNavigator
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject

class RoomListFragment : RiotFragment(), RoomSummaryController.Callback {

    companion object {
        fun newInstance(): RoomListFragment {
            return RoomListFragment()
        }
    }

    private val homeNavigator by inject<HomeNavigator>()
    private val homeViewModel: RoomListViewModel by activityViewModel()
    private lateinit var roomController: RoomSummaryController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        roomController = RoomSummaryController(this)
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
        homeViewModel.subscribe { renderState(it) }
    }

    private fun renderState(state: RoomListViewState) {
        when (state.asyncRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncRooms.error)
        }
    }

    private fun renderSuccess(state: RoomListViewState) {
        if (state.asyncRooms().isNullOrEmpty()) {
            stateView.state = StateView.State.Empty(getString(R.string.room_list_empty))
        } else {
            stateView.state = StateView.State.Content
        }
        roomController.setData(state)
    }

    private fun renderLoading() {
        stateView.state = StateView.State.Loading
    }

    private fun renderFailure(error: Throwable) {
        val message = when (error) {
            is Failure.NetworkConnection -> getString(R.string.error_no_network)
            else                         -> getString(R.string.error_common)
        }
        stateView.state = StateView.State.Error(message)
    }

    override fun onRoomSelected(room: RoomSummary) {
        homeViewModel.accept(RoomListActions.SelectRoom(room))
        homeNavigator.openRoomDetail(room.roomId, null)
    }

}