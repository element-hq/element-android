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
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.home.HomeNavigator
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope

@Parcelize
data class RoomListParams(
        val displayMode: RoomListFragment.DisplayMode
) : Parcelable


class RoomListFragment : VectorBaseFragment(), RoomSummaryController.Callback {

    enum class DisplayMode(@StringRes val titleRes: Int) {
        HOME(R.string.bottom_action_home),
        PEOPLE(R.string.bottom_action_people),
        ROOMS(R.string.bottom_action_rooms)
    }

    companion object {
        fun newInstance(roomListParams: RoomListParams): RoomListFragment {
            return RoomListFragment().apply {
                setArguments(roomListParams)
            }
        }
    }

    private val roomListParams: RoomListParams by args()
    private val roomController by inject<RoomSummaryController>()
    private val homeNavigator by inject<HomeNavigator>()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_list

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(HomeModule.ROOM_LIST_SCOPE))
        setupRecyclerView()
        setupCreateRoomButton()
        roomListViewModel.subscribe { renderState(it) }
        roomListViewModel.openRoomLiveData.observeEvent(this) {
            homeNavigator.openRoomDetail(it, null)
        }
    }

    private fun setupCreateRoomButton() {
        createRoomButton.setImageResource(R.drawable.ic_add_white)
        createRoomButton.setOnClickListener {
            vectorBaseActivity.notImplemented()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        epoxyRecyclerView.layoutManager = layoutManager
        epoxyRecyclerView.itemAnimator = RoomListAnimator()
        roomController.callback = this
        roomController.addModelBuildListener { it.dispatchTo(stateRestorer) }
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
    }

    private fun renderState(state: RoomListViewState) {
        when (state.asyncFilteredRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncFilteredRooms.error)
        }
    }

    private fun renderSuccess(state: RoomListViewState) {
        val allRooms = state.asyncRooms()
        val filteredRooms = state.asyncFilteredRooms()
        if (filteredRooms.isNullOrEmpty()) {
            renderEmptyState(allRooms)
        } else {
            stateView.state = StateView.State.Content
        }
        roomController.setData(state)
    }

    private fun renderEmptyState(allRooms: List<RoomSummary>?) {
        val hasNoRoom = allRooms.isNullOrEmpty()
        val emptyState = when (roomListParams.displayMode) {
            DisplayMode.HOME   -> {
                if (hasNoRoom) {
                    StateView.State.Empty(getString(R.string.room_list_catchup_welcome_title), null, getString(R.string.room_list_catchup_welcome_body))
                } else {
                    StateView.State.Empty(getString(R.string.room_list_catchup_empty_title), null, getString(R.string.room_list_catchup_empty_body))
                }
            }
            DisplayMode.PEOPLE -> StateView.State.Empty()
            DisplayMode.ROOMS  -> StateView.State.Empty()
        }
        stateView.state = emptyState
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