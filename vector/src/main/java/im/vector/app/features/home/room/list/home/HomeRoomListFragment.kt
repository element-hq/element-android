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

package im.vector.app.features.home.room.list.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyControllerAdapter
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.databinding.FragmentRoomListBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.room.list.RoomListAnimator
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.home.room.list.actions.RoomListSharedAction
import im.vector.app.features.home.room.list.actions.RoomListSharedActionViewModel
import im.vector.app.features.home.room.list.home.filter.HomeFilteredRoomsController
import im.vector.app.features.home.room.list.home.filter.HomeRoomFilter
import im.vector.app.features.home.room.list.home.recent.RecentRoomCarouselController
import im.vector.app.features.spaces.SpaceListBottomSheet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

class HomeRoomListFragment @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val recentRoomCarouselController: RecentRoomCarouselController
) : VectorBaseFragment<FragmentRoomListBinding>(),
        RoomListListener {

    private val roomListViewModel: HomeRoomListViewModel by fragmentViewModel()
    private lateinit var sharedQuickActionsViewModel: RoomListQuickActionsSharedActionViewModel
    private lateinit var sharedActionViewModel: RoomListSharedActionViewModel
    private var concatAdapter = ConcatAdapter()
    private var modelBuildListener: OnModelBuildFinishedListener? = null

    private val spaceListBottomSheet = SpaceListBottomSheet()

    private lateinit var stateRestorer: LayoutManagerStateRestorer

    private val newChatBottomSheet = NewChatBottomSheet()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomListBinding {
        return FragmentRoomListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.stateView.contentView = views.roomListView
        views.stateView.state = StateView.State.Loading
        setupObservers()
        setupRecyclerView()
        setupFabs()
    }

    private fun setupObservers() {
        sharedQuickActionsViewModel = activityViewModelProvider[RoomListQuickActionsSharedActionViewModel::class.java]
        sharedActionViewModel = activityViewModelProvider[RoomListSharedActionViewModel::class.java]

        sharedActionViewModel
                .stream()
                .onEach(::handleSharedAction)
                .launchIn(viewLifecycleOwner.lifecycleScope)
        sharedQuickActionsViewModel
                .stream()
                .onEach(::handleQuickActions)
                .launchIn(viewLifecycleOwner.lifecycleScope)

        roomListViewModel.observeViewEvents {
            when (it) {
                is HomeRoomListViewEvents.Loading -> showLoading(it.message)
                is HomeRoomListViewEvents.Failure -> showFailure(it.throwable)
                is HomeRoomListViewEvents.SelectRoom -> handleSelectRoom(it, it.isInviteAlreadyAccepted)
                is HomeRoomListViewEvents.Done -> Unit
            }
        }
    }

    private fun handleSharedAction(action: RoomListSharedAction) {
        when (action) {
            RoomListSharedAction.CloseBottomSheet -> spaceListBottomSheet.dismiss()
        }
    }

    private fun handleQuickActions(quickAction: RoomListQuickActionsSharedAction) {
        when (quickAction) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy -> {
                roomListViewModel.handle(HomeRoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES_NOISY))
            }
            is RoomListQuickActionsSharedAction.NotificationsAll -> {
                roomListViewModel.handle(HomeRoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES))
            }
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
                roomListViewModel.handle(HomeRoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MENTIONS_ONLY))
            }
            is RoomListQuickActionsSharedAction.NotificationsMute -> {
                roomListViewModel.handle(HomeRoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MUTE))
            }
            is RoomListQuickActionsSharedAction.Settings -> {
                navigator.openRoomProfile(requireActivity(), quickAction.roomId)
            }
            is RoomListQuickActionsSharedAction.Favorite -> {
                roomListViewModel.handle(HomeRoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_FAVOURITE))
            }
            is RoomListQuickActionsSharedAction.LowPriority -> {
                roomListViewModel.handle(HomeRoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_LOW_PRIORITY))
            }
            is RoomListQuickActionsSharedAction.Leave -> {
                roomListViewModel.handle(HomeRoomListAction.LeaveRoom(quickAction.roomId))
                promptLeaveRoom(quickAction.roomId)
            }
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        views.roomListView.layoutManager = layoutManager
        views.roomListView.itemAnimator = RoomListAnimator()
        layoutManager.recycleChildrenOnDetach = true

        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }

        roomListViewModel.sections.onEach { sections ->
            setUpAdapters(sections)
        }.launchIn(lifecycleScope)

        views.roomListView.adapter = concatAdapter

        // we need to force scroll when recents/filter tabs are added to make them visible
        concatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    layoutManager.scrollToPosition(0)
                }
            }
        })
    }

    private fun setupFabs() {
        showFABs()

        views.newLayoutCreateChatButton.setOnClickListener {
            newChatBottomSheet.show(requireActivity().supportFragmentManager, NewChatBottomSheet.TAG)
        }

        views.newLayoutOpenSpacesButton.setOnClickListener {
            // Click action for open spaces modal goes here
            spaceListBottomSheet.show(requireActivity().supportFragmentManager, SpaceListBottomSheet.TAG)
        }

        // Hide FABs when list is scrolling
        views.roomListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                views.createChatFabMenu.handler.removeCallbacksAndMessages(null)

                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> views.createChatFabMenu.postDelayed(::showFABs, 250)
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> hideFABs()
                }
            }
        })
    }

    private fun showFABs() {
        views.newLayoutCreateChatButton.show()
        views.newLayoutOpenSpacesButton.show()
    }

    private fun hideFABs() {
        views.newLayoutCreateChatButton.hide()
        views.newLayoutOpenSpacesButton.hide()
    }

    override fun invalidate() = withState(roomListViewModel) { state ->
        views.stateView.state = state.state
    }

    private fun setUpAdapters(sections: Set<HomeRoomSection>) {
        concatAdapter.adapters.forEach {
            concatAdapter.removeAdapter(it)
        }
        sections.forEach {
            concatAdapter.addAdapter(getAdapterForData(it))
        }
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
        MaterialAlertDialogBuilder(requireContext(), if (isPublicRoom) 0 else R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_leave) { _, _ ->
                    roomListViewModel.handle(HomeRoomListAction.LeaveRoom(roomId))
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    private fun getAdapterForData(section: HomeRoomSection): EpoxyControllerAdapter {
        return when (section) {
            is HomeRoomSection.RoomSummaryData -> {
                HomeFilteredRoomsController(
                        roomSummaryItemFactory,
                ).also { controller ->
                    controller.listener = this
                    controller.onFilterChanged = ::onRoomFilterChanged
                    section.filtersData.onEach {
                        controller.submitFiltersData(it.getOrNull())
                    }.launchIn(lifecycleScope)
                    section.list.observe(viewLifecycleOwner) { list ->
                        controller.submitList(list)
                    }
                }.adapter
            }
            is HomeRoomSection.RecentRoomsData -> recentRoomCarouselController.also { controller ->
                controller.listener = this
                section.list.observe(viewLifecycleOwner) { list ->
                    controller.submitList(list)
                }
            }.adapter
        }
    }

    private fun onRoomFilterChanged(filter: HomeRoomFilter) {
        roomListViewModel.handle(HomeRoomListAction.ChangeRoomFilter(filter))
    }

    private fun handleSelectRoom(event: HomeRoomListViewEvents.SelectRoom, isInviteAlreadyAccepted: Boolean) {
        navigator.openRoom(
                context = requireActivity(),
                roomId = event.roomSummary.roomId,
                isInviteAlreadyAccepted = isInviteAlreadyAccepted,
                trigger = ViewRoom.Trigger.RoomList
        )
    }

    override fun onDestroyView() {
        views.roomListView.cleanup()
        recentRoomCarouselController.listener = null
        super.onDestroyView()
    }

    // region RoomListListener

    override fun onRoomClicked(room: RoomSummary) {
        roomListViewModel.handle(HomeRoomListAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean {
        userPreferencesProvider.neverShowLongClickOnRoomHelpAgain()
        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        TODO("Not yet implemented")
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        TODO("Not yet implemented")
    }

    override fun onJoinSuggestedRoom(room: SpaceChildInfo) {
        TODO("Not yet implemented")
    }

    override fun onSuggestedRoomClicked(room: SpaceChildInfo) {
        TODO("Not yet implemented")
    }

    // endregion
}
