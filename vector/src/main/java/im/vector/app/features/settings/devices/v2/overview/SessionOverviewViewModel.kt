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
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.settings.devices.v2.IsCurrentSessionUseCase
import im.vector.app.features.settings.devices.v2.signout.InterceptSignoutFlowResponseUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionResult
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionUseCase
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import timber.log.Timber
import kotlin.coroutines.Continuation

class SessionOverviewViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionOverviewViewState,
        private val activeSessionHolder: ActiveSessionHolder,
        private val isCurrentSessionUseCase: IsCurrentSessionUseCase,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
        private val checkIfCurrentSessionCanBeVerifiedUseCase: CheckIfCurrentSessionCanBeVerifiedUseCase,
        private val signoutSessionUseCase: SignoutSessionUseCase,
        private val interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
        private val pendingAuthHandler: PendingAuthHandler,
) : VectorViewModel<SessionOverviewViewState, SessionOverviewAction, SessionOverviewViewEvent>(initialState) {

    companion object : MavericksViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> {
        override fun create(initialState: SessionOverviewViewState): SessionOverviewViewModel
    }

    init {
        setState {
            copy(isCurrentSession = isCurrentSession(deviceId))
        }
        observeSessionInfo(initialState.deviceId)
        observeCurrentSessionInfo()
    }

    private fun isCurrentSession(deviceId: String): Boolean {
        return isCurrentSessionUseCase.execute(deviceId)
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

    // TODO add unit tests
    override fun handle(action: SessionOverviewAction) {
        when (action) {
            is SessionOverviewAction.VerifySession -> handleVerifySessionAction()
            SessionOverviewAction.SignoutSession -> handleSignoutSession()
            SessionOverviewAction.SsoAuthDone -> handleSsoAuthDone()
            is SessionOverviewAction.PasswordAuthDone -> handlePasswordAuthDone(action)
            SessionOverviewAction.ReAuthCancelled -> handleReAuthCancelled()
        }
    }

    private fun handleVerifySessionAction() = withState { viewState ->
        if (viewState.isCurrentSession) {
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

    // TODO add unit tests
    private fun handleSignoutSession() = withState { state ->
        // TODO should we do something different when it is current session?
        viewModelScope.launch {
            signoutSessionUseCase.execute(state.deviceId, object : UserInteractiveAuthInterceptor {
                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                    when (val result = interceptSignoutFlowResponseUseCase.execute(flowResponse, errCode, promise)) {
                        is SignoutSessionResult.ReAuthNeeded -> onReAuthNeeded(result)
                        is SignoutSessionResult.Completed -> {
                            Timber.d("signout completed")
                            // TODO check if it is called after a reAuth
                            // TODO refresh devices list? + post event to close the associated screen
                        }
                    }
                }
            })
        }
    }

    private fun onReAuthNeeded(reAuthNeeded: SignoutSessionResult.ReAuthNeeded) {
        Timber.d("onReAuthNeeded")
        pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        pendingAuthHandler.uiaContinuation = reAuthNeeded.uiaContinuation
        _viewEvents.post(SessionOverviewViewEvent.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode))
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
}
