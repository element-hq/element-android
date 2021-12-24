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

package im.vector.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.badge.BadgeDrawable
import de.spiritcroc.viewpager.reduceDragSensitivity
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.extensions.restart
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.ui.views.CurrentCallsView
import im.vector.app.core.ui.views.CurrentCallsViewPresenter
import im.vector.app.core.ui.views.KeysBackupBanner
import im.vector.app.databinding.FragmentHomeDetailBinding
import im.vector.app.features.call.SharedKnownCallsViewModel
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.dialpad.DialPadFragment
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.home.room.list.RoomListFragment
import im.vector.app.features.home.room.list.RoomListParams
import im.vector.app.features.home.room.list.RoomListSectionBuilderSpace.Companion.SPACE_ID_FOLLOW_APP
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.app.features.settings.VectorLocale
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity.Companion.EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.workers.signout.BannerState
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

class HomeDetailFragment @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val colorProvider: ColorProvider,
        private val alertManager: PopupAlertManager,
        private val callManager: WebRtcCallManager,
        private val vectorPreferences: VectorPreferences,
        private val appStateHandler: AppStateHandler
) : VectorBaseFragment<FragmentHomeDetailBinding>(),
        KeysBackupBanner.Delegate,
        CurrentCallsView.Callback {

    companion object {
        const val DEBUG_VIEW_PAGER = true
    }

    private val viewModel: HomeDetailViewModel by fragmentViewModel()
    private val unknownDeviceDetectorSharedViewModel: UnknownDeviceDetectorSharedViewModel by activityViewModel()
    private val unreadMessagesSharedViewModel: UnreadMessagesSharedViewModel by activityViewModel()
    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private lateinit var sharedCallActionViewModel: SharedKnownCallsViewModel

    // When this changes, restart the activity for changes to apply
    private val shouldShowUnimportantCounterBadge = vectorPreferences.shouldShowUnimportantCounterBadge()
    private val useAggregateCounts = vectorPreferences.aggregateUnreadRoomCounts()

    private var hasUnreadRooms = false
        set(value) {
            if (value != field) {
                field = value
                invalidateOptionsMenu()
            }
        }

    private var initialPageSelected = false
    private var pagerSpaces: List<String?>? = null
    private var pagerTab: HomeTab? = null
    private var pagerPagingEnabled: Boolean = false
    private val pendingSpaceIds = mutableListOf<String?>()

    override fun getMenuRes() = R.menu.room_list

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_home_mark_all_as_read -> {
                viewModel.handle(HomeDetailAction.MarkAllRoomsRead)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) { state ->
            val isRoomList = state.currentTab is HomeTab.RoomList
            menu.findItem(R.id.menu_home_mark_all_as_read).isVisible = isRoomList && hasUnreadRooms
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeDetailBinding {
        return FragmentHomeDetailBinding.inflate(inflater, container, false)
    }

    private val currentCallsViewPresenter = CurrentCallsViewPresenter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)
        sharedCallActionViewModel = activityViewModelProvider.get(SharedKnownCallsViewModel::class.java)
        setupBottomNavigationView()
        setupToolbar()
        setupKeysBackupBanner()
        setupActiveCallView()

        checkNotificationTabStatus()

        // Reduce sensitivity of viewpager to avoid scrolling horizontally by accident too easily
        views.roomListContainerPager.reduceDragSensitivity(4)

        // space pager: update appStateHandler's current page to update rest of the UI accordingly
        views.roomListContainerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (DEBUG_VIEW_PAGER) Timber.i("Home pager: selected page $position $initialPageSelected")
                super.onPageSelected(position)
                if (!initialPageSelected) {
                    // Do not apply before we have restored the previous value
                    if (position == 0) {
                        return
                    } else {
                        // User has swiped, store it anyways
                        initialPageSelected = true
                    }
                }
                val selectedId = getSpaceIdForPageIndex(position)
                pendingSpaceIds.add(selectedId)
                appStateHandler.setCurrentSpace(selectedId)
            }
        })

        viewModel.onEach(HomeDetailViewState::roomGroupingMethod) { roomGroupingMethod ->
            when (roomGroupingMethod) {
                is RoomGroupingMethod.ByLegacyGroup -> {
                    onGroupChange(roomGroupingMethod.groupSummary)
                }
                is RoomGroupingMethod.BySpace       -> {
                    onSpaceChange(roomGroupingMethod.spaceSummary)
                }
            }
        }

        viewModel.onEach(HomeDetailViewState::currentTab) { currentTab ->
            updateUIForTab(currentTab)
        }

        viewModel.onEach(HomeDetailViewState::showDialPadTab) { showDialPadTab ->
            updateTabVisibilitySafely(R.id.bottom_action_dial_pad, showDialPadTab)
            checkNotificationTabStatus(showDialPadTab)
        }

        viewModel.observeViewEvents { viewEvent ->
            when (viewEvent) {
                HomeDetailViewEvents.CallStarted   -> handleCallStarted()
                is HomeDetailViewEvents.FailToCall -> showFailure(viewEvent.failure)
                HomeDetailViewEvents.Loading       -> showLoadingDialog()
            }
        }

        unknownDeviceDetectorSharedViewModel.onEach { state ->
            state.unknownSessions.invoke()?.let { unknownDevices ->
//                Timber.v("## Detector Triggerred in fragment - ${unknownDevices.firstOrNull()}")
                if (unknownDevices.firstOrNull()?.currentSessionTrust == true) {
                    val uid = "review_login"
                    alertManager.cancelAlert(uid)
                    val olderUnverified = unknownDevices.filter { !it.isNew }
                    val newest = unknownDevices.firstOrNull { it.isNew }?.deviceInfo
                    if (newest != null) {
                        promptForNewUnknownDevices(uid, state, newest)
                    } else if (olderUnverified.isNotEmpty()) {
                        // In this case we prompt to go to settings to review logins
                        promptToReviewChanges(uid, state, olderUnverified.map { it.deviceInfo })
                    }
                }
            }
        }

        unreadMessagesSharedViewModel.onEach { state ->
            views.drawerUnreadCounterBadgeView.render(
                    UnreadCounterBadgeView.State(
                            count = state.otherSpacesUnread.totalCount,
                            highlighted = state.otherSpacesUnread.isHighlight,
                            unread = state.otherSpacesUnread.unreadCount,
                            markedUnread = false
                    )
            )
        }

        viewModel.onEach(HomeDetailViewState::roomGroupingMethod, HomeDetailViewState::rootSpacesOrdered, HomeDetailViewState::currentTab) { roomGroupingMethod, rootSpacesOrdered, currentTab ->
            setupViewPager(roomGroupingMethod, rootSpacesOrdered, currentTab)
        }

        sharedCallActionViewModel
                .liveKnownCalls
                .observe(viewLifecycleOwner) {
                    currentCallsViewPresenter.updateCall(callManager.getCurrentCall(), callManager.getCalls())
                    invalidateOptionsMenu()
                }
    }

    private fun handleCallStarted() {
        dismissLoadingDialog()
        val fragmentTag = HomeTab.DialPad.toFragmentTag()
        (childFragmentManager.findFragmentByTag(fragmentTag) as? DialPadFragment)?.clear()
    }

    override fun onDestroyView() {
        currentCallsViewPresenter.unBind()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        if (vectorPreferences.shouldShowUnimportantCounterBadge() != shouldShowUnimportantCounterBadge ||
                vectorPreferences.aggregateUnreadRoomCounts() != useAggregateCounts) {
            activity?.restart()
            return
        }

        // update notification tab if needed

        //updateTabVisibilitySafely(R.id.bottom_action_notification, vectorPreferences.labAddNotificationTab())
        checkNotificationTabStatus()
        callManager.checkForProtocolsSupportIfNeeded()

        // Current space/group is not live so at least refresh toolbar on resume
        appStateHandler.getCurrentRoomGroupingMethod()?.let { roomGroupingMethod ->
            when (roomGroupingMethod) {
                is RoomGroupingMethod.ByLegacyGroup -> {
                    onGroupChange(roomGroupingMethod.groupSummary)
                }
                is RoomGroupingMethod.BySpace       -> {
                    onSpaceChange(roomGroupingMethod.spaceSummary)
                }
            }
        }
    }

    private fun checkNotificationTabStatus(enableDialPad: Boolean? = null) {
        val wasVisible = views.bottomNavigationView.menu.findItem(R.id.bottom_action_notification).isVisible
        val combinedOverview = vectorPreferences.combinedOverview()
        val combinedOverviewWasVisible = views.bottomNavigationView.menu.findItem(R.id.bottom_action_all).isVisible
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_notification).isVisible = vectorPreferences.labAddNotificationTab()
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_people).isVisible = !combinedOverview
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_rooms).isVisible = !combinedOverview
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_all).isVisible = combinedOverview
        if (combinedOverviewWasVisible != combinedOverview) {
            // As we hide it check if it's not the current item!
            withState(viewModel) {
                val menuId = it.currentTab.toMenuId()
                if (combinedOverview) {
                    if (menuId == R.id.bottom_action_people || menuId == R.id.bottom_action_rooms) {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.ALL)))
                    }
                } else {
                    if (menuId == R.id.bottom_action_all) {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.PEOPLE)))
                    }
                }
            }
        }
        if (wasVisible && !vectorPreferences.labAddNotificationTab()) {
            // As we hide it check if it's not the current item!
            withState(viewModel) {
                if (it.currentTab.toMenuId() == R.id.bottom_action_notification) {
                    if (combinedOverview) {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.ALL)))
                    } else {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.PEOPLE)))
                    }
                }
            }
        }
        val wasBottomBarVisible = views.bottomNavigationView.isVisible
        val showDialPad = enableDialPad ?: withState(viewModel) { it.showDialPadTab }
        val showTabBar = vectorPreferences.enableOverviewTabs() || showDialPad
        if (wasBottomBarVisible != showTabBar) {
            withState(viewModel) {
                // Update the navigation view if needed (for when we restore the tabs)
                if (showTabBar) {
                    views.bottomNavigationView.selectedItemId = it.currentTab.toMenuId()
                    views.bottomNavigationView.isVisible = true
                } else {
                    views.bottomNavigationView.isVisible = false
                }

            }
        }
    }

    private fun promptForNewUnknownDevices(uid: String, state: UnknownDevicesState, newest: DeviceInfo) {
        val user = state.myMatrixItem
        alertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = uid,
                        title = getString(R.string.new_session),
                        description = getString(R.string.verify_this_session, newest.displayName ?: newest.deviceId ?: ""),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer)
                    colorInt = colorProvider.getColorFromAttribute(R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)
                                ?.navigator
                                ?.requestSessionVerification(requireContext(), newest.deviceId ?: "")
                        unknownDeviceDetectorSharedViewModel.handle(
                                UnknownDeviceDetectorSharedViewModel.Action.IgnoreDevice(newest.deviceId?.let { listOf(it) }.orEmpty())
                        )
                    }
                    dismissedAction = Runnable {
                        unknownDeviceDetectorSharedViewModel.handle(
                                UnknownDeviceDetectorSharedViewModel.Action.IgnoreDevice(newest.deviceId?.let { listOf(it) }.orEmpty())
                        )
                    }
                }
        )
    }

    private fun promptToReviewChanges(uid: String, state: UnknownDevicesState, oldUnverified: List<DeviceInfo>) {
        val user = state.myMatrixItem
        alertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = uid,
                        title = getString(R.string.review_logins),
                        description = getString(R.string.verify_other_sessions),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer)
                    colorInt = colorProvider.getColorFromAttribute(R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // mark as ignored to avoid showing it again
                            unknownDeviceDetectorSharedViewModel.handle(
                                    UnknownDeviceDetectorSharedViewModel.Action.IgnoreDevice(oldUnverified.mapNotNull { it.deviceId })
                            )
                            it.navigator.openSettings(it, EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS)
                        }
                    }
                    dismissedAction = Runnable {
                        unknownDeviceDetectorSharedViewModel.handle(
                                UnknownDeviceDetectorSharedViewModel.Action.IgnoreDevice(oldUnverified.mapNotNull { it.deviceId })
                        )
                    }
                }
        )
    }

    private fun onGroupChange(groupSummary: GroupSummary?) {
        if (groupSummary == null) {
            views.groupToolbarSpaceTitleView.isVisible = false
        } else {
            views.groupToolbarSpaceTitleView.isVisible = true
            views.groupToolbarSpaceTitleView.text = groupSummary.displayName
        }
    }

    private fun onSpaceChange(spaceSummary: RoomSummary?) {
        if (spaceSummary == null) {
            views.groupToolbarSpaceTitleView.isVisible = false
        } else {
            views.groupToolbarSpaceTitleView.isVisible = true
            views.groupToolbarSpaceTitleView.text = spaceSummary.displayName
        }
    }

    private fun setupKeysBackupBanner() {
        serverBackupStatusViewModel
                .onEach {
                    when (val banState = it.bannerState.invoke()) {
                        is BannerState.Setup  -> views.homeKeysBackupBanner.render(KeysBackupBanner.State.Setup(banState.numberOfKeys), false)
                        BannerState.BackingUp -> views.homeKeysBackupBanner.render(KeysBackupBanner.State.BackingUp, false)
                        null,
                        BannerState.Hidden    -> views.homeKeysBackupBanner.render(KeysBackupBanner.State.Hidden, false)
                    }
                }
        views.homeKeysBackupBanner.delegate = this
    }

    private fun setupActiveCallView() {
        currentCallsViewPresenter.bind(views.currentCallsView, this)
    }

    private fun setupToolbar() {
        val parentActivity = vectorBaseActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(views.groupToolbar)
        }
        views.groupToolbar.title = ""
        views.groupToolbarAvatarImageView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.OpenDrawer)
        }

        views.homeToolbarContent.debouncedClicks {
            withState(viewModel) {
                when (it.roomGroupingMethod) {
                    is RoomGroupingMethod.ByLegacyGroup -> {
                        // nothing do far
                    }
                    is RoomGroupingMethod.BySpace       -> {
                        it.roomGroupingMethod.spaceSummary?.let {
                            sharedActionViewModel.post(HomeActivitySharedAction.ShowSpaceSettings(it.roomId))
                        }
                    }
                }
            }
        }
    }

    private fun setupBottomNavigationView() {
        val combinedOverview = vectorPreferences.combinedOverview()
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_notification).isVisible = vectorPreferences.labAddNotificationTab()
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_people).isVisible = !combinedOverview
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_rooms).isVisible = !combinedOverview
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_all).isVisible = combinedOverview
        views.bottomNavigationView.setOnItemSelectedListener {
            val tab = when (it.itemId) {
                R.id.bottom_action_people       -> HomeTab.RoomList(RoomListDisplayMode.PEOPLE)
                R.id.bottom_action_rooms        -> HomeTab.RoomList(RoomListDisplayMode.ROOMS)
                R.id.bottom_action_notification -> HomeTab.RoomList(RoomListDisplayMode.NOTIFICATIONS)
                R.id.bottom_action_all          -> HomeTab.RoomList(RoomListDisplayMode.ALL)
                else                            -> HomeTab.DialPad
            }
            viewModel.handle(HomeDetailAction.SwitchTab(tab))
            true
        }

