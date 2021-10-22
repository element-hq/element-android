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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.gouv.tchap.features.home.contact.list.TchapContactListViewModel
import fr.gouv.tchap.features.home.contact.list.TchapContactListViewState
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.pushers.PushersManager
import im.vector.app.databinding.ActivityHomeBinding
import im.vector.app.features.contactsbook.ContactsBookViewModel
import im.vector.app.features.contactsbook.ContactsBookViewState
import im.vector.app.features.disclaimer.showDisclaimerDialog
import im.vector.app.features.home.room.list.RoomListViewModel
import im.vector.app.features.home.room.list.RoomListViewModelFactory
import im.vector.app.features.home.room.list.RoomListViewState
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.permalink.PermalinkHandler.Companion.MATRIX_TO_CUSTOM_SCHEME_URL_BASE
import im.vector.app.features.permalink.PermalinkHandler.Companion.ROOM_LINK_PREFIX
import im.vector.app.features.permalink.PermalinkHandler.Companion.USER_LINK_PREFIX
import im.vector.app.features.popup.DefaultVectorAlert
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.app.features.rageshake.ReportType
import im.vector.app.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.spaces.RestrictedPromoBottomSheet
import im.vector.app.features.spaces.SpaceCreationActivity
import im.vector.app.features.spaces.SpacePreviewActivity
import im.vector.app.features.spaces.SpaceSettingsMenuBottomSheet
import im.vector.app.features.spaces.invite.SpaceInviteBottomSheet
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.app.features.workers.signout.ServerBackupStatusViewState
import im.vector.app.push.fcm.FcmHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class HomeActivityArgs(
        val clearNotification: Boolean,
        val accountCreation: Boolean,
        val inviteNotificationRoomId: String? = null
) : Parcelable

