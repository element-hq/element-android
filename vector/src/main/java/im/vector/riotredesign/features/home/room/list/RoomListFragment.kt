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

package im.vector.riotredesign.features.home.room.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.extensions.setupAsSearch
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.home.HomeNavigator
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope

class RoomListFragment : VectorBaseFragment(), RoomSummaryController.Callback {

    companion object {
        fun newInstance(): RoomListFragment {
            return RoomListFragment()
        }
    }

    private val roomController by inject<RoomSummaryController>()
    private val homeNavigator by inject<HomeNavigator>()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_list

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(HomeModule.ROOM_LIST_SCOPE))
        setupRecyclerView()
        setupFilterView()
        roomListViewModel.subscribe { renderState(it) }
        roomListViewModel.openRoomLiveData.observeEvent(this) {
            homeNavigator.openRoomDetail(it, null)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        epoxyRecyclerView.layoutManager = layoutManager
        roomController.callback = this
        roomController.addModelBuildListener { it.dispatchTo(stateRestorer) }
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
    }

    private fun setupFilterView() {
        filterRoomView.setupAsSearch()
        filterRoomView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                roomListViewModel.accept(RoomListActions.FilterRooms(s))
            }
        })
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
            is Failure.NetworkConnection -> getString(R.string.network_error_please_check_and_retry)
            else                         -> getString(R.string.unknown_error)
        }
        stateView.state = StateView.State.Error(message)
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomSelected(room: RoomSummary) {
        roomListViewModel.accept(RoomListActions.SelectRoom(room))
    }

    override fun onToggleRoomCategory(roomCategory: RoomCategory) {
        roomListViewModel.accept(RoomListActions.ToggleCategory(roomCategory))
    }
}