//        val menuView = bottomNavigationView.getChildAt(0) as BottomNavigationMenuView

//        bottomNavigationView.getOrCreateBadge()
//        menuView.forEachIndexed { index, view ->
//            val itemView = view as BottomNavigationItemView
//            val badgeLayout = LayoutInflater.from(requireContext()).inflate(R.layout.vector_home_badge_unread_layout, menuView, false)
//            val unreadCounterBadgeView: UnreadCounterBadgeView = badgeLayout.findViewById(R.id.actionUnreadCounterBadgeView)
//            itemView.addView(badgeLayout)
//            unreadCounterBadgeViews.add(index, unreadCounterBadgeView)
//        }
    }

    private fun updateUIForTab(tab: HomeTab) {
        views.bottomNavigationView.menu.findItem(tab.toMenuId()).isChecked = true
        views.groupToolbarTitleView.setText(tab.titleRes)
        //updateSelectedFragment(tab)
        invalidateOptionsMenu()
    }

    private fun HomeTab.toFragmentTag() = "FRAGMENT_TAG_$this"

    /*
    private fun updateSelectedFragment(tab: HomeTab) {
        val fragmentTag = tab.toFragmentTag()
        val fragmentToShow = childFragmentManager.findFragmentByTag(fragmentTag)
        childFragmentManager.commitTransaction {
            childFragmentManager.fragments
                    .filter { it != fragmentToShow }
                    .forEach {
                        detach(it)
                    }
            if (fragmentToShow == null) {
                when (tab) {
                    is HomeTab.RoomList -> {
                        val params = RoomListParams(tab.displayMode)
                        add(R.id.roomListContainer, RoomListFragment::class.java, params.toMvRxBundle(), fragmentTag)
                    }
                    is HomeTab.DialPad  -> {
                        add(R.id.roomListContainer, createDialPadFragment(), fragmentTag)
                    }
                }
            } else {
                if (tab is HomeTab.DialPad) {
                    (fragmentToShow as? DialPadFragment)?.applyCallback()
                }
                attach(fragmentToShow)
            }
        }
    }
     */

    private fun setupViewPager(roomGroupingMethod: RoomGroupingMethod, spaces: List<RoomSummary>?, tab: HomeTab) {
        val oldAdapter = views.roomListContainerPager.adapter as? FragmentStateAdapter
        val pagingAllowed = vectorPreferences.enableSpacePager() && tab is HomeTab.RoomList
        if (DEBUG_VIEW_PAGER) Timber.i("Home pager: setup, old adapter: $oldAdapter")
        val unsafeSpaces = spaces?.map { it.roomId } ?: listOf()
        val selectedSpaceId = (roomGroupingMethod as? RoomGroupingMethod.BySpace)?.spaceSummary?.roomId
        val selectedIndex = getPageIndexForSpaceId(selectedSpaceId, unsafeSpaces)
        val pagingEnabled = pagingAllowed && roomGroupingMethod is RoomGroupingMethod.BySpace && unsafeSpaces.size > 0 && selectedIndex != null
        val safeSpaces = if (pagingEnabled) unsafeSpaces else listOf()
        // Check if we need to recreate the adapter for a new tab
        if (oldAdapter != null) {
            val changed = pagerTab != tab || pagerSpaces != safeSpaces || pagerPagingEnabled != pagingEnabled
            if (DEBUG_VIEW_PAGER) Timber.i("Home pager: has changed: $changed (${pagerTab != tab} ${pagerSpaces != safeSpaces} ${pagerPagingEnabled != pagingEnabled} $selectedIndex ${views.roomListContainerPager.currentItem}) | $safeSpaces")
            if (!changed) {
                if (pagingEnabled) {
                    // No need to re-setup pager, just check for selected page
                    // Discard state changes that we created ourselves by swiping on the pager
                    while (pendingSpaceIds.size > 0) {
                        val pendingSpaceId = pendingSpaceIds.removeAt(0)
                        if (pendingSpaceId == selectedSpaceId) {
                            return
                        }
                    }
                    if (selectedIndex != null) {
                        if (selectedIndex != views.roomListContainerPager.currentItem) {
                            // post() mitigates a case where we could end up in an endless loop circling around the same few spaces
                            views.roomListContainerPager.post {
                                // Do not smooth scroll large distances to avoid loading unnecessary many room lists
                                val diff = selectedIndex - views.roomListContainerPager.currentItem
                                val smoothScroll = abs(diff) <= 1
                                views.roomListContainerPager.setCurrentItem(selectedIndex, smoothScroll)
                            }
                        }
                        return
                    }
                } else {
                    // Nothing to change
                    return
                }
            } else {
                // Clean up old fragments
                val transaction = childFragmentManager.beginTransaction()
                childFragmentManager.fragments
                        .forEach {
                            transaction.detach(it)
                        }
                transaction.commit()
            }
        }
        pagerSpaces = safeSpaces
        pagerTab = tab
        pagerPagingEnabled = pagingEnabled
        initialPageSelected = false
        pendingSpaceIds.clear()

        // OFFSCREEN_PAGE_LIMIT_DEFAULT: default recyclerview caching mechanism instead of explicit fixed prefetching
        //views.roomListContainerPager.offscreenPageLimit = 2

        val adapter = object: FragmentStateAdapter(this@HomeDetailFragment) {
            override fun getItemCount(): Int {
                if (!pagingEnabled) {
                    return 1
                }
                return when (tab) {
                    is HomeTab.DialPad -> 1
                    else -> safeSpaces.size + 1
                }
            }

            override fun createFragment(position: Int): Fragment {
                if (DEBUG_VIEW_PAGER) Timber.i("Home pager: create fragment for position $position")
                return when (tab) {
                    is HomeTab.DialPad -> createDialPadFragment()
                    is HomeTab.RoomList -> {
                        val params = if (pagingEnabled) {
                            RoomListParams(tab.displayMode, getSpaceIdForPageIndex(position)).toMvRxBundle()
                        } else {
                            RoomListParams(tab.displayMode, SPACE_ID_FOLLOW_APP).toMvRxBundle()
                        }
                        childFragmentManager.fragmentFactory.instantiate(activity!!.classLoader, RoomListFragment::class.java.name).apply {
                            arguments = params
                        }
                    }
                }
            }
        }

        views.roomListContainerPager.adapter = adapter
        if (pagingEnabled) {
            views.roomListContainerPager.post {
                try {
                    if (DEBUG_VIEW_PAGER) Timber.i("Home pager: set initial page $selectedIndex")
                    views.roomListContainerPager.setCurrentItem(selectedIndex ?: 0, false)
                    initialPageSelected = true
                } catch (e: Exception) {
                    Timber.e("Home pager: Could not set initial page after creating adapter: $e")
                }
            }
        }
    }

    private fun getPageIndexForSpaceId(spaceId: String?, spaces: List<String?>? = pagerSpaces): Int? {
        if (spaceId == null) {
            return 0
        }
        val indexInList = spaces?.indexOf(spaceId)
        return when (indexInList) {
            null, -1 -> null
            else -> indexInList + 1
        }
    }

    private fun getSpaceIdForPageIndex(position: Int, spaces: List<String?>? = pagerSpaces): String? {
        val safeSpaces = spaces ?: return null
        return if (position == 0) null else safeSpaces[position-1]
    }

    private fun createDialPadFragment(): Fragment {
        val fragment = childFragmentManager.fragmentFactory.instantiate(vectorBaseActivity.classLoader, DialPadFragment::class.java.name)
        return (fragment as DialPadFragment).apply {
            arguments = Bundle().apply {
                putBoolean(DialPadFragment.EXTRA_ENABLE_DELETE, true)
                putBoolean(DialPadFragment.EXTRA_ENABLE_OK, true)
                putString(DialPadFragment.EXTRA_REGION_CODE, VectorLocale.applicationLocale.country)
            }
            applyCallback()
        }
    }

    private fun updateTabVisibilitySafely(tabId: Int, isVisible: Boolean) {
        val wasVisible = views.bottomNavigationView.menu.findItem(tabId).isVisible
        views.bottomNavigationView.menu.findItem(tabId).isVisible = isVisible
        if (wasVisible && !isVisible) {
            // As we hide it check if it's not the current item!
            withState(viewModel) {
                if (it.currentTab.toMenuId() == tabId) {
                    val combinedOverview = vectorPreferences.combinedOverview()
                    if (combinedOverview) {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.PEOPLE)))
                    } else {
                        viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.ALL)))
                    }
                }
            }
        }
    }

    /* ==========================================================================================
     * KeysBackupBanner Listener
     * ========================================================================================== */

    override fun setupKeysBackup() {
        navigator.openKeysBackupSetup(requireActivity(), false)
    }

    override fun recoverKeysBackup() {
        navigator.openKeysBackupManager(requireActivity())
    }

    override fun invalidate() = withState(viewModel) {
//        Timber.v(it.toString())
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_people).render(it.notificationCountPeople, it.notificationHighlightPeople)
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_rooms).render(it.notificationCountRooms, it.notificationHighlightRooms)
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_notification).render(it.notificationCountCatchup, it.notificationHighlightCatchup)
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_all).render(it.notificationCountCatchup, it.notificationHighlightCatchup)
        views.syncStateView.render(
                it.syncState,
                it.incrementalSyncStatus,
                it.pushCounter,
                vectorPreferences.developerShowDebugInfo())

        hasUnreadRooms = it.hasUnreadMessages
    }

    private fun BadgeDrawable.render(count: Int, highlight: Boolean) {
        isVisible = count > 0
        number = count
        maxCharacterCount = 3
        badgeTextColor = ThemeUtils.getColor(requireContext(), R.attr.colorOnPrimary)
        backgroundColor = if (highlight) {
            ThemeUtils.getColor(requireContext(), R.attr.colorError)
        } else {
            ThemeUtils.getColor(requireContext(), R.attr.vctr_unread_room_badge)
        }
    }

    private fun HomeTab.toMenuId() = when (this) {
        is HomeTab.DialPad  -> R.id.bottom_action_dial_pad
        is HomeTab.RoomList -> when (displayMode) {
            RoomListDisplayMode.PEOPLE -> R.id.bottom_action_people
            RoomListDisplayMode.ROOMS  -> R.id.bottom_action_rooms
            RoomListDisplayMode.ALL    -> R.id.bottom_action_all
            else                       -> R.id.bottom_action_notification
        }
    }

    override fun onTapToReturnToCall() {
        callManager.getCurrentCall()?.let { call ->
            VectorCallActivity.newIntent(
                    context = requireContext(),
                    callId = call.callId,
                    signalingRoomId = call.signalingRoomId,
                    otherUserId = call.mxCall.opponentUserId,
                    isIncomingCall = !call.mxCall.isOutgoing,
                    isVideoCall = call.mxCall.isVideoCall,
                    mode = null
            ).let {
                startActivity(it)
            }
        }
    }

    private fun DialPadFragment.applyCallback(): DialPadFragment {
        callback = object : DialPadFragment.Callback {
            override fun onOkClicked(formatted: String?, raw: String?) {
                if (raw.isNullOrEmpty()) return
                viewModel.handle(HomeDetailAction.StartCallWithPhoneNumber(raw))
            }
        }
        return this
    }
}