class HomeActivity :
        VectorBaseActivity<ActivityHomeBinding>(),
        ToolbarConfigurable,
        UnknownDeviceDetectorSharedViewModel.Factory,
        ServerBackupStatusViewModel.Factory,
        TchapContactListViewModel.Factory,
        RoomListViewModel.Factory,
        ContactsBookViewModel.Factory,
        UnreadMessagesSharedViewModel.Factory,
        PromoteRestrictedViewModel.Factory,
        NavigationInterceptor,
        SpaceInviteBottomSheet.InteractionListener,
        MatrixToBottomSheet.InteractionListener {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()
    @Inject lateinit var viewModelFactory: HomeActivityViewModel.Factory

    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by viewModel()
    @Inject lateinit var serverBackupviewModelFactory: ServerBackupStatusViewModel.Factory
    @Inject lateinit var promoteRestrictedViewModelFactory: PromoteRestrictedViewModel.Factory
    private val promoteRestrictedViewModel: PromoteRestrictedViewModel by viewModel()

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var pushManager: PushersManager
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var shortcutsHandler: ShortcutsHandler
    @Inject lateinit var unknownDeviceViewModelFactory: UnknownDeviceDetectorSharedViewModel.Factory
    @Inject lateinit var unreadMessagesSharedViewModelFactory: UnreadMessagesSharedViewModel.Factory
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var tchapContactViewModelFactory: TchapContactListViewModel.Factory
    @Inject lateinit var contactsBookViewModelFactory: ContactsBookViewModel.Factory
    @Inject lateinit var roomListViewModelFactory: RoomListViewModelFactory
    @Inject lateinit var initSyncStepFormatter: InitSyncStepFormatter
    @Inject lateinit var appStateHandler: AppStateHandler

    private val createSpaceResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val spaceId = SpaceCreationActivity.getCreatedSpaceId(activityResult.data)
            val defaultRoomId = SpaceCreationActivity.getDefaultRoomId(activityResult.data)
            val isJustMe = SpaceCreationActivity.isJustMeSpace(activityResult.data)
            views.drawerLayout.closeDrawer(GravityCompat.START)

            val postSwitchOption: Navigator.PostSwitchSpaceAction = if (defaultRoomId != null) {
                Navigator.PostSwitchSpaceAction.OpenDefaultRoom(defaultRoomId, !isJustMe)
            } else if (isJustMe) {
                Navigator.PostSwitchSpaceAction.OpenAddExistingRooms
            } else {
                Navigator.PostSwitchSpaceAction.None
            }
            // Here we want to change current space to the newly created one, and then immediately open the default room
            if (spaceId != null) {
                navigator.switchToSpace(context = this,
                        spaceId = spaceId,
                        postSwitchOption)
            }
        }
    }

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = this@HomeActivity
            }
            super.onFragmentResumed(fm, f)
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = null
            }
            super.onFragmentPaused(fm, f)
        }
    }

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getBinding() = ActivityHomeBinding.inflate(layoutInflater)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun create(initialState: UnknownDevicesState): UnknownDeviceDetectorSharedViewModel {
        return unknownDeviceViewModelFactory.create(initialState)
    }

    override fun create(initialState: ServerBackupStatusViewState): ServerBackupStatusViewModel {
        return serverBackupviewModelFactory.create(initialState)
    }

    override fun create(initialState: TchapContactListViewState) = tchapContactViewModelFactory.create(initialState)

    override fun create(initialState: ContactsBookViewState): ContactsBookViewModel = contactsBookViewModelFactory.create(initialState)

    override fun create(initialState: RoomListViewState): RoomListViewModel = roomListViewModelFactory.create(initialState)

    override fun create(initialState: UnreadMessagesState): UnreadMessagesSharedViewModel {
        return unreadMessagesSharedViewModelFactory.create(initialState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
        FcmHelper.ensureFcmTokenIsRetrieved(this, pushManager, vectorPreferences.areNotificationEnabledForDevice())
        sharedActionViewModel = viewModelProvider.get(HomeSharedActionViewModel::class.java)
        views.drawerLayout.addDrawerListener(drawerListener)
        if (isFirstCreation()) {
            replaceFragment(R.id.homeDetailFragmentContainer, HomeDetailFragment::class.java)
            replaceFragment(R.id.homeDrawerFragmentContainer, HomeDrawerFragment::class.java)
        }

        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        is HomeActivitySharedAction.OpenDrawer        -> views.drawerLayout.openDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.CloseDrawer       -> views.drawerLayout.closeDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.OpenGroup         -> {
                            views.drawerLayout.closeDrawer(GravityCompat.START)

                            // Temporary
                            // When switching from space to group or group to space, we need to reload the fragment
                            // To be removed when dropping legacy groups
                            if (sharedAction.clearFragment) {
                                replaceFragment(R.id.homeDetailFragmentContainer, HomeDetailFragment::class.java, allowStateLoss = true)
                            } else {
                                // nop
                            }
                            // we might want to delay that to avoid having the drawer animation lagging
                            // would be probably better to let the drawer do that? in the on closed callback?
                        }
                        is HomeActivitySharedAction.OpenSpacePreview  -> {
                            startActivity(SpacePreviewActivity.newIntent(this, sharedAction.spaceId))
                        }
                        is HomeActivitySharedAction.AddSpace          -> {
                            createSpaceResultLauncher.launch(SpaceCreationActivity.newIntent(this))
                        }
                        is HomeActivitySharedAction.ShowSpaceSettings -> {
                            // open bottom sheet
                            SpaceSettingsMenuBottomSheet
                                    .newInstance(sharedAction.spaceId, object : SpaceSettingsMenuBottomSheet.InteractionListener {
                                        override fun onShareSpaceSelected(spaceId: String) {
                                            ShareSpaceBottomSheet.show(supportFragmentManager, spaceId)
                                        }
                                    })
                                    .show(supportFragmentManager, "SPACE_SETTINGS")
                        }
                        is HomeActivitySharedAction.OpenSpaceInvite   -> {
                            SpaceInviteBottomSheet.newInstance(sharedAction.spaceId)
                                    .show(supportFragmentManager, "SPACE_INVITE")
                        }
                        HomeActivitySharedAction.SendSpaceFeedBack    -> {
                            bugReporter.openBugReportScreen(this, ReportType.SPACE_BETA_FEEDBACK)
                        }
                        is HomeActivitySharedAction.InviteByEmail    -> {
                            // TODO
                        }
                    }.exhaustive
                }
                .disposeOnDestroy()

        val args = intent.getParcelableExtra<HomeActivityArgs>(MvRx.KEY_ARG)

        if (args?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (args?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(args.inviteNotificationRoomId)?.let {
                navigator.openMatrixToBottomSheet(this, it)
            }
        }

        homeActivityViewModel.observeViewEvents {
            when (it) {
                is HomeActivityViewEvents.AskPasswordToInitCrossSigning -> handleAskPasswordToInitCrossSigning(it)
                is HomeActivityViewEvents.OnNewSession                  -> handleOnNewSession(it)
                HomeActivityViewEvents.PromptToEnableSessionPush        -> handlePromptToEnablePush()
                is HomeActivityViewEvents.OnCrossSignedInvalidated      -> handleCrossSigningInvalidated(it)
            }.exhaustive
        }
        homeActivityViewModel.subscribe(this) { renderState(it) }

        shortcutsHandler.observeRoomsAndBuildShortcuts()
                .disposeOnDestroy()

        if (!vectorPreferences.didPromoteNewRestrictedFeature()) {
            promoteRestrictedViewModel.subscribe(this) {
                if (it.activeSpaceSummary != null && !it.activeSpaceSummary.isPublic &&
                        it.activeSpaceSummary.otherMemberIds.isNotEmpty()) {
                    // It's a private space with some members show this once
                    if (it.canUserManageSpace && !popupAlertManager.hasAlertsToShow()) {
                        if (!vectorPreferences.didPromoteNewRestrictedFeature()) {
                            vectorPreferences.setDidPromoteNewRestrictedFeature()
                            RestrictedPromoBottomSheet().show(supportFragmentManager, "RestrictedPromoBottomSheet")
                        }
                    }
                }
            }
        }

        if (isFirstCreation()) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.let { deepLink ->
            val resolvedLink = when {
                // Element custom scheme is not handled by the sdk, convert it to matrix.to link for compatibility
                deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE) -> {
                    val let = when {
                        deepLink.startsWith(USER_LINK_PREFIX) -> deepLink.substring(USER_LINK_PREFIX.length)
                        deepLink.startsWith(ROOM_LINK_PREFIX) -> deepLink.substring(ROOM_LINK_PREFIX.length)
                        else                                  -> null
                    }?.let { permalinkId ->
                        activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(permalinkId)
                    }
                    let
                }
                else                                                  -> deepLink
            }
            permalinkHandler.launch(
                    context = this,
                    deepLink = resolvedLink,
                    navigationInterceptor = this,
                    buildTask = true
            )
                    // .delay(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isHandled ->
                        if (!isHandled) {
                            val isMatrixToLink = deepLink.startsWith(PermalinkService.MATRIX_TO_URL_BASE) ||
                                    deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE)
                            MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(if (isMatrixToLink) R.string.permalink_malformed else R.string.universal_link_malformed)
                                    .setPositiveButton(R.string.ok, null)
                                    .show()
                        }
                    }
                    .disposeOnDestroy()
        }
    }

    private fun renderState(state: HomeActivityViewState) {
        when (val status = state.syncStatusServiceStatus) {
            is SyncStatusService.Status.Progressing -> {
                val initSyncStepStr = initSyncStepFormatter.format(status.initSyncStep)
                Timber.v("$initSyncStepStr ${status.percentProgress}")
                views.waitingView.root.setOnClickListener {
                    // block interactions
                }
                views.waitingView.waitingHorizontalProgress.apply {
                    isIndeterminate = false
                    max = 100
                    progress = status.percentProgress
                    isVisible = true
                }
                views.waitingView.waitingStatusText.apply {
                    text = initSyncStepStr
                    isVisible = true
                }
                views.waitingView.root.isVisible = true
            }
            else                                    -> {
                // Idle or Incremental sync status
                views.waitingView.root.isVisible = false
            }
        }.exhaustive
    }

    private fun handleAskPasswordToInitCrossSigning(events: HomeActivityViewEvents.AskPasswordToInitCrossSigning) {
        // We need to ask
        promptSecurityEvent(
                events.userItem,
                R.string.upgrade_security,
                R.string.security_prompt_text
        ) {
            it.navigator.upgradeSessionSecurity(it, true)
        }
    }

    private fun handleCrossSigningInvalidated(event: HomeActivityViewEvents.OnCrossSignedInvalidated) {
        // We need to ask
        promptSecurityEvent(
                event.userItem,
                R.string.crosssigning_verify_this_session,
                R.string.confirm_your_identity
        ) {
            it.navigator.waitSessionVerification(it)
        }
    }

    private fun handleOnNewSession(event: HomeActivityViewEvents.OnNewSession) {
        // We need to ask
        promptSecurityEvent(
                event.userItem,
                R.string.crosssigning_verify_this_session,
                R.string.confirm_your_identity
        ) {
            if (event.waitForIncomingRequest) {
                it.navigator.waitSessionVerification(it)
            } else {
                it.navigator.requestSelfSessionVerification(it)
            }
        }
    }

    private fun handlePromptToEnablePush() {
        popupAlertManager.postVectorAlert(
                DefaultVectorAlert(
                        uid = "enablePush",
                        title = getString(R.string.alert_push_are_disabled_title),
                        description = getString(R.string.alert_push_are_disabled_description),
                        iconId = R.drawable.ic_room_actions_notifications_mutes,
                        shouldBeDisplayedIn = {
                            it is HomeActivity
                        }
                ).apply {
                    colorInt = ThemeUtils.getColor(this@HomeActivity, R.attr.vctr_notice_secondary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // action(it)
                            homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                            it.navigator.openSettings(it, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
                        }
                    }
                    dismissedAction = Runnable {
                        homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                    }
                    addButton(getString(R.string.dismiss), {
                        homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                    }, true)
                    addButton(getString(R.string.settings), {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // action(it)
                            homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                            it.navigator.openSettings(it, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
                        }
                    }, true)
                }
        )
    }

    private fun promptSecurityEvent(userItem: MatrixItem.UserItem?, titleRes: Int, descRes: Int, action: ((VectorBaseActivity<*>) -> Unit)) {
        popupAlertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = "upgradeSecurity",
                        title = getString(titleRes),
                        description = getString(descRes),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(userItem, avatarRenderer)
                    colorInt = ThemeUtils.getColor(this@HomeActivity, R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            action(it)
                        }
                    }
                    dismissedAction = Runnable {}
                }
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val parcelableExtra = intent?.getParcelableExtra<HomeActivityArgs>(MvRx.KEY_ARG)
        if (parcelableExtra?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (parcelableExtra?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()
                    ?.permalinkService()
                    ?.createPermalink(parcelableExtra.inviteNotificationRoomId)?.let {
                        navigator.openMatrixToBottomSheet(this, it)
                    }
        }
        handleIntent(intent)
    }

    override fun onDestroy() {
        views.drawerLayout.removeDrawerListener(drawerListener)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (vectorUncaughtExceptionHandler.didAppCrash(this)) {
            vectorUncaughtExceptionHandler.clearAppCrashStatus(this)

            MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.send_bug_report_app_crashed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> bugReporter.openBugReportScreen(this) }
                    .setNegativeButton(R.string.no) { _, _ -> bugReporter.deleteCrashFile(this) }
                    .show()
        } else {
            showDisclaimerDialog(this)
        }

        // Force remote backup state update to update the banner if needed
        serverBackupStatusViewModel.refreshRemoteStateIfNeeded()
    }

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar, false)
    }

