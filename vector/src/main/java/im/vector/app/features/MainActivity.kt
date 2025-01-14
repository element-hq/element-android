/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.viewModel
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.extensions.vectorStore
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.deleteAllFiles
import im.vector.app.databinding.ActivityMainBinding
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.ShortcutsHandler
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.threads.ThreadsActivity
import im.vector.app.features.location.live.map.LiveLocationMapViewActivity
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.pin.UnlockedActivity
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import im.vector.app.features.pin.lockscreen.pincode.PinCodeHelper
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.session.VectorSessionStore
import im.vector.app.features.signout.hard.SignedOutActivity
import im.vector.app.features.start.StartAppAction
import im.vector.app.features.start.StartAppAndroidService
import im.vector.app.features.start.StartAppViewEvent
import im.vector.app.features.start.StartAppViewModel
import im.vector.app.features.start.StartAppViewState
import im.vector.app.features.themes.ActivityOtherThemes
import im.vector.app.features.ui.UiStateRepository
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class MainActivityArgs(
        val clearCache: Boolean = false,
        val clearCredentials: Boolean = false,
        val isUserLoggedOut: Boolean = false,
        val isAccountDeactivated: Boolean = false,
        val isSoftLogout: Boolean = false
) : Parcelable

/**
 * This is the entry point of Element Android.
 * This Activity, when started with argument, is also doing some cleanup when user signs out,
 * clears cache, is logged out, or is soft logged out.
 */
@AndroidEntryPoint
class MainActivity : VectorBaseActivity<ActivityMainBinding>(), UnlockedActivity {

    companion object {
        private const val EXTRA_ARGS = "EXTRA_ARGS"
        private const val EXTRA_NEXT_INTENT = "EXTRA_NEXT_INTENT"
        private const val EXTRA_INIT_SESSION = "EXTRA_INIT_SESSION"
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val ACTION_ROOM_DETAILS_FROM_SHORTCUT = "ROOM_DETAILS_FROM_SHORTCUT"

        // Special action to clear cache and/or clear credentials
        fun restartApp(activity: Activity, args: MainActivityArgs) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            intent.putExtra(EXTRA_ARGS, args)
            activity.startActivity(intent)
        }

        fun getIntentToInitSession(activity: Activity): Intent {
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra(EXTRA_INIT_SESSION, true)
            return intent
        }

