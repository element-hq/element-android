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

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.AppStateHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.databinding.FragmentRoomListBinding
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.settings.VectorPreferences
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

class RoomListFragment @Inject constructor(
        private val appStateHandler: AppStateHandler,
        private val pagedControllerFactory: RoomSummaryPagedControllerFactory,
        val roomListViewModelFactory: RoomListViewModel.Factory,
        private val notificationDrawerManager: NotificationDrawerManager,
        private val vectorPreferences: VectorPreferences,
        private val footerController: RoomListFooterController,
        private val userPreferencesProvider: UserPreferencesProvider
) : VectorBaseFragment<FragmentRoomListBinding>(),
        RoomListListener,
        OnBackPressed,
        AppStateHandler.OnSwitchSpaceListener,
        NotifsFabMenuView.Listener {

    private var modelBuildListener: OnModelBuildFinishedListener? = null
    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private val roomListParams: RoomListParams by args()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()
    private lateinit var stateRestorer: LayoutManagerStateRestorer

    private var expandStatusSpaceId: String? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomListBinding {
        return FragmentRoomListBinding.inflate(inflater, container, false)
    }

    data class SectionKey(
            val name: String,
            val isExpanded: Boolean,
            val notifyOfLocalEcho: Boolean
    )

    data class SectionAdapterInfo(
            var section: SectionKey,
            val sectionHeaderAdapter: SectionHeaderAdapter,
            val contentEpoxyController: EpoxyController
    )

    private val adapterInfosList = mutableListOf<SectionAdapterInfo>()
    private var concatAdapter: ConcatAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.stateView.contentView = views.roomListView
        views.stateView.state = StateView.State.Loading
        setupCreateRoomButton()
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        roomListViewModel.observeViewEvents {
            when (it) {
                is RoomListViewEvents.Loading                   -> showLoading(it.message)
                is RoomListViewEvents.Failure                   -> showFailure(it.throwable)
                is RoomListViewEvents.SelectRoom                -> handleSelectRoom(it)
                is RoomListViewEvents.Done                      -> Unit
                is RoomListViewEvents.NavigateToMxToBottomSheet -> handleShowMxToLink(it.link)
            }.exhaustive
        }

        views.createChatFabMenu.listener = this

        sharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()

        roomListViewModel.selectSubscribe(viewLifecycleOwner, RoomListViewState::roomMembershipChanges) { ms ->
            // it's for invites local echo
            adapterInfosList.filter { it.section.notifyOfLocalEcho }
                    .onEach {
                        (it.contentEpoxyController as? RoomSummaryPagedController)?.roomChangeMembershipStates = ms
                    }
        }
        appStateHandler.onSwitchSpaceListener = this
    }

    override fun onPause() {
        super.onPause()

        persistExpandStatus()
    }

    private fun refreshCollapseStates() {
        roomListViewModel.sections.forEachIndexed { index, roomsSection ->
            val actualBlock = adapterInfosList[index]
            val isRoomSectionExpanded = roomsSection.isExpanded.value.orTrue()
            if (actualBlock.section.isExpanded && !isRoomSectionExpanded) {
                // mark controller as collapsed
                actualBlock.contentEpoxyController.setCollapsed(true)
            } else if (!actualBlock.section.isExpanded && isRoomSectionExpanded) {
                // we must expand!
                actualBlock.contentEpoxyController.setCollapsed(false)
            }
            actualBlock.section = actualBlock.section.copy(
                    isExpanded = isRoomSectionExpanded
            )
            actualBlock.sectionHeaderAdapter.updateSection(
                    actualBlock.sectionHeaderAdapter.roomsSectionData.copy(isExpanded = isRoomSectionExpanded)
            )
        }
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    private fun handleShowMxToLink(link: String) {
        navigator.openMatrixToBottomSheet(requireContext(), link)
    }

    override fun onDestroyView() {
        adapterInfosList.onEach { it.contentEpoxyController.removeModelBuildListener(modelBuildListener) }
        adapterInfosList.clear()
        modelBuildListener = null
        views.roomListView.cleanup()
        footerController.listener = null
        // TODO Cleanup listener on the ConcatAdapter's adapters?
        stateRestorer.clear()
        views.createChatFabMenu.listener = null
        concatAdapter = null
        super.onDestroyView()
    }

    private fun handleSelectRoom(event: RoomListViewEvents.SelectRoom) {
        navigator.openRoom(requireActivity(), event.roomSummary.roomId)
    }

    private fun setupCreateRoomButton() {
        when (roomListParams.displayMode) {
            RoomListDisplayMode.ALL,
            RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.isVisible = true
            RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.isVisible = true
            RoomListDisplayMode.ROOMS -> views.createGroupRoomButton.isVisible = true
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
                                    RoomListDisplayMode.ALL,
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
        if (vectorPreferences.simplifiedMode()) {
            // Simplified mode: don't browse room directories, just create a room
            navigator.openCreateRoom(requireActivity())
        } else {
            navigator.openRoomDirectory(requireActivity(), initialFilter)
        }
    }

    override fun createDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        views.roomListView.layoutManager = layoutManager
        views.roomListView.itemAnimator = RoomListAnimator()
        layoutManager.recycleChildrenOnDetach = true

        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }

        val concatAdapter = ConcatAdapter()

        roomListViewModel.sections.forEach { section ->
            val sectionAdapter = SectionHeaderAdapter {
                roomListViewModel.handle(RoomListAction.ToggleSection(section))
            }.also {
                it.updateSection(SectionHeaderAdapter.RoomsSectionData(section.sectionName))
            }

            val contentAdapter =
                    when {
                        section.livePages != null     -> {
                            pagedControllerFactory.createRoomSummaryPagedController()
                                    .also { controller ->
                                        section.livePages.observe(viewLifecycleOwner) { pl ->
                                            controller.submitList(pl)
                                            sectionAdapter.updateSection(sectionAdapter.roomsSectionData.copy(
                                                    isHidden = pl.isEmpty(),
                                                    isLoading = false
                                            ))
                                            checkEmptyState()
                                        }
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection(sectionAdapter.roomsSectionData.copy(
                                                    notificationCount = counts.totalCount,
                                                    isHighlighted = counts.isHighlight,
                                                    unread = counts.unreadCount,
                                                    markedUnread = counts.markedUnread
                                            ))
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) { _ ->
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        section.liveSuggested != null -> {
                            pagedControllerFactory.createSuggestedRoomListController()
                                    .also { controller ->
                                        section.liveSuggested.observe(viewLifecycleOwner) { info ->
                                            controller.setData(info)
                                            sectionAdapter.updateSection(sectionAdapter.roomsSectionData.copy(
                                                    isHidden = info.rooms.isEmpty(),
                                                    isLoading = false
                                            ))
                                            checkEmptyState()
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) { _ ->
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        else                          -> {
                            pagedControllerFactory.createRoomSummaryListController()
                                    .also { controller ->
                                        section.liveList?.observe(viewLifecycleOwner) { list ->
                                            controller.setData(list)
                                            sectionAdapter.updateSection(sectionAdapter.roomsSectionData.copy(
                                                    isHidden = list.isEmpty(),
                                                    isLoading = false))
                                            checkEmptyState()
                                        }
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection(sectionAdapter.roomsSectionData.copy(
                                                    notificationCount = counts.totalCount,
                                                    isHighlighted = counts.isHighlight,
                                                    unread = counts.unreadCount,
                                                    markedUnread = counts.markedUnread
                                            ))
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) { _ ->
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                    }
            adapterInfosList.add(
                    SectionAdapterInfo(
                            SectionKey(
                                    name = section.sectionName,
                                    isExpanded = section.isExpanded.value.orTrue(),
                                    notifyOfLocalEcho = section.notifyOfLocalEcho
                            ),
                            sectionAdapter,
                            contentAdapter
                    )
            )
            concatAdapter.addAdapter(sectionAdapter)
            if (section.isExpanded.value.orTrue()) {
                concatAdapter.addAdapter(contentAdapter.adapter)
            }
        }

        // Add the footer controller
        footerController.listener = this
        concatAdapter.addAdapter(footerController.adapter)

        this.concatAdapter = concatAdapter
        views.roomListView.adapter = concatAdapter

        // Load initial expand statuses from settings
        loadExpandStatus()
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                RoomListDisplayMode.ALL,
                RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.show()
                RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.show()
                RoomListDisplayMode.ROOMS -> views.createGroupRoomButton.show()
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
            is RoomListQuickActionsSharedAction.MarkUnread                -> {
                roomListViewModel.handle(RoomListAction.SetMarkedUnread(quickAction.roomId, true))
            }
            is RoomListQuickActionsSharedAction.MarkRead                  -> {
                roomListViewModel.handle(RoomListAction.SetMarkedUnread(quickAction.roomId, false))
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
        MaterialAlertDialogBuilder(requireContext(), if (isPublicRoom) 0 else R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.leave) { _, _ ->
                    roomListViewModel.handle(RoomListAction.LeaveRoom(roomId))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun invalidate() = withState(roomListViewModel) { state ->
        footerController.setData(state)
    }

    private fun checkEmptyState() {
        val shouldShowEmpty = adapterInfosList.all { it.sectionHeaderAdapter.roomsSectionData.isHidden }
                && !adapterInfosList.any { it.sectionHeaderAdapter.roomsSectionData.isLoading }
        if (shouldShowEmpty) {
            val emptyState = when (roomListParams.displayMode) {
                RoomListDisplayMode.ALL ->
                    StateView.State.Empty(
                            title = getString(R.string.all_list_rooms_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_bottom_group),
                            message = getString(R.string.all_list_rooms_empty_body)
                    )
                RoomListDisplayMode.NOTIFICATIONS -> {
                    StateView.State.Empty(
                            title = getString(R.string.room_list_catchup_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            message = getString(R.string.room_list_catchup_empty_body))
                }
                RoomListDisplayMode.PEOPLE ->
                    StateView.State.Empty(
                            title = getString(R.string.room_list_people_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_dm),
                            isBigImage = true,
                            message = getString(R.string.room_list_people_empty_body)
                    )
                RoomListDisplayMode.ROOMS ->
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
        } else {
            // is there something to show already?
            if (adapterInfosList.any { !it.sectionHeaderAdapter.roomsSectionData.isHidden }) {
                views.stateView.state = StateView.State.Content
            } else {
                views.stateView.state = StateView.State.Loading
            }
        }
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
        userPreferencesProvider.neverShowLongClickOnRoomHelpAgain()
        withState(roomListViewModel) {
            // refresh footer
            footerController.setData(it)
        }
        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId, RoomListActionsArgs.Mode.FULL)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.AcceptInvitation(room))
    }

    override fun onJoinSuggestedRoom(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.JoinSuggestedRoom(room.childRoomId, room.viaServers))
    }

    override fun onSuggestedRoomClicked(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.ShowRoomDetails(room.childRoomId, room.viaServers))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(room.roomId)
        roomListViewModel.handle(RoomListAction.RejectInvitation(room))
    }

    override fun createRoom(initialName: String) {
        navigator.openCreateRoom(requireActivity(), initialName)
    }


    // SC addition: remember expanded sections across restarts
    companion object {
        const val ROOM_LIST_ROOM_EXPANDED_ANY_PREFIX = "ROOM_LIST_ROOM_EXPANDED"
    }

    override fun onSwitchSpace(spaceId: String?) {
        if (spaceId != expandStatusSpaceId) {
            persistExpandStatus()
            expandStatusSpaceId = spaceId
            loadExpandStatus()
        }
    }

    private fun persistExpandStatus() {
        val spEdit = getSharedPreferences().edit()
        roomListViewModel.sections.forEach{section ->
            val isExpanded = section.isExpanded.value
            if (isExpanded != null) {
                val pref = getRoomListExpandedPref(section)
                spEdit.putBoolean(pref, isExpanded)
            }
        }
        spEdit.apply()
    }

    private fun loadExpandStatus() {
        roomListViewModel.sections.forEach { section ->
            roomListViewModel.handle(RoomListAction.SetSectionExpanded(section, shouldInitiallyExpand(section)))
        }
    }

    private fun shouldInitiallyExpand(section: RoomsSection): Boolean {
        val sp = getSharedPreferences()
        val pref = getRoomListExpandedPref(section)
        return sp.getBoolean(pref, true)
    }

    private fun getSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    private fun getRoomListExpandedPref(section: RoomsSection): String {
        return "${ROOM_LIST_ROOM_EXPANDED_ANY_PREFIX}_${section.sectionName}_${roomListParams.displayMode}_${expandStatusSpaceId}"
    }
}
