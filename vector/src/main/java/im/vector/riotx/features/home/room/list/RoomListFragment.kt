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

package im.vector.riotx.features.home.room.list

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.*
import com.google.android.material.snackbar.Snackbar
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.extensions.observeEventFirstThrottle
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.StateView
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.room.list.widget.FabMenuView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_list.*
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListFragment.DisplayMode
) : Parcelable


class RoomListFragment : VectorBaseFragment(), RoomSummaryController.Listener, OnBackPressed, FabMenuView.Listener {

    enum class DisplayMode(@StringRes val titleRes: Int) {
        HOME(R.string.bottom_action_home),
        PEOPLE(R.string.bottom_action_people_x),
        ROOMS(R.string.bottom_action_rooms),
        FILTERED(/* Not used */ R.string.bottom_action_rooms)
    }

    companion object {
        fun newInstance(roomListParams: RoomListParams): RoomListFragment {
            return RoomListFragment().apply {
                setArguments(roomListParams)
            }
        }
    }

    private val roomListParams: RoomListParams by args()
    @Inject lateinit var roomController: RoomSummaryController
    @Inject lateinit var roomListViewModelFactory: RoomListViewModel.Factory
    @Inject lateinit var errorFormatter: ErrorFormatter
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_list

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupCreateRoomButton()
        setupRecyclerView()
        roomListViewModel.subscribe { renderState(it) }
        roomListViewModel.openRoomLiveData.observeEventFirstThrottle(this, 800L) {
            navigator.openRoom(requireActivity(), it)
        }

        createChatFabMenu.listener = this

        roomListViewModel.invitationAnswerErrorLiveData.observeEvent(this) { throwable ->
            vectorBaseActivity.coordinatorLayout?.let {
                Snackbar.make(it, errorFormatter.toHumanReadable(throwable), Snackbar.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun setupCreateRoomButton() {
        when (roomListParams.displayMode) {
            DisplayMode.HOME     -> createChatFabMenu.isVisible = true
            DisplayMode.PEOPLE   -> createChatRoomButton.isVisible = true
            DisplayMode.ROOMS    -> createGroupRoomButton.isVisible = true
            DisplayMode.FILTERED -> Unit // No button in this mode
        }

        createChatRoomButton.setOnClickListener {
            createDirectChat()
        }
        createGroupRoomButton.setOnClickListener {
            openRoomDirectory()
        }

        // Hide FAB when list is scrolling
        epoxyRecyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        createChatFabMenu.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE     -> {
                                createChatFabMenu.postDelayed(showFabRunnable, 1000)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                when (roomListParams.displayMode) {
                                    DisplayMode.HOME     -> createChatFabMenu.hide()
                                    DisplayMode.PEOPLE   -> createChatRoomButton.hide()
                                    DisplayMode.ROOMS    -> createGroupRoomButton.hide()
                                    DisplayMode.FILTERED -> Unit
                                }
                            }
                        }
                    }
                })
    }

    fun filterRoomsWith(filter: String) {
        roomListViewModel.accept(RoomListActions.FilterWith(filter))
    }

    override fun openRoomDirectory(initialFilter: String) {
        navigator.openRoomDirectory(requireActivity(), initialFilter)
    }

    override fun createDirectChat() {
        vectorBaseActivity.notImplemented("creating direct chat")
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        epoxyRecyclerView.layoutManager = layoutManager
        epoxyRecyclerView.itemAnimator = RoomListAnimator()
        roomController.listener = this
        roomController.addModelBuildListener { it.dispatchTo(stateRestorer) }
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                DisplayMode.HOME     -> createChatFabMenu.show()
                DisplayMode.PEOPLE   -> createChatRoomButton.show()
                DisplayMode.ROOMS    -> createGroupRoomButton.show()
                DisplayMode.FILTERED -> Unit
            }
        }
    }

    private fun renderState(state: RoomListViewState) {
        when (state.asyncFilteredRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncFilteredRooms.error)
        }
        roomController.setData(state)
    }

    private fun renderSuccess(state: RoomListViewState) {
        val allRooms = state.asyncRooms()
        val filteredRooms = state.asyncFilteredRooms()
        if (filteredRooms.isNullOrEmpty()) {
            renderEmptyState(allRooms)
        } else {
            stateView.state = StateView.State.Content
        }
    }

    private fun renderEmptyState(allRooms: List<RoomSummary>?) {
        val hasNoRoom = allRooms
                ?.filter {
                    it.membership == Membership.JOIN || it.membership == Membership.INVITE
                }
                .isNullOrEmpty()
        val emptyState = when (roomListParams.displayMode) {
            DisplayMode.HOME     -> {
                if (hasNoRoom) {
                    StateView.State.Empty(
                            getString(R.string.room_list_catchup_welcome_title),
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_catchup),
                            getString(R.string.room_list_catchup_welcome_body)
                    )
                } else {
                    StateView.State.Empty(
                            getString(R.string.room_list_catchup_empty_title),
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            getString(R.string.room_list_catchup_empty_body))
                }
            }
            DisplayMode.PEOPLE   ->
                StateView.State.Empty(
                        getString(R.string.room_list_people_empty_title),
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_chat),
                        getString(R.string.room_list_people_empty_body)
                )
            DisplayMode.ROOMS    ->
                StateView.State.Empty(
                        getString(R.string.room_list_rooms_empty_title),
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_group),
                        getString(R.string.room_list_rooms_empty_body)
                )
            DisplayMode.FILTERED ->
                // Always display the content in this mode, because if the footer
                StateView.State.Content
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

    override fun onBackPressed(): Boolean {
        if (createChatFabMenu.onBackPressed()) {
            return true
        }

        return super.onBackPressed()
    }

// RoomSummaryController.Callback **************************************************************

    override fun onRoomSelected(room: RoomSummary) {
        roomListViewModel.accept(RoomListActions.SelectRoom(room))
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        roomListViewModel.accept(RoomListActions.AcceptInvitation(room))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        roomListViewModel.accept(RoomListActions.RejectInvitation(room))
    }

    override fun onToggleRoomCategory(roomCategory: RoomCategory) {
        roomListViewModel.accept(RoomListActions.ToggleCategory(roomCategory))
    }

    override fun createRoom(initialName: String) {
        navigator.openCreateRoom(requireActivity(), initialName)
    }

}