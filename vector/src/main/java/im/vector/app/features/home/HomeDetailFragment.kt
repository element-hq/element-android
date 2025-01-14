/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.badge.BadgeDrawable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.SpaceStateHandler
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
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
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.app.features.settings.VectorLocaleProvider
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity.Companion.EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.workers.signout.BannerState
import im.vector.app.features.workers.signout.ServerBackupStatusAction
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

@AndroidEntryPoint
class HomeDetailFragment :
        VectorBaseFragment<FragmentHomeDetailBinding>(),
        KeysBackupBanner.Delegate,
        CurrentCallsView.Callback,
        OnBackPressed,
        VectorMenuProvider {

    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var alertManager: PopupAlertManager
    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var spaceStateHandler: SpaceStateHandler
    @Inject lateinit var vectorLocale: VectorLocaleProvider

    private val viewModel: HomeDetailViewModel by fragmentViewModel()
    private val unknownDeviceDetectorSharedViewModel: UnknownDeviceDetectorSharedViewModel by activityViewModel()
    private val unreadMessagesSharedViewModel: UnreadMessagesSharedViewModel by activityViewModel()
    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private lateinit var sharedCallActionViewModel: SharedKnownCallsViewModel

    private var hasUnreadRooms = false
        set(value) {
            if (value != field) {
                field = value
                invalidateOptionsMenu()
            }
        }

    override fun getMenuRes() = R.menu.room_list

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home_mark_all_as_read -> {
                viewModel.handle(HomeDetailAction.MarkAllRoomsRead)
                true
            }
            else -> false
        }
    }

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            val isRoomList = state.currentTab is HomeTab.RoomList
            menu.findItem(R.id.menu_home_mark_all_as_read).isVisible = isRoomList && hasUnreadRooms
        }
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

        withState(viewModel) {
            // Update the navigation view if needed (for when we restore the tabs)
            views.bottomNavigationView.selectedItemId = it.currentTab.toMenuId()
        }

        viewModel.onEach(HomeDetailViewState::selectedSpace) { selectedSpace ->
            onSpaceChange(selectedSpace)
        }

        viewModel.onEach(HomeDetailViewState::currentTab) { currentTab ->
            updateUIForTab(currentTab)
        }

        viewModel.onEach(HomeDetailViewState::showDialPadTab) { showDialPadTab ->
            updateTabVisibilitySafely(R.id.bottom_action_dial_pad, showDialPadTab)
        }

        viewModel.observeViewEvents { viewEvent ->
            when (viewEvent) {
                HomeDetailViewEvents.CallStarted -> handleCallStarted()
                is HomeDetailViewEvents.FailToCall -> showFailure(viewEvent.failure)
                HomeDetailViewEvents.Loading -> showLoadingDialog()
            }
        }

        unknownDeviceDetectorSharedViewModel.onEach { state ->
            state.unknownSessions.invoke()?.let { unknownDevices ->
                if (unknownDevices.firstOrNull()?.currentSessionTrust == true) {
                    val uid = PopupAlertManager.REVIEW_LOGIN_UID
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
                    UnreadCounterBadgeView.State.Count(
                            count = state.otherSpacesUnread.totalCount,
                            highlighted = state.otherSpacesUnread.isHighlight
                    )
            )
        }

        sharedCallActionViewModel
                .liveKnownCalls
                .observe(viewLifecycleOwner) {
                    currentCallsViewPresenter.updateCall(callManager.getCurrentCall(), callManager.getCalls())
                    invalidateOptionsMenu()
                }
    }

    private fun navigateBack() {
        val previousSpaceId = spaceStateHandler.popSpaceBackstack()
        val parentSpaceId = spaceStateHandler.getCurrentSpace()?.flattenParentIds?.lastOrNull()
        setCurrentSpace(previousSpaceId ?: parentSpaceId)
    }

    private fun setCurrentSpace(spaceId: String?) {
        spaceStateHandler.setCurrentSpace(spaceId, isForwardNavigation = false)
        sharedActionViewModel.post(HomeActivitySharedAction.OnCloseSpace)
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
        updateTabVisibilitySafely(R.id.bottom_action_notification, vectorPreferences.labAddNotificationTab())
        callManager.checkForProtocolsSupportIfNeeded()
        refreshSpaceState()
    }

    private fun refreshSpaceState() {
        spaceStateHandler.getCurrentSpace()?.let {
            onSpaceChange(it)
        }
    }

    private fun promptForNewUnknownDevices(uid: String, state: UnknownDevicesState, newest: DeviceInfo) {
        val user = state.myMatrixItem
        alertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = uid,
                        title = getString(CommonStrings.new_session),
                        description = getString(CommonStrings.verify_this_session, newest.displayName ?: newest.deviceId ?: ""),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer)
                    colorInt = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let { vectorBaseActivity ->
                            vectorBaseActivity.navigator
                                    .requestSessionVerification(vectorBaseActivity, newest.deviceId ?: "")
                        }
                        unknownDeviceDetectorSharedViewModel.handle(
                                UnknownDeviceDetectorSharedViewModel.Action.IgnoreNewLogin(newest.deviceId?.let { listOf(it) }.orEmpty())
                        )
                    }
                    dismissedAction = Runnable {
                        unknownDeviceDetectorSharedViewModel.handle(
                                UnknownDeviceDetectorSharedViewModel.Action.IgnoreNewLogin(newest.deviceId?.let { listOf(it) }.orEmpty())
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
                        title = getString(CommonStrings.review_unverified_sessions_title),
                        description = getString(CommonStrings.review_unverified_sessions_description),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer)
                    colorInt = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let { activity ->
                            // mark as ignored to avoid showing it again
                            unknownDeviceDetectorSharedViewModel.handle(
                                    UnknownDeviceDetectorSharedViewModel.Action.IgnoreDevice(oldUnverified.mapNotNull { it.deviceId })
                            )
                            activity.navigator.openSettings(activity, EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS)
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

    private fun onSpaceChange(spaceSummary: RoomSummary?) {
        if (spaceSummary == null) {
            views.groupToolbarSpaceTitleView.isVisible = false
        } else {
            views.groupToolbarSpaceTitleView.isVisible = true
            views.groupToolbarSpaceTitleView.text = spaceSummary.displayName
        }
    }

    private fun setupKeysBackupBanner() {
        serverBackupStatusViewModel.handle(ServerBackupStatusAction.OnBannerDisplayed)
        serverBackupStatusViewModel
                .onEach {
                    when (val banState = it.bannerState.invoke()) {
                        is BannerState.Setup,
                        BannerState.BackingUp,
                        BannerState.Hidden -> views.homeKeysBackupBanner.render(banState, false)
                        null -> views.homeKeysBackupBanner.render(BannerState.Hidden, false)
                        else -> Unit /* No op? */
                    }
                }
        views.homeKeysBackupBanner.delegate = this
    }

    private fun setupActiveCallView() {
        currentCallsViewPresenter.bind(views.currentCallsView, this)
    }

    private fun setupToolbar() {
        setupToolbar(views.groupToolbar)
                .setTitle(null)

        views.groupToolbarAvatarImageView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.OpenDrawer)
        }

        views.homeToolbarContent.debouncedClicks {
            withState(viewModel) { viewState ->
                viewState.selectedSpace?.let {
                    sharedActionViewModel.post(HomeActivitySharedAction.ShowSpaceSettings(it.roomId))
                }
            }
        }
    }

    private fun setupBottomNavigationView() {
        views.bottomNavigationView.menu.findItem(R.id.bottom_action_notification).isVisible = vectorPreferences.labAddNotificationTab()
        views.bottomNavigationView.setOnItemSelectedListener {
            val tab = when (it.itemId) {
                R.id.bottom_action_people -> HomeTab.RoomList(RoomListDisplayMode.PEOPLE)
                R.id.bottom_action_rooms -> HomeTab.RoomList(RoomListDisplayMode.ROOMS)
                R.id.bottom_action_notification -> HomeTab.RoomList(RoomListDisplayMode.NOTIFICATIONS)
                else -> HomeTab.DialPad
            }
            viewModel.handle(HomeDetailAction.SwitchTab(tab))
            true
        }
    }

    private fun updateUIForTab(tab: HomeTab) {
        views.bottomNavigationView.menu.findItem(tab.toMenuId()).isChecked = true
        views.groupToolbarTitleView.setText(tab.titleRes)
        updateSelectedFragment(tab)
        invalidateOptionsMenu()
    }

    private fun HomeTab.toFragmentTag() = "FRAGMENT_TAG_$this"

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
                    is HomeTab.DialPad -> {
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

    private fun createDialPadFragment(): Fragment {
        val fragment = childFragmentManager.fragmentFactory.instantiate(vectorBaseActivity.classLoader, DialPadFragment::class.java.name)
        return (fragment as DialPadFragment).apply {
            arguments = Bundle().apply {
                putBoolean(DialPadFragment.EXTRA_ENABLE_DELETE, true)
                putBoolean(DialPadFragment.EXTRA_ENABLE_OK, true)
                putString(DialPadFragment.EXTRA_REGION_CODE, vectorLocale.applicationLocale.country)
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
                    viewModel.handle(HomeDetailAction.SwitchTab(HomeTab.RoomList(RoomListDisplayMode.PEOPLE)))
                }
            }
        }
    }

    /* ==========================================================================================
     * KeysBackupBanner Listener
     * ========================================================================================== */

    override fun onCloseClicked() {
        serverBackupStatusViewModel.handle(ServerBackupStatusAction.OnBannerClosed)
    }

    override fun setupKeysBackup() {
        navigator.openKeysBackupSetup(requireActivity(), false)
    }

    override fun recoverKeysBackup() {
        navigator.openKeysBackupManager(requireActivity())
    }

    override fun invalidate() = withState(viewModel) {
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_people).render(it.notificationCountPeople, it.notificationHighlightPeople)
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_rooms).render(it.notificationCountRooms, it.notificationHighlightRooms)
        views.bottomNavigationView.getOrCreateBadge(R.id.bottom_action_notification).render(it.notificationCountCatchup, it.notificationHighlightCatchup)
        views.syncStateView.render(
                it.syncState,
                it.incrementalSyncRequestState,
                it.pushCounter,
                vectorPreferences.developerShowDebugInfo()
        )

        hasUnreadRooms = it.hasUnreadMessages
    }

    private fun BadgeDrawable.render(count: Int, highlight: Boolean) {
        isVisible = count > 0
        number = count
        maxCharacterCount = 3
        badgeTextColor = ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary)
        backgroundColor = if (highlight) {
            ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorError)
        } else {
            ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_unread_background)
        }
    }

    private fun HomeTab.toMenuId() = when (this) {
        is HomeTab.DialPad -> R.id.bottom_action_dial_pad
        is HomeTab.RoomList -> when (displayMode) {
            RoomListDisplayMode.PEOPLE -> R.id.bottom_action_people
            RoomListDisplayMode.ROOMS -> R.id.bottom_action_rooms
            else -> R.id.bottom_action_notification
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

    override fun onBackPressed(toolbarButton: Boolean) = if (spaceStateHandler.getCurrentSpace() != null) {
        navigateBack()
        true
    } else {
        false
    }
}
