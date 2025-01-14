/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.SpaceStateHandler
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.restart
import im.vector.app.core.extensions.validateBackPressed
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.ActivityHomeBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.accountdata.AnalyticsAccountDataViewModel
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.home.room.list.actions.RoomListSharedAction
import im.vector.app.features.home.room.list.actions.RoomListSharedActionViewModel
import im.vector.app.features.home.room.list.home.layout.HomeLayoutSettingBottomDialogFragment
import im.vector.app.features.home.room.list.home.release.ReleaseNotesActivity
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.onboarding.AuthenticationDescription
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
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.spaces.SpaceCreationActivity
import im.vector.app.features.spaces.SpacePreviewActivity
import im.vector.app.features.spaces.SpaceSettingsMenuBottomSheet
import im.vector.app.features.spaces.invite.SpaceInviteBottomSheet
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.usercode.UserCodeActivity
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.session.sync.initialSyncStrategy
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class HomeActivityArgs(
        val clearNotification: Boolean,
        val authenticationDescription: AuthenticationDescription? = null,
        val hasExistingSession: Boolean = false,
        val inviteNotificationRoomId: String? = null
) : Parcelable

@AndroidEntryPoint
class HomeActivity :
        VectorBaseActivity<ActivityHomeBinding>(),
        NavigationInterceptor,
        SpaceInviteBottomSheet.InteractionListener,
        MatrixToBottomSheet.InteractionListener,
        VectorMenuProvider {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private lateinit var roomListSharedActionViewModel: RoomListSharedActionViewModel

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()

    @Suppress("UNUSED")
    private val analyticsAccountDataViewModel: AnalyticsAccountDataViewModel by viewModel()

    @Suppress("UNUSED")
    private val userColorAccountDataViewModel: UserColorAccountDataViewModel by viewModel()

    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by viewModel()

    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var shortcutsHandler: ShortcutsHandler
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var initSyncStepFormatter: InitSyncStepFormatter
    @Inject lateinit var spaceStateHandler: SpaceStateHandler
    @Inject lateinit var unifiedPushHelper: UnifiedPushHelper
    @Inject lateinit var nightlyProxy: NightlyProxy
    @Inject lateinit var notificationPermissionManager: NotificationPermissionManager

    private var isNewAppLayoutEnabled: Boolean = false // delete once old app layout is removed

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
                navigator.switchToSpace(
                        context = this,
                        spaceId = spaceId,
                        postSwitchOption,
                )
                roomListSharedActionViewModel.post(RoomListSharedAction.CloseBottomSheet)
            }
        }
    }

    private val postPermissionLauncher = registerForPermissionsResult { _, _ ->
        // Nothing to do with the result.
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
        override fun onDrawerOpened(drawerView: View) {
            analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.Sidebar))
        }

        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()
        }
    }

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getBinding() = ActivityHomeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNewAppLayoutEnabled = vectorPreferences.isNewAppLayoutEnabled()
        analyticsScreenName = MobileScreen.ScreenName.Home
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
        sharedActionViewModel = viewModelProvider[HomeSharedActionViewModel::class.java]
        roomListSharedActionViewModel = viewModelProvider[RoomListSharedActionViewModel::class.java]
        views.drawerLayout.addDrawerListener(drawerListener)
        if (isFirstCreation()) {
            if (vectorPreferences.isNewAppLayoutEnabled()) {
                views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                replaceFragment(views.homeDetailFragmentContainer, NewHomeDetailFragment::class.java)
            } else {
                replaceFragment(views.homeDetailFragmentContainer, HomeDetailFragment::class.java)
                replaceFragment(views.homeDrawerFragmentContainer, HomeDrawerFragment::class.java)
                views.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        is HomeActivitySharedAction.OpenDrawer -> views.drawerLayout.openDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.CloseDrawer -> views.drawerLayout.closeDrawer(GravityCompat.START)
                        is HomeActivitySharedAction.OpenSpacePreview -> startActivity(SpacePreviewActivity.newIntent(this, sharedAction.spaceId))
                        is HomeActivitySharedAction.AddSpace -> createSpaceResultLauncher.launch(SpaceCreationActivity.newIntent(this))
                        is HomeActivitySharedAction.ShowSpaceSettings -> showSpaceSettings(sharedAction.spaceId)
                        is HomeActivitySharedAction.OpenSpaceInvite -> openSpaceInvite(sharedAction.spaceId)
                        HomeActivitySharedAction.SendSpaceFeedBack -> bugReporter.openBugReportScreen(this, ReportType.SPACE_BETA_FEEDBACK)
                        HomeActivitySharedAction.OnCloseSpace -> onCloseSpace()
                    }
                }
                .launchIn(lifecycleScope)

        val args = intent.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)

        if (args?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (args?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(args.inviteNotificationRoomId)?.let {
                navigator.openMatrixToBottomSheet(this, it, OriginOfMatrixTo.NOTIFICATION)
            }
        }

        homeActivityViewModel.observeViewEvents {
            when (it) {
                is HomeActivityViewEvents.AskPasswordToInitCrossSigning -> handleAskPasswordToInitCrossSigning(it)
                is HomeActivityViewEvents.CurrentSessionNotVerified -> handleOnNewSession(it)
                is HomeActivityViewEvents.CurrentSessionCannotBeVerified -> handleCantVerify(it)
                HomeActivityViewEvents.PromptToEnableSessionPush -> handlePromptToEnablePush()
                HomeActivityViewEvents.StartRecoverySetupFlow -> handleStartRecoverySetup()
                is HomeActivityViewEvents.ForceVerification -> {
                    navigator.requestSelfSessionVerification(this)
                }
                is HomeActivityViewEvents.OnCrossSignedInvalidated -> handleCrossSigningInvalidated(it)
                HomeActivityViewEvents.ShowAnalyticsOptIn -> handleShowAnalyticsOptIn()
                HomeActivityViewEvents.ShowNotificationDialog -> handleShowNotificationDialog()
                HomeActivityViewEvents.ShowReleaseNotes -> handleShowReleaseNotes()
                HomeActivityViewEvents.NotifyUserForThreadsMigration -> handleNotifyUserForThreadsMigration()
                is HomeActivityViewEvents.MigrateThreads -> migrateThreadsIfNeeded(it.checkSession)
                is HomeActivityViewEvents.AskUserForPushDistributor -> askUserToSelectPushDistributor()
            }
        }
        homeActivityViewModel.onEach { renderState(it) }

        shortcutsHandler.observeRoomsAndBuildShortcuts(lifecycleScope)

        if (isFirstCreation()) {
            handleIntent(intent)
        }
        homeActivityViewModel.handle(HomeActivityViewActions.ViewStarted)
    }

    private fun askUserToSelectPushDistributor() {
        unifiedPushHelper.showSelectDistributorDialog(this) { selection ->
            homeActivityViewModel.handle(HomeActivityViewActions.RegisterPushDistributor(selection))
        }
    }

    private fun handleShowNotificationDialog() {
        notificationPermissionManager.eventuallyRequestPermission(this, postPermissionLauncher)
    }

    private fun handleShowReleaseNotes() {
        startActivity(Intent(this, ReleaseNotesActivity::class.java))
    }

    private fun showSpaceSettings(spaceId: String) {
        // open bottom sheet
        SpaceSettingsMenuBottomSheet
                .newInstance(spaceId, object : SpaceSettingsMenuBottomSheet.InteractionListener {
                    override fun onShareSpaceSelected(spaceId: String) {
                        ShareSpaceBottomSheet.show(supportFragmentManager, spaceId)
                    }
                })
                .show(supportFragmentManager, "SPACE_SETTINGS")
    }

    private fun showLayoutSettings() {
        HomeLayoutSettingBottomDialogFragment()
                .show(supportFragmentManager, "LAYOUT_SETTINGS")
    }

    private fun openSpaceInvite(spaceId: String) {
        SpaceInviteBottomSheet.newInstance(spaceId)
                .show(supportFragmentManager, "SPACE_INVITE")
    }

    private fun onCloseSpace() {
        views.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun handleShowAnalyticsOptIn() {
        navigator.openAnalyticsOptIn(this)
    }

    /**
     * Migrating from old threads io.element.thread to new m.thread needs an initial sync to
     * sync and display existing messages appropriately.
     */
    private fun migrateThreadsIfNeeded(checkSession: Boolean) {
        if (checkSession) {
            // We should check session to ensure we will only clear cache if needed
            val args = intent.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)
            if (args?.hasExistingSession == true) {
                // existingSession --> Will be true only if we came from an existing active session
                Timber.i("----> Migrating threads from an existing session..")
                handleThreadsMigration()
            } else {
                // We came from a new session and not an existing one,
                // so there is no need to migrate threads while an initial synced performed
                Timber.i("----> No thread migration needed, we are ok")
                vectorPreferences.setShouldMigrateThreads(shouldMigrate = false)
            }
        } else {
            // Proceed with migration
            handleThreadsMigration()
        }
    }

    /**
     * Clear cache and restart to invoke an initial sync for threads migration.
     */
    private fun handleThreadsMigration() {
        Timber.i("----> Threads Migration detected, clearing cache and sync...")
        vectorPreferences.setShouldMigrateThreads(shouldMigrate = false)
        MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
    }

    private fun handleNotifyUserForThreadsMigration() {
        MaterialAlertDialogBuilder(this)
                .setTitle(CommonStrings.threads_notice_migration_title)
                .setMessage(CommonStrings.threads_notice_migration_message)
                .setCancelable(true)
                .setPositiveButton(CommonStrings.sas_got_it) { _, _ -> }
                .show()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.let { deepLink ->
            val resolvedLink = when {
                // Element custom scheme is not handled by the sdk, convert it to matrix.to link for compatibility
                deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE) -> {
                    when {
                        deepLink.startsWith(USER_LINK_PREFIX) -> deepLink.substring(USER_LINK_PREFIX.length)
                        deepLink.startsWith(ROOM_LINK_PREFIX) -> deepLink.substring(ROOM_LINK_PREFIX.length)
                        else -> null
                    }?.let { permalinkId ->
                        activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(permalinkId)
                    }
                }
                else -> deepLink
            }

            lifecycleScope.launch {
                val isHandled = permalinkHandler.launch(
                        fragmentActivity = this@HomeActivity,
                        deepLink = resolvedLink,
                        navigationInterceptor = this@HomeActivity,
                        buildTask = true
                )
                if (!isHandled) {
                    val isMatrixToLink = deepLink.startsWith(PermalinkService.MATRIX_TO_URL_BASE) ||
                            deepLink.startsWith(MATRIX_TO_CUSTOM_SCHEME_URL_BASE)
                    MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle(CommonStrings.dialog_title_error)
                            .setMessage(if (isMatrixToLink) CommonStrings.permalink_malformed else CommonStrings.universal_link_malformed)
                            .setPositiveButton(CommonStrings.ok, null)
                            .show()
                }
            }
        }
    }

    private fun handleStartRecoverySetup() {
        // To avoid IllegalStateException in case the transaction was executed after onSaveInstanceState
        lifecycleScope.launch {
            withResumed {
                navigator.open4SSetup(this@HomeActivity, SetupMode.NORMAL)
            }
        }
    }

    private fun renderState(state: HomeActivityViewState) {
        when (val status = state.syncRequestState) {
            is SyncRequestState.InitialSyncProgressing -> {
                val initSyncStepStr = initSyncStepFormatter.format(status.initialSyncStep)
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
            else -> {
                // Idle or Incremental sync status
                views.waitingView.root.isVisible = false
            }
        }
    }

    private fun handleAskPasswordToInitCrossSigning(events: HomeActivityViewEvents.AskPasswordToInitCrossSigning) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.UPGRADE_SECURITY_UID,
                userItem = events.userItem,
                titleRes = CommonStrings.upgrade_security,
                descRes = CommonStrings.security_prompt_text,
        ) {
            it.navigator.upgradeSessionSecurity(it, true)
        }
    }

    private fun handleCrossSigningInvalidated(event: HomeActivityViewEvents.OnCrossSignedInvalidated) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.VERIFY_SESSION_UID,
                userItem = event.userItem,
                titleRes = CommonStrings.crosssigning_verify_this_session,
                descRes = CommonStrings.confirm_your_identity,
        ) {
            // check first if it's not an outdated request?
            activeSessionHolder.getSafeActiveSession()?.let { session ->
                session.coroutineScope.launch {
                    if (!session.cryptoService().crossSigningService().isCrossSigningVerified()) {
                        withContext(Dispatchers.Main) {
                            it.navigator.requestSelfSessionVerification(it)
                        }
                    }
                }
            }
        }
    }

    private fun handleOnNewSession(event: HomeActivityViewEvents.CurrentSessionNotVerified) {
        // We need to ask
        val titleRes = if (event.afterMigration) {
            CommonStrings.crosssigning_verify_after_update
        } else {
            CommonStrings.crosssigning_verify_this_session
        }
        val descRes = if (event.afterMigration) {
            CommonStrings.confirm_your_identity_after_update
        } else {
            CommonStrings.confirm_your_identity
        }
        promptSecurityEvent(
                uid = PopupAlertManager.VERIFY_SESSION_UID,
                userItem = event.userItem,
                titleRes = titleRes,
                descRes = descRes,
        ) {
            it.navigator.requestSelfSessionVerification(it)
        }
    }

    private fun handleCantVerify(event: HomeActivityViewEvents.CurrentSessionCannotBeVerified) {
        // We need to ask
        promptSecurityEvent(
                uid = PopupAlertManager.UPGRADE_SECURITY_UID,
                userItem = event.userItem,
                titleRes = CommonStrings.crosssigning_cannot_verify_this_session,
                descRes = CommonStrings.crosssigning_cannot_verify_this_session_desc,
        ) {
            it.navigator.open4SSetup(it, SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
        }
    }

    private fun handlePromptToEnablePush() {
        popupAlertManager.postVectorAlert(
                DefaultVectorAlert(
                        uid = PopupAlertManager.ENABLE_PUSH_UID,
                        title = getString(CommonStrings.alert_push_are_disabled_title),
                        description = getString(CommonStrings.alert_push_are_disabled_description),
                        iconId = R.drawable.ic_room_actions_notifications_mutes,
                        shouldBeDisplayedIn = {
                            it is HomeActivity
                        }
                ).apply {
                    colorInt = ThemeUtils.getColor(this@HomeActivity, im.vector.lib.ui.styles.R.attr.vctr_notice_secondary)
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
                    addButton(getString(CommonStrings.action_dismiss), {
                        homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                    }, true)
                    addButton(getString(CommonStrings.settings), {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            // action(it)
                            homeActivityViewModel.handle(HomeActivityViewActions.PushPromptHasBeenReviewed)
                            it.navigator.openSettings(it, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
                        }
                    }, true)
                }
        )
    }

    private fun promptSecurityEvent(
            uid: String,
            userItem: MatrixItem.UserItem,
            titleRes: Int,
            descRes: Int,
            action: ((VectorBaseActivity<*>) -> Unit),
    ) {
        popupAlertManager.postVectorAlert(
                VerificationVectorAlert(
                        uid = uid,
                        title = getString(titleRes),
                        description = getString(descRes),
                        iconId = R.drawable.ic_shield_warning
                ).apply {
                    viewBinder = VerificationVectorAlert.ViewBinder(userItem, avatarRenderer)
                    colorInt = ThemeUtils.getColor(this@HomeActivity, com.google.android.material.R.attr.colorPrimary)
                    contentAction = Runnable {
                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                            action(it)
                        }
                    }
                    dismissedAction = Runnable {}
                }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val parcelableExtra = intent.getParcelableExtraCompat<HomeActivityArgs>(Mavericks.KEY_ARG)
        if (parcelableExtra?.clearNotification == true) {
            notificationDrawerManager.clearAllEvents()
        }
        if (parcelableExtra?.inviteNotificationRoomId != null) {
            activeSessionHolder.getSafeActiveSession()
                    ?.permalinkService()
                    ?.createPermalink(parcelableExtra.inviteNotificationRoomId)?.let {
                        navigator.openMatrixToBottomSheet(this, it, OriginOfMatrixTo.NOTIFICATION)
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

        if (vectorUncaughtExceptionHandler.didAppCrash()) {
            vectorUncaughtExceptionHandler.clearAppCrashStatus()

            MaterialAlertDialogBuilder(this)
                    .setMessage(CommonStrings.send_bug_report_app_crashed)
                    .setCancelable(false)
                    .setPositiveButton(CommonStrings.yes) { _, _ -> bugReporter.openBugReportScreen(this) }
                    .setNegativeButton(CommonStrings.no) { _, _ -> bugReporter.deleteCrashFile() }
                    .show()
        }

        // Force remote backup state update to update the banner if needed
        serverBackupStatusViewModel.refreshRemoteStateIfNeeded()

        // Check nightly
        if (nightlyProxy.canDisplayPopup()) {
            nightlyProxy.updateApplication()
        }

        checkNewAppLayoutFlagChange()
    }

    private fun checkNewAppLayoutFlagChange() {
        if (vectorPreferences.isNewAppLayoutEnabled() != isNewAppLayoutEnabled) {
            restart()
        }
    }

    override fun getMenuRes() = if (vectorPreferences.isNewAppLayoutEnabled()) R.menu.menu_new_home else R.menu.menu_home

    override fun handlePrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_home_init_sync_legacy).isVisible = vectorPreferences.developerMode()
        menu.findItem(R.id.menu_home_init_sync_optimized).isVisible = vectorPreferences.developerMode()
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home_suggestion -> {
                bugReporter.openBugReportScreen(this, ReportType.SUGGESTION)
                true
            }
            R.id.menu_home_report_bug -> {
                bugReporter.openBugReportScreen(this, ReportType.BUG_REPORT)
                true
            }
            R.id.menu_home_init_sync_legacy -> {
                // Configure the SDK
                initialSyncStrategy = InitialSyncStrategy.Legacy
                // And clear cache
                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
                true
            }
            R.id.menu_home_init_sync_optimized -> {
                // Configure the SDK
                initialSyncStrategy = InitialSyncStrategy.Optimized()
                // And clear cache
                MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
                true
            }
            R.id.menu_home_filter -> {
                navigator.openRoomsFiltering(this)
                true
            }
            R.id.menu_home_setting -> {
                navigator.openSettings(this)
                true
            }
            R.id.menu_home_layout_settings -> {
                showLayoutSettings()
                true
            }
            R.id.menu_home_invite_friends -> {
                launchInviteFriends()
                true
            }
            R.id.menu_home_qr -> {
                launchQrCode()
                true
            }
            else -> false
        }
    }

    private fun launchQrCode() {
        startActivity(UserCodeActivity.newIntent(this, sharedActionViewModel.session.myUserId))
    }

    private fun launchInviteFriends() {
        activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createPermalink(sharedActionViewModel.session.myUserId)?.let { permalink ->
            analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.InviteFriends))
            val text = getString(CommonStrings.invite_friends_text, permalink)

            startSharePlainTextIntent(
                    context = this,
                    activityResultLauncher = null,
                    chooserTitle = getString(CommonStrings.invite_friends),
                    text = text,
                    extraTitle = getString(CommonStrings.invite_friends_rich_title)
            )
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (views.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            views.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            validateBackPressed {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        // TODO check if there is already one??
        MatrixToBottomSheet.withLink(deepLink.toString(), OriginOfMatrixTo.LINK)
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri?, rootThreadEventId: String?): Boolean {
        if (roomId == null) return false
        MatrixToBottomSheet.withLink(deepLink.toString(), OriginOfMatrixTo.LINK)
                .show(supportFragmentManager, "HA#MatrixToBottomSheet")
        return true
    }

    override fun spaceInviteBottomSheetOnAccept(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.OpenRoomList)
    }

    override fun spaceInviteBottomSheetOnDecline(spaceId: String) {
        // nop
    }

    companion object {
        fun newIntent(
                context: Context,
                firstStartMainActivity: Boolean,
                clearNotification: Boolean = false,
                authenticationDescription: AuthenticationDescription? = null,
                existingSession: Boolean = false,
                inviteNotificationRoomId: String? = null
        ): Intent {
            val args = HomeActivityArgs(
                    clearNotification = clearNotification,
                    authenticationDescription = authenticationDescription,
                    hasExistingSession = existingSession,
                    inviteNotificationRoomId = inviteNotificationRoomId
            )

            val intent = Intent(context, HomeActivity::class.java)
                    .apply {
                        putExtra(Mavericks.KEY_ARG, args)
                    }

            return if (firstStartMainActivity) {
                MainActivity.getIntentWithNextIntent(context, intent)
            } else {
                intent
            }
        }
    }

    override fun mxToBottomSheetNavigateToRoom(roomId: String, trigger: ViewRoom.Trigger?) {
        navigator.openRoom(this, roomId, trigger = trigger)
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.OpenRoomList)
    }
}
