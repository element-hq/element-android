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
package im.vector.app.features.settings.account.deactivation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.auth.PendingAuthHandler
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.failure.isInvalidUIAAuth
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import kotlin.coroutines.Continuation

data class DeactivateAccountViewState(
        val dummy: Boolean = false
) : MavericksState

class DeactivateAccountViewModel @AssistedInject constructor(
        @Assisted private val initialState: DeactivateAccountViewState,
        private val session: Session,
        private val pendingAuthHandler: PendingAuthHandler,
) :
        VectorViewModel<DeactivateAccountViewState, DeactivateAccountAction, DeactivateAccountViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DeactivateAccountViewModel, DeactivateAccountViewState> {
        override fun create(initialState: DeactivateAccountViewState): DeactivateAccountViewModel
    }

    override fun handle(action: DeactivateAccountAction) {
        when (action) {
            is DeactivateAccountAction.DeactivateAccount -> handleDeactivateAccount(action)
            DeactivateAccountAction.SsoAuthDone -> {
                _viewEvents.post(DeactivateAccountViewEvents.Loading())
                pendingAuthHandler.ssoAuthDone()
            }
            is DeactivateAccountAction.PasswordAuthDone -> {
                _viewEvents.post(DeactivateAccountViewEvents.Loading())
                pendingAuthHandler.passwordAuthDone(action.password)
            }
            DeactivateAccountAction.ReAuthCancelled -> pendingAuthHandler.reAuthCancelled()
        }
    }

    private fun handleDeactivateAccount(action: DeactivateAccountAction.DeactivateAccount) {
        _viewEvents.post(DeactivateAccountViewEvents.Loading())

        viewModelScope.launch {
            val event = try {
                session.accountService().deactivateAccount(
                        action.eraseAllData,
                        object : UserInteractiveAuthInterceptor {
                            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                _viewEvents.post(DeactivateAccountViewEvents.RequestReAuth(flowResponse, errCode))
                                pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                                pendingAuthHandler.uiaContinuation = promise
                            }
                        }
                )
                DeactivateAccountViewEvents.Done
            } catch (failure: Throwable) {
                if (failure.isInvalidUIAAuth()) {
                    DeactivateAccountViewEvents.InvalidAuth
                } else {
                    DeactivateAccountViewEvents.OtherFailure(failure)
                }
            }

            _viewEvents.post(event)
        }
    }

    companion object : MavericksViewModelFactory<DeactivateAccountViewModel, DeactivateAccountViewState> by hiltMavericksViewModelFactory()
}