//    override fun getMenuRes() = R.menu.home
//
//    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//        menu.findItem(R.id.menu_home_init_sync_legacy).isVisible = vectorPreferences.developerMode()
//        menu.findItem(R.id.menu_home_init_sync_optimized).isVisible = vectorPreferences.developerMode()
//        return super.onPrepareOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.menu_home_suggestion          -> {
//                bugReporter.openBugReportScreen(this, ReportType.SUGGESTION)
//                return true
//            }
//            R.id.menu_home_report_bug          -> {
//                bugReporter.openBugReportScreen(this, ReportType.BUG_REPORT)
//                return true
//            }
//            R.id.menu_home_init_sync_legacy    -> {
//                // Configure the SDK
//                initialSyncStrategy = InitialSyncStrategy.Legacy
//                // And clear cache
//                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
//                return true
//            }
//            R.id.menu_home_init_sync_optimized -> {
//                // Configure the SDK
//                initialSyncStrategy = InitialSyncStrategy.Optimized()
//                // And clear cache
//                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
//                return true
//            }
//            R.id.menu_home_filter              -> {
//                navigator.openRoomsFiltering(this)
//                return true
//            }
//            R.id.menu_home_setting             -> {
//                navigator.openSettings(this)
//                return true
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }

    override fun onBackPressed() {
        if (views.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            views.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        // TODO check if there is already one??
        MatrixToBottomSheet.withLink(deepLink.toString())
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri?): Boolean {
        if (roomId == null) return false
        MatrixToBottomSheet.withLink(deepLink.toString())
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun spaceInviteBottomSheetOnAccept(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.None)
    }

    override fun spaceInviteBottomSheetOnDecline(spaceId: String) {
        // nop
    }

    companion object {
        fun newIntent(context: Context,
                      clearNotification: Boolean = false,
                      accountCreation: Boolean = false,
                      inviteNotificationRoomId: String? = null
        ): Intent {
            val args = HomeActivityArgs(
                    clearNotification = clearNotification,
                    accountCreation = accountCreation,
                    inviteNotificationRoomId = inviteNotificationRoomId
            )

            return Intent(context, HomeActivity::class.java)
                    .apply {
                        putExtra(MvRx.KEY_ARG, args)
                    }
        }
    }

    override fun create(initialState: ActiveSpaceViewState) = promoteRestrictedViewModelFactory.create(initialState)

    override fun mxToBottomSheetNavigateToRoom(roomId: String) {
        navigator.openRoom(this, roomId)
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.None)
    }
}
