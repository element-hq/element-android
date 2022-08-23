/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home

import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsType
import im.vector.app.features.analytics.plan.Signup
import im.vector.app.features.analytics.store.AnalyticsStore
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.onboarding.AuthenticationDescription
import im.vector.app.features.raw.wellknown.ElementWellKnown
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isSecureBackupRequired
import im.vector.app.features.raw.wellknown.withElementWellKnown
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HomeActivityViewModel @AssistedInject constructor(
        @Assisted private val initialState: HomeActivityViewState,
        private val activeSessionHolder: ActiveSessionHolder,
        private val rawService: RawService,
        private val reAuthHelper: ReAuthHelper,
        private val analyticsStore: AnalyticsStore,
        private val lightweightSettingsStorage: LightweightSettingsStorage,
        private val vectorPreferences: VectorPreferences,
        private val analyticsTracker: AnalyticsTracker,
        private val analyticsConfig: AnalyticsConfig,
) : VectorViewModel<HomeActivityViewState, HomeActivityViewActions, HomeActivityViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<HomeActivityViewModel, HomeActivityViewState> {
        override fun create(initialState: HomeActivityViewState): HomeActivityViewModel
    }

    companion object : MavericksViewModelFactory<HomeActivityViewModel, HomeActivityViewState> by hiltMavericksViewModelFactory() {
        override fun initialState(viewModelContext: ViewModelContext): HomeActivityViewState? {
            val activity: HomeActivity = viewModelContext.activity()
            val args: HomeActivityArgs? = activity.intent.getParcelableExtra(Mavericks.KEY_ARG)
            return args?.let { HomeActivityViewState(authenticationDescription = it.authenticationDescription) }
                    ?: super.initialState(viewModelContext)
        }
    }

    private var isInitialized = false
    private var hasCheckedBootstrap = false
    private var onceTrusted = false

    private fun initialize() {
        if (isInitialized) return
        isInitialized = true
        cleanupFiles()
        observeInitialSync()
        checkSessionPushIsOn()
        observeCrossSigningReset()
        observeAnalytics()
        initThreadsMigration()
    }

    private fun observeAnalytics() {
        if (analyticsConfig.isEnabled) {
            analyticsStore.didAskUserConsentFlow
                    .onEach { didAskUser ->
                        if (!didAskUser) {
                            _viewEvents.post(HomeActivityViewEvents.ShowAnalyticsOptIn)
                        }
                    }
                    .launchIn(viewModelScope)

            when (val recentAuthentication = initialState.authenticationDescription) {
                is AuthenticationDescription.Register -> {
                    viewModelScope.launch {
                        analyticsStore.onUserGaveConsent {
                            analyticsTracker.capture(Signup(authenticationType = recentAuthentication.type.toAnalyticsType()))
                        }
                    }
                }
                AuthenticationDescription.Login -> {
                    // do nothing
                }
                null -> {
                    // do nothing
                }
            }
        }
    }

    private suspend fun AnalyticsStore.onUserGaveConsent(action: () -> Unit) {
        userConsentFlow
                .takeWhile { !it }
                .onCompletion { action() }
                .collect()
    }

    private fun cleanupFiles() {
        // Mitigation: delete all cached decrypted files each time the application is started.
        activeSessionHolder.getSafeActiveSession()?.fileService()?.clearDecryptedCache()
    }

    private fun observeCrossSigningReset() {
        val safeActiveSession = activeSessionHolder.getSafeActiveSession() ?: return

        onceTrusted = safeActiveSession
                .cryptoService()
                .crossSigningService().allPrivateKeysKnown()

        safeActiveSession
                .flow()
                .liveCrossSigningInfo(safeActiveSession.myUserId)
                .onEach { info ->
                    val isVerified = info.getOrNull()?.isTrusted() ?: false
                    if (!isVerified && onceTrusted) {
                        rawService.withElementWellKnown(viewModelScope, safeActiveSession.sessionParams) {
                            sessionHasBeenUnverified(it)
                        }
                    }
                    onceTrusted = isVerified
                }
                .launchIn(viewModelScope)
    }

    /**
     * Handle threads migration. The migration includes:
     * - Notify users that had io.element.thread enabled from labs
     * - Re-Enable m.thread to those users (that they had enabled labs threads)
     * - Handle migration when threads are enabled by default
     */
    private fun initThreadsMigration() {
        // When we would like to enable threads for all users
//        if(vectorPreferences.shouldMigrateThreads()) {
//            vectorPreferences.setThreadMessagesEnabled()
//            lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
//        }

        when {
            // Notify users
            vectorPreferences.shouldNotifyUserAboutThreads() && vectorPreferences.areThreadMessagesEnabled() -> {
                Timber.i("----> Notify users about threads")
                // Notify the user if needed that we migrated to support m.thread
                // instead of io.element.thread so old thread messages will be displayed as normal timeline messages
                _viewEvents.post(HomeActivityViewEvents.NotifyUserForThreadsMigration)
                vectorPreferences.userNotifiedAboutThreads()
            }
            // Migrate users with enabled lab settings
            vectorPreferences.shouldNotifyUserAboutThreads() && vectorPreferences.shouldMigrateThreads() -> {
                Timber.i("----> Migrate threads with enabled labs")
                // If user had io.element.thread enabled then enable the new thread support,
                // clear cache to sync messages appropriately
                vectorPreferences.setThreadMessagesEnabled()
                lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
                // Clear Cache
                _viewEvents.post(HomeActivityViewEvents.MigrateThreads(checkSession = false))
            }
            // Enable all users
            vectorPreferences.shouldMigrateThreads() && vectorPreferences.areThreadMessagesEnabled() -> {
                Timber.i("----> Try to migrate threads")
                _viewEvents.post(HomeActivityViewEvents.MigrateThreads(checkSession = true))
            }
        }
    }

    private fun observeInitialSync() {
        val session = activeSessionHolder.getSafeActiveSession() ?: return

        session.syncService().getSyncRequestStateFlow()
                .onEach { status ->
                    when (status) {
                        is SyncRequestState.Idle -> {
                            maybeVerifyOrBootstrapCrossSigning()
                        }
                        else -> Unit
                    }

                    setState {
                        copy(
                                syncRequestState = status
                        )
                    }
                }
                .launchIn(viewModelScope)

        if (session.syncService().hasAlreadySynced()) {
            maybeVerifyOrBootstrapCrossSigning()
        }
    }

    /**
     * After migration from riot to element some users reported that their
     * push setting for the session was set to off.
     * In order to mitigate this, we want to display a popup once to the user
     * giving him the option to review this setting.
     */
    private fun checkSessionPushIsOn() {
        viewModelScope.launch(Dispatchers.IO) {
            // Don't do that if it's a login or a register (pass in memory)
            if (reAuthHelper.data != null) return@launch
            // Check if disabled for this device
            if (!vectorPreferences.areNotificationEnabledForDevice()) {
                // Check if set at account level
                val mRuleMaster = activeSessionHolder.getSafeActiveSession()
                        ?.pushRuleService()
                        ?.getPushRules()
                        ?.getAllRules()
                        ?.find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }
                if (mRuleMaster?.enabled == false) {
                    // So push are enabled at account level but not for this session
                    // Let's check that there are some rooms?
                    val knownRooms = activeSessionHolder.getSafeActiveSession()
                            ?.roomService()
                            ?.getRoomSummaries(roomSummaryQueryParams {
                                memberships = Membership.activeMemberships()
                            })?.size ?: 0

                    // Prompt once to the user
                    if (knownRooms > 1 && !vectorPreferences.didAskUserToEnableSessionPush()) {
                        // delay a bit
                        delay(1500)
                        _viewEvents.post(HomeActivityViewEvents.PromptToEnableSessionPush)
                    }
                }
            }
        }
    }

    private fun sessionHasBeenUnverified(elementWellKnown: ElementWellKnown?) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val isSecureBackupRequired = elementWellKnown?.isSecureBackupRequired() ?: false
        if (isSecureBackupRequired) {
            // If 4S is forced, force verification
            // for stability cancel all pending verifications?
            session.cryptoService().verificationService().getExistingVerificationRequests(session.myUserId).forEach {
                session.cryptoService().verificationService().cancelVerificationRequest(it)
            }
            _viewEvents.post(HomeActivityViewEvents.ForceVerification(false))
        } else {
            // cross signing keys have been reset
            // Trigger a popup to re-verify
            // Note: user can be null in case of logout
            session.getUser(session.myUserId)
                    ?.toMatrixItem()
                    ?.let { user ->
                        _viewEvents.post(HomeActivityViewEvents.OnCrossSignedInvalidated(user))
                    }
        }
    }

    private fun maybeVerifyOrBootstrapCrossSigning() {
        // The contents of this method should only run once
        if (hasCheckedBootstrap) return
        hasCheckedBootstrap = true

        // We do not use the viewModel context because we do not want to tie this action to activity view model
        activeSessionHolder.getSafeActiveSession()?.coroutineScope?.launch(Dispatchers.IO) {
            val session = activeSessionHolder.getSafeActiveSession() ?: return@launch Unit.also {
                Timber.w("## No session to init cross signing or bootstrap")
            }

            val elementWellKnown = rawService.getElementWellknown(session.sessionParams)
            val isSecureBackupRequired = elementWellKnown?.isSecureBackupRequired() ?: false

            // In case of account creation, it is already done before
            if (initialState.authenticationDescription is AuthenticationDescription.Register) {
                if (isSecureBackupRequired) {
                    _viewEvents.post(HomeActivityViewEvents.StartRecoverySetupFlow)
                } else {
                    val password = reAuthHelper.data ?: return@launch Unit.also {
                        Timber.w("No password to init cross signing")
                    }

                    // Silently initialize cross signing without 4S
                    // We do not use the viewModel context because we do not want to cancel this action
                    Timber.d("Initialize cross signing")
                    try {
                        session.cryptoService().crossSigningService().awaitCrossSigninInitialization { response, _ ->
                            resume(
                                    UserPasswordAuth(
                                            session = response.session,
                                            user = session.myUserId,
                                            password = password
                                    )
                            )
                        }
                    } catch (failure: Throwable) {
                        Timber.e(failure, "Failed to initialize cross signing")
                    }
                }
                return@launch
            }

            tryOrNull("## MaybeVerifyOrBootstrapCrossSigning: Failed to download keys") {
                awaitCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
                    session.cryptoService().downloadKeys(listOf(session.myUserId), true, it)
                }
            }

            // From there we are up to date with server
            // Is there already cross signing keys here?
            val mxCrossSigningInfo = session.cryptoService().crossSigningService().getMyCrossSigningKeys()
            if (mxCrossSigningInfo != null) {
                if (isSecureBackupRequired && !session.sharedSecretStorageService().isRecoverySetup()) {
                    // If 4S is forced, start the full interactive setup flow
                    _viewEvents.post(HomeActivityViewEvents.StartRecoverySetupFlow)
                } else {
                    // Cross-signing is already set up for this user, is it trusted?
                    if (!mxCrossSigningInfo.isTrusted()) {
                        if (isSecureBackupRequired) {
                            // If 4S is forced, force verification
                            _viewEvents.post(HomeActivityViewEvents.ForceVerification(true))
                        } else {
                            // we wan't to check if there is a way to actually verify this session,
                            // that means that there is another session to verify against, or
                            // secure backup is setup
                            val hasTargetDeviceToVerifyAgainst = session
                                    .cryptoService()
                                    .getUserDevices(session.myUserId)
                                    .size >= 2 // this one + another
                            val is4Ssetup = session.sharedSecretStorageService().isRecoverySetup()
                            if (hasTargetDeviceToVerifyAgainst || is4Ssetup) {
                                // New session
                                _viewEvents.post(
                                        HomeActivityViewEvents.CurrentSessionNotVerified(
                                                session.getUser(session.myUserId)?.toMatrixItem(),
                                                // Always send request instead of waiting for an incoming as per recent EW changes
                                                false
                                        )
                                )
                            } else {
                                _viewEvents.post(
                                        HomeActivityViewEvents.CurrentSessionCannotBeVerified(
                                                session.getUser(session.myUserId)?.toMatrixItem(),
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                // Cross signing is not initialized
                if (isSecureBackupRequired) {
                    // If 4S is forced, start the full interactive setup flow
                    _viewEvents.post(HomeActivityViewEvents.StartRecoverySetupFlow)
                } else {
                    // Initialize cross-signing silently
                    val password = reAuthHelper.data

                    if (password == null) {
                        // Check this is not an SSO account
                        if (session.homeServerCapabilitiesService().getHomeServerCapabilities().canChangePassword) {
                            // Ask password to the user: Upgrade security
                            _viewEvents.post(HomeActivityViewEvents.AskPasswordToInitCrossSigning(session.getUser(session.myUserId)?.toMatrixItem()))
                        }
                        // Else (SSO) just ignore for the moment
                    } else {
                        // Try to initialize cross signing in background if possible
                        Timber.d("Initialize cross signing...")
                        try {
                            session.cryptoService().crossSigningService().awaitCrossSigninInitialization { response, errCode ->
                                // We missed server grace period or it's not setup, see if we remember locally password
                                if (response.nextUncompletedStage() == LoginFlowTypes.PASSWORD &&
                                        errCode == null &&
                                        reAuthHelper.data != null) {
                                    resume(
                                            UserPasswordAuth(
                                                    session = response.session,
                                                    user = session.myUserId,
                                                    password = reAuthHelper.data
                                            )
                                    )
                                    Timber.d("Initialize cross signing SUCCESS")
                                } else {
                                    resumeWithException(Exception("Cannot silently initialize cross signing, UIA missing"))
                                }
                            }
                        } catch (failure: Throwable) {
                            Timber.e(failure, "Failed to initialize cross signing")
                        }
                    }
                }
            }
        }
    }

    override fun handle(action: HomeActivityViewActions) {
        when (action) {
            HomeActivityViewActions.PushPromptHasBeenReviewed -> {
                vectorPreferences.setDidAskUserToEnableSessionPush()
            }
            HomeActivityViewActions.ViewStarted -> {
                initialize()
            }
        }
    }
}

private suspend fun CrossSigningService.awaitCrossSigninInitialization(
        block: Continuation<UIABaseAuth>.(response: RegistrationFlowResponse, errCode: String?) -> Unit
) {
    awaitCallback<Unit> {
        initializeCrossSigning(
                object : UserInteractiveAuthInterceptor {
                    override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                        promise.block(flowResponse, errCode)
                    }
                },
                callback = it
        )
    }
}
