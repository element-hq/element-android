/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.content.SharedPreferences
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.ToggleIpAddressVisibilityUseCase
import im.vector.app.features.settings.devices.v2.VectorSessionsListViewModel
import im.vector.app.features.settings.devices.v2.notification.GetNotificationsStatusUseCase
import im.vector.app.features.settings.devices.v2.notification.ToggleNotificationsUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionsReAuthNeeded
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionsUseCase
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import timber.log.Timber

class SessionOverviewViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionOverviewViewState,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
        private val checkIfCurrentSessionCanBeVerifiedUseCase: CheckIfCurrentSessionCanBeVerifiedUseCase,
        private val signoutSessionsUseCase: SignoutSessionsUseCase,
        private val pendingAuthHandler: PendingAuthHandler,
        private val activeSessionHolder: ActiveSessionHolder,
        private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
        private val getNotificationsStatusUseCase: GetNotificationsStatusUseCase,
        refreshDevicesUseCase: RefreshDevicesUseCase,
        private val vectorPreferences: VectorPreferences,
        private val toggleIpAddressVisibilityUseCase: ToggleIpAddressVisibilityUseCase,
) : VectorSessionsListViewModel<SessionOverviewViewState, SessionOverviewAction, SessionOverviewViewEvent>(
        initialState, activeSessionHolder, refreshDevicesUseCase
), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object : MavericksViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> {
        override fun create(initialState: SessionOverviewViewState): SessionOverviewViewModel
    }

    init {
        refreshPushers()
        observeSessionInfo(initialState.deviceId)
        observeCurrentSessionInfo()
        observeNotificationsStatus(initialState.deviceId)
        refreshIpAddressVisibility()
        observePreferences()
        initExternalAccountManagementUrl()
    }

    private fun initExternalAccountManagementUrl() {
        setState {
            copy(
                    externalAccountManagementUrl = activeSessionHolder.getSafeActiveSession()
                            ?.homeServerCapabilitiesService()
                            ?.getHomeServerCapabilities()
                            ?.externalAccountManagementUrl
            )
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        refreshIpAddressVisibility()
    }

    private fun observePreferences() {
        vectorPreferences.subscribeToChanges(this)
    }

    override fun onCleared() {
        vectorPreferences.unsubscribeToChanges(this)
        super.onCleared()
    }
    private fun refreshIpAddressVisibility() {
        val shouldShowIpAddress = vectorPreferences.showIpAddressInSessionManagerScreens()
        setState {
            copy(isShowingIpAddress = shouldShowIpAddress)
        }
    }

    private fun refreshPushers() {
        activeSessionHolder.getSafeActiveSession()?.pushersService()?.refreshPushers()
    }

    private fun observeSessionInfo(deviceId: String) {
        getDeviceFullInfoUseCase.execute(deviceId)
                .onEach { setState { copy(deviceInfo = Success(it)) } }
                .launchIn(viewModelScope)
    }

    private fun observeCurrentSessionInfo() {
        activeSessionHolder.getSafeActiveSession()
                ?.sessionParams
                ?.deviceId
                ?.let { deviceId ->
                    getDeviceFullInfoUseCase.execute(deviceId)
                            .map { it.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Trusted }
                            .distinctUntilChanged()
                            .onEach { setState { copy(isCurrentSessionTrusted = it) } }
                            .launchIn(viewModelScope)
                }
    }

    private fun observeNotificationsStatus(deviceId: String) {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            getNotificationsStatusUseCase.execute(session, deviceId)
                    .onEach { setState { copy(notificationsStatus = it) } }
                    .launchIn(viewModelScope)
        }
    }

    override fun handle(action: SessionOverviewAction) {
        when (action) {
            is SessionOverviewAction.VerifySession -> handleVerifySessionAction()
            SessionOverviewAction.SignoutOtherSession -> handleSignoutOtherSession()
            SessionOverviewAction.SsoAuthDone -> handleSsoAuthDone()
            is SessionOverviewAction.PasswordAuthDone -> handlePasswordAuthDone(action)
            SessionOverviewAction.ReAuthCancelled -> handleReAuthCancelled()
            is SessionOverviewAction.TogglePushNotifications -> handleTogglePusherAction(action)
            SessionOverviewAction.ToggleIpAddressVisibility -> handleToggleIpAddressVisibility()
        }
    }

    private fun handleToggleIpAddressVisibility() {
        toggleIpAddressVisibilityUseCase.execute()
    }

    private fun handleVerifySessionAction() = withState { viewState ->
        if (viewState.deviceInfo.invoke()?.isCurrentDevice.orFalse()) {
            handleVerifyCurrentSession()
        } else {
            handleVerifyOtherSession(viewState.deviceId)
        }
    }

    private fun handleVerifyCurrentSession() {
        viewModelScope.launch {
            val currentSessionCanBeVerified = checkIfCurrentSessionCanBeVerifiedUseCase.execute()
            if (currentSessionCanBeVerified) {
                _viewEvents.post(SessionOverviewViewEvent.ShowVerifyCurrentSession)
            } else {
                _viewEvents.post(SessionOverviewViewEvent.PromptResetSecrets)
            }
        }
    }

    private fun handleVerifyOtherSession(deviceId: String) {
        _viewEvents.post(SessionOverviewViewEvent.ShowVerifyOtherSession(deviceId))
    }

    private fun handleSignoutOtherSession() = withState { state ->
        // signout process for current session is not handled here
        if (!state.deviceInfo.invoke()?.isCurrentDevice.orFalse()) {
            handleSignoutOtherSession(state.deviceId)
        }
    }

    private fun handleSignoutOtherSession(deviceId: String) {
        viewModelScope.launch {
            setLoading(true)
            val result = signout(deviceId)
            setLoading(false)

            val error = result.exceptionOrNull()
            if (error == null) {
                onSignoutSuccess()
            } else {
                onSignoutFailure(error)
            }
        }
    }

    private suspend fun signout(deviceId: String) = signoutSessionsUseCase.execute(listOf(deviceId), this::onReAuthNeeded)

    private fun onReAuthNeeded(reAuthNeeded: SignoutSessionsReAuthNeeded) {
        Timber.d("onReAuthNeeded")
        pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        pendingAuthHandler.uiaContinuation = reAuthNeeded.uiaContinuation
        _viewEvents.post(SessionOverviewViewEvent.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode))
    }

    private fun setLoading(isLoading: Boolean) {
        setState { copy(isLoading = isLoading) }
    }

    private fun onSignoutSuccess() {
        Timber.d("signout success")
        refreshDeviceList()
        _viewEvents.post(SessionOverviewViewEvent.SignoutSuccess)
    }

    private fun onSignoutFailure(failure: Throwable) {
        Timber.e("signout failure", failure)
        _viewEvents.post(SessionOverviewViewEvent.SignoutError(failure))
    }

    private fun handleSsoAuthDone() {
        pendingAuthHandler.ssoAuthDone()
    }

    private fun handlePasswordAuthDone(action: SessionOverviewAction.PasswordAuthDone) {
        pendingAuthHandler.passwordAuthDone(action.password)
    }

    private fun handleReAuthCancelled() {
        pendingAuthHandler.reAuthCancelled()
    }

    private fun handleTogglePusherAction(action: SessionOverviewAction.TogglePushNotifications) {
        viewModelScope.launch {
            toggleNotificationsUseCase.execute(action.deviceId, action.enabled)
        }
    }
}
