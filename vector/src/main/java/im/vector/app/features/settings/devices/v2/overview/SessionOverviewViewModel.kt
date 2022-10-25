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

package im.vector.app.features.settings.devices.v2.overview

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.VectorSessionsListViewModel
import im.vector.app.features.settings.devices.v2.signout.InterceptSignoutFlowResponseUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionResult
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionUseCase
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.Continuation

class SessionOverviewViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionOverviewViewState,
        private val stringProvider: StringProvider,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
        private val checkIfCurrentSessionCanBeVerifiedUseCase: CheckIfCurrentSessionCanBeVerifiedUseCase,
        private val signoutSessionUseCase: SignoutSessionUseCase,
        private val interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
        private val pendingAuthHandler: PendingAuthHandler,
        private val activeSessionHolder: ActiveSessionHolder,
        private val togglePushNotificationUseCase: TogglePushNotificationUseCase,
        refreshDevicesUseCase: RefreshDevicesUseCase,
) : VectorSessionsListViewModel<SessionOverviewViewState, SessionOverviewAction, SessionOverviewViewEvent>(
        initialState, activeSessionHolder, refreshDevicesUseCase
) {

    companion object : MavericksViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> {
        override fun create(initialState: SessionOverviewViewState): SessionOverviewViewModel
    }

    init {
        refreshPushers()
        observeSessionInfo(initialState.deviceId)
        observeCurrentSessionInfo()
        observePushers(initialState.deviceId)
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

    private fun observePushers(deviceId: String) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val pusherFlow = session.flow()
                .livePushers()
                .map { it.filter { pusher -> pusher.deviceId == deviceId } }
                .map { it.takeIf { it.isNotEmpty() }?.any { pusher -> pusher.enabled } }

        val accountDataFlow = session.flow()
                .liveUserAccountData(TYPE_LOCAL_NOTIFICATION_SETTINGS + deviceId)
                .unwrap()
                .map { it.content.toModel<LocalNotificationSettingsContent>()?.isSilenced?.not() }

        merge(pusherFlow, accountDataFlow)
                .onEach { it?.let { setState { copy(notificationsEnabled = it) } } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: SessionOverviewAction) {
        when (action) {
            is SessionOverviewAction.VerifySession -> handleVerifySessionAction()
            SessionOverviewAction.SignoutOtherSession -> handleSignoutOtherSession()
            SessionOverviewAction.SsoAuthDone -> handleSsoAuthDone()
            is SessionOverviewAction.PasswordAuthDone -> handlePasswordAuthDone(action)
            SessionOverviewAction.ReAuthCancelled -> handleReAuthCancelled()
            is SessionOverviewAction.TogglePushNotifications -> handleTogglePusherAction(action)
        }
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
            val signoutResult = signout(deviceId)
            setLoading(false)

            if (signoutResult.isSuccess) {
                onSignoutSuccess()
            } else {
                when (val failure = signoutResult.exceptionOrNull()) {
                    null -> onSignoutSuccess()
                    else -> onSignoutFailure(failure)
                }
            }
        }
    }

    private suspend fun signout(deviceId: String) = signoutSessionUseCase.execute(deviceId, object : UserInteractiveAuthInterceptor {
        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
            when (val result = interceptSignoutFlowResponseUseCase.execute(flowResponse, errCode, promise)) {
                is SignoutSessionResult.ReAuthNeeded -> onReAuthNeeded(result)
                is SignoutSessionResult.Completed -> Unit
            }
        }
    })

    private fun onReAuthNeeded(reAuthNeeded: SignoutSessionResult.ReAuthNeeded) {
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
        val failureMessage = if (failure is Failure.OtherServerError && failure.httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
            stringProvider.getString(R.string.authentication_error)
        } else {
            stringProvider.getString(R.string.matrix_error)
        }
        _viewEvents.post(SessionOverviewViewEvent.SignoutError(Exception(failureMessage)))
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
            togglePushNotificationUseCase.execute(action.deviceId, action.enabled)
            setState { copy(notificationsEnabled = action.enabled) }
        }
    }
}
