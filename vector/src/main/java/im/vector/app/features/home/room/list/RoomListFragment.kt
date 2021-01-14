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

package im.vector.app.features.home.room.list

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.dialogs.withColoredButton
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomListBinding
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import im.vector.app.features.notifications.NotificationDrawerManager
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

class RoomListFragment @Inject constructor(
        private val roomController: RoomSummaryController,
        val roomListViewModelFactory: RoomListViewModel.Factory,
        private val notificationDrawerManager: NotificationDrawerManager,
        private val sharedViewPool: RecyclerView.RecycledViewPool
) : VectorBaseFragment<FragmentRoomListBinding>(),
        RoomSummaryController.Listener,
        OnBackPressed,
        NotifsFabMenuView.Listener {

    private var modelBuildListener: OnModelBuildFinishedListener? = null
    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private val roomListParams: RoomListParams by args()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()
    private lateinit var stateRestorer: LayoutManagerStateRestorer

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomListBinding {
        return FragmentRoomListBinding.inflate(inflater, container, false)
    }

    private var hasUnreadRooms = false

    override fun getMenuRes() = R.menu.room_list

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_home_mark_all_as_read -> {
                roomListViewModel.handle(RoomListAction.MarkAllRoomsRead)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_home_mark_all_as_read).isVisible = hasUnreadRooms
        super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCreateRoomButton()
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        roomListViewModel.observeViewEvents {
            when (it) {
                is RoomListViewEvents.Loading    -> showLoading(it.message)
                is RoomListViewEvents.Failure    -> showFailure(it.throwable)
                is RoomListViewEvents.SelectRoom -> handleSelectRoom(it)
                is RoomListViewEvents.Done       -> Unit
            }.exhaustive
        }

        views.createChatFabMenu.listener = this

        sharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    override fun onDestroyView() {
        roomController.removeModelBuildListener(modelBuildListener)
        modelBuildListener = null
        views.roomListView.cleanup()
        roomController.listener = null
        stateRestorer.clear()
        views.createChatFabMenu.listener = null
        super.onDestroyView()
    }

    private fun handleSelectRoom(event: RoomListViewEvents.SelectRoom) {
        navigator.openRoom(requireActivity(), event.roomSummary.roomId)
    }

    private fun setupCreateRoomButton() {
        when (roomListParams.displayMode) {
            RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.isVisible = true
            RoomListDisplayMode.PEOPLE        -> views.createChatRoomButton.isVisible = true
            RoomListDisplayMode.ROOMS         -> views.createGroupRoomButton.isVisible = true
            else                              -> Unit // No button in this mode
        }

        views.createChatRoomButton.debouncedClicks {
            createDirectChat()
        }
        views.createGroupRoomButton.debouncedClicks {
            openRoomDirectory()
        }

        // Hide FAB when list is scrolling
        views.roomListView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        views.createChatFabMenu.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE     -> {
                                views.createChatFabMenu.postDelayed(showFabRunnable, 250)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                when (roomListParams.displayMode) {
                                    RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.hide()
                                    RoomListDisplayMode.PEOPLE        -> views.createChatRoomButton.hide()
                                    RoomListDisplayMode.ROOMS         -> views.createGroupRoomButton.hide()
                                    else                              -> Unit
                                }
                            }
                        }
                    }
                })
    }

    fun filterRoomsWith(filter: String) {
        // Scroll the list to top
        views.roomListView.scrollToPosition(0)

        roomListViewModel.handle(RoomListAction.FilterWith(filter))
    }

    override fun openRoomDirectory(initialFilter: String) {
        navigator.openRoomDirectory(requireActivity(), initialFilter)
    }

    override fun createDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        views.roomListView.layoutManager = layoutManager
        views.roomListView.itemAnimator = RoomListAnimator()
        views.roomListView.setRecycledViewPool(sharedViewPool)
        layoutManager.recycleChildrenOnDetach = true
        roomController.listener = this
        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }
        roomController.addModelBuildListener(modelBuildListener)
        views.roomListView.adapter = roomController.adapter
        views.stateView.contentView = views.roomListView
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.show()
                RoomListDisplayMode.PEOPLE        -> views.createChatRoomButton.show()
                RoomListDisplayMode.ROOMS         -> views.createGroupRoomButton.show()
                else                              -> Unit
            }
        }
    }

    private fun handleQuickActions(quickAction: RoomListQuickActionsSharedAction) {
        when (quickAction) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy     -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES_NOISY))
            }
            is RoomListQuickActionsSharedAction.NotificationsAll          -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES))
            }
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MENTIONS_ONLY))
            }
            is RoomListQuickActionsSharedAction.NotificationsMute         -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MUTE))
            }
            is RoomListQuickActionsSharedAction.Settings                  -> {
                navigator.openRoomProfile(requireActivity(), quickAction.roomId)
            }
            is RoomListQuickActionsSharedAction.Favorite                  -> {
                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_FAVOURITE))
            }
            is RoomListQuickActionsSharedAction.LowPriority               -> {
                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_LOW_PRIORITY))
            }
            is RoomListQuickActionsSharedAction.Leave                     -> {
                promptLeaveRoom(quickAction.roomId)
            }
        }.exhaustive
    }

    private fun promptLeaveRoom(roomId: String) {
        val isPublicRoom = roomListViewModel.isPublicRoom(roomId)
        val message = buildString {
            append(getString(R.string.room_participants_leave_prompt_msg))
            if (!isPublicRoom) {
                append("\n\n")
                append(getString(R.string.room_participants_leave_private_warning))
            }
        }
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.leave) { _, _ ->
                    roomListViewModel.handle(RoomListAction.LeaveRoom(roomId))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
                .apply {
                    if (!isPublicRoom) {
                        withColoredButton(DialogInterface.BUTTON_POSITIVE)
                    }
                }
    }

    override fun invalidate() = withState(roomListViewModel) { state ->
        when (state.asyncFilteredRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncFilteredRooms.error)
        }
        roomController.update(state)
        // Mark all as read menu
        when (roomListParams.displayMode) {
            RoomListDisplayMode.NOTIFICATIONS,
            RoomListDisplayMode.PEOPLE,
            RoomListDisplayMode.ROOMS -> {
                val newValue = state.hasUnread
                if (hasUnreadRooms != newValue) {
                    hasUnreadRooms = newValue
                    invalidateOptionsMenu()
                }
            }
            else                      -> Unit
        }
    }

    private fun renderSuccess(state: RoomListViewState) {
        val allRooms = state.asyncRooms()
        val filteredRooms = state.asyncFilteredRooms()
        if (filteredRooms.isNullOrEmpty()) {
            renderEmptyState(allRooms)
        } else {
            views.stateView.state = StateView.State.Content
        }
    }

    private fun renderEmptyState(allRooms: List<RoomSummary>?) {
        val hasNoRoom = allRooms
                ?.filter {
                    it.membership == Membership.JOIN || it.membership == Membership.INVITE
                }
                .isNullOrEmpty()
        val emptyState = when (roomListParams.displayMode) {
            RoomListDisplayMode.NOTIFICATIONS -> {
                if (hasNoRoom) {
                    StateView.State.Empty(
                            title = getString(R.string.room_list_catchup_welcome_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_catchup),
                            message = getString(R.string.room_list_catchup_welcome_body)
                    )
                } else {
                    StateView.State.Empty(
                            title = getString(R.string.room_list_catchup_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            message = getString(R.string.room_list_catchup_empty_body))
                }
            }
            RoomListDisplayMode.PEOPLE        ->
                StateView.State.Empty(
                        title = getString(R.string.room_list_people_empty_title),
                        image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_dm),
                        isBigImage = true,
                        message = getString(R.string.room_list_people_empty_body)
                )
            RoomListDisplayMode.ROOMS         ->
                StateView.State.Empty(
                        title = getString(R.string.room_list_rooms_empty_title),
                        image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_room),
                        isBigImage = true,
                        message = getString(R.string.room_list_rooms_empty_body)
                )
            else                              ->
                // Always display the content in this mode, because if the footer
                StateView.State.Content
        }
        views.stateView.state = emptyState
    }

    private fun renderLoading() {
        views.stateView.state = StateView.State.Loading
    }

    private fun renderFailure(error: Throwable) {
        val message = when (error) {
            is Failure.NetworkConnection -> getString(R.string.network_error_please_check_and_retry)
            else                         -> getString(R.string.unknown_error)
        }
        views.stateView.state = StateView.State.Error(message)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (views.createChatFabMenu.onBackPressed()) {
            return true
        }
        return false
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomClicked(room: RoomSummary) {
        roomListViewModel.handle(RoomListAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean {
        roomController.onRoomLongClicked()
        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId, RoomListActionsArgs.Mode.FULL)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.AcceptInvitation(room))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.RejectInvitation(room))
    }

    override fun onToggleRoomCategory(roomCategory: RoomCategory) {
        roomListViewModel.handle(RoomListAction.ToggleCategory(roomCategory))
    }

    override fun createRoom(initialName: String) {
        navigator.openCreateRoom(requireActivity(), initialName)
    }
}