        fun getIntentWithNextIntent(context: Context, nextIntent: Intent): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_NEXT_INTENT, nextIntent)
            return intent
        }

        // Shortcuts can't have intents with parcelables
        fun shortcutIntent(context: Context, roomId: String): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = ACTION_ROOM_DETAILS_FROM_SHORTCUT
                putExtra(EXTRA_ROOM_ID, roomId)
            }
        }

        val allowList = listOf(
                HomeActivity::class.java.name,
                MainActivity::class.java.name,
                RoomDetailActivity::class.java.name,
                ThreadsActivity::class.java.name,
                LiveLocationMapViewActivity::class.java.name,
        )
    }

    private val startAppViewModel: StartAppViewModel by viewModel()

    override fun getBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun getOtherThemes() = ActivityOtherThemes.Launcher

    private lateinit var args: MainActivityArgs

    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var uiStateRepository: UiStateRepository
    @Inject lateinit var shortcutsHandler: ShortcutsHandler
    @Inject lateinit var pinCodeHelper: PinCodeHelper
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var vectorAnalytics: VectorAnalytics
    @Inject lateinit var lockScreenKeyRepository: LockScreenKeyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shortcutsHandler.updateShortcutsWithPreviousIntent()

        startAppViewModel.onEach {
            renderState(it)
        }
        startAppViewModel.observeViewEvents {
            handleViewEvents(it)
        }

        startAppViewModel.handle(StartAppAction.StartApp)
    }

    private fun renderState(state: StartAppViewState) {
        if (state.mayBeLongToProcess) {
            views.status.setText(CommonStrings.updating_your_data)
        }
        views.status.isVisible = state.mayBeLongToProcess
    }

    private fun handleViewEvents(event: StartAppViewEvent) {
        when (event) {
            StartAppViewEvent.StartForegroundService -> handleStartForegroundService()
            StartAppViewEvent.AppStarted -> handleAppStarted()
        }
    }

    private fun handleStartForegroundService() {
        if (startAppViewModel.shouldStartApp()) {
            // Start foreground service, because the operation may take a while
            val intent = Intent(this, StartAppAndroidService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun handleAppStarted() {
        // On the first run with rust crypto this would be false
        if (!vectorPreferences.isOnRustCrypto()) {
            if (activeSessionHolder.hasActiveSession()) {
                vectorPreferences.setHadExistingLegacyData(activeSessionHolder.getActiveSession().isOpenable)
            } else {
                vectorPreferences.setHadExistingLegacyData(false)
            }
        }

        vectorPreferences.setIsOnRustCrypto(true)

        if (intent.hasExtra(EXTRA_NEXT_INTENT)) {
            // Start the next Activity
            startSyncing()
            val nextIntent = intent.getParcelableExtraCompat<Intent>(EXTRA_NEXT_INTENT)
                    ?.takeIf { it.isValid() }
            startIntentAndFinish(nextIntent)
        } else if (intent.hasExtra(EXTRA_INIT_SESSION)) {
            startSyncing()
            setResult(RESULT_OK)
            finish()
        } else if (intent.action == ACTION_ROOM_DETAILS_FROM_SHORTCUT) {
            startSyncing()
            val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
            if (roomId?.isNotEmpty() == true) {
                navigator.openRoom(this, roomId, trigger = ViewRoom.Trigger.Shortcut)
            }
            finish()
        } else {
            args = parseArgs()
            if (args.clearCredentials || args.isUserLoggedOut || args.clearCache) {
                clearNotifications()
            }
            // Handle some wanted cleanup
            if (args.clearCache || args.clearCredentials) {
                doCleanUp()
            } else {
                startSyncing()
                startNextActivityAndFinish()
            }
        }
    }

    private fun startSyncing() {
        activeSessionHolder.getSafeActiveSession()?.startSyncing(this)
    }

    private fun clearNotifications() {
        // Dismiss all notifications
        notificationDrawerManager.clearAllEvents()

        // Also clear the dynamic shortcuts
        shortcutsHandler.clearShortcuts()

        // Also clear the alerts
        popupAlertManager.cancelAll()
    }

    private fun parseArgs(): MainActivityArgs {
        val argsFromIntent: MainActivityArgs? = intent.getParcelableExtraCompat(EXTRA_ARGS)
        Timber.w("Starting MainActivity with $argsFromIntent")

        return MainActivityArgs(
                clearCache = argsFromIntent?.clearCache ?: false,
                clearCredentials = argsFromIntent?.clearCredentials ?: false,
                isUserLoggedOut = argsFromIntent?.isUserLoggedOut ?: false,
                isAccountDeactivated = argsFromIntent?.isAccountDeactivated ?: false,
                isSoftLogout = argsFromIntent?.isSoftLogout ?: false
        )
    }

    private fun doCleanUp() {
        val session = activeSessionHolder.getSafeActiveSession()
        if (session == null) {
            startNextActivityAndFinish()
            return
        }

        val onboardingStore = session.vectorStore(this)
        when {
            args.isAccountDeactivated -> {
                lifecycleScope.launch {
                    // Just do the local cleanup
                    Timber.w("Account deactivated, start app")
                    activeSessionHolder.clearActiveSession()
                    doLocalCleanup(clearPreferences = true, onboardingStore)
                    startNextActivityAndFinish()
                }
            }
            args.clearCredentials -> {
                signout(session, onboardingStore, ignoreServerError = false)
            }
            args.clearCache -> {
                lifecycleScope.launch {
                    session.clearCache()
                    doLocalCleanup(clearPreferences = false, onboardingStore)
                    session.startSyncing(applicationContext)
                    startNextActivityAndFinish()
                }
            }
        }
    }

    private fun signout(
            session: Session,
            onboardingStore: VectorSessionStore,
            ignoreServerError: Boolean,
    ) {
        lifecycleScope.launch {
            try {
                session.signOutService().signOut(!args.isUserLoggedOut, ignoreServerError)
            } catch (failure: Throwable) {
                Timber.e(failure, "SIGN_OUT: error, propose to sign out anyway")
                displaySignOutFailedDialog(session, onboardingStore)
                return@launch
            }
            Timber.w("SIGN_OUT: success, start app")
            activeSessionHolder.clearActiveSession()
            doLocalCleanup(clearPreferences = true, onboardingStore)
            startNextActivityAndFinish()
        }
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        // No op here
        Timber.w("Ignoring invalid token global error")
    }

    private suspend fun doLocalCleanup(clearPreferences: Boolean, vectorSessionStore: VectorSessionStore) {
        // On UI Thread
        Glide.get(this@MainActivity).clearMemory()

        if (clearPreferences) {
            vectorPreferences.clearPreferences()
            uiStateRepository.reset()
            pinLocker.unlock()
            pinCodeHelper.deletePinCode()
            vectorAnalytics.onSignOut()
            vectorSessionStore.clear()
            lockScreenKeyRepository.deleteSystemKey()
        }
        withContext(Dispatchers.IO) {
            // On BG thread
            Glide.get(this@MainActivity).clearDiskCache()

            // Also clear cache (Logs, etc...)
            deleteAllFiles(this@MainActivity.cacheDir)
        }
    }

    private fun displaySignOutFailedDialog(
            session: Session,
            onboardingStore: VectorSessionStore,
    ) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            MaterialAlertDialogBuilder(this)
                    .setTitle(CommonStrings.dialog_title_error)
                    .setMessage(CommonStrings.sign_out_failed_dialog_message)
                    .setPositiveButton(CommonStrings.sign_out_anyway) { _, _ ->
                        signout(session, onboardingStore, ignoreServerError = true)
                    }
                    .setNeutralButton(CommonStrings.global_retry) { _, _ ->
                        signout(session, onboardingStore, ignoreServerError = false)
                    }
                    .setNegativeButton(CommonStrings.action_cancel) { _, _ -> startNextActivityAndFinish(ignoreClearCredentials = true) }
                    .setCancelable(false)
                    .show()
        }
    }

    private fun startNextActivityAndFinish(ignoreClearCredentials: Boolean = false) {
        val intent = when {
            args.clearCredentials &&
                    !ignoreClearCredentials &&
                    (!args.isUserLoggedOut || args.isAccountDeactivated) -> {
                // User has explicitly asked to log out or deactivated his account
                navigator.openLogin(this, null)
                null
            }
            args.isSoftLogout -> {
                // The homeserver has invalidated the token, with a soft logout
                navigator.softLogout(this)
                null
            }
            args.isUserLoggedOut ->
                // the homeserver has invalidated the token (password changed, device deleted, other security reasons)
                SignedOutActivity.newIntent(this)
            activeSessionHolder.hasActiveSession() ->
                // We have a session.
                // Check it can be opened
                if (activeSessionHolder.getActiveSession().isOpenable) {
                    HomeActivity.newIntent(this, firstStartMainActivity = false, existingSession = true)
                } else {
                    // The token is still invalid
                    navigator.softLogout(this)
                    null
                }
            else -> {
                // First start, or no active session
                navigator.openLogin(this, null)
                null
            }
        }
        startIntentAndFinish(intent)
    }

    private fun startIntentAndFinish(intent: Intent?) {
        intent?.let { startActivity(it) }
        finish()
    }

    private fun Intent.isValid(): Boolean {
        val componentName = resolveActivity(packageManager) ?: return false
        val packageName = componentName.packageName
        val className = componentName.className
        return packageName == buildMeta.applicationId && className in allowList
    }
}
