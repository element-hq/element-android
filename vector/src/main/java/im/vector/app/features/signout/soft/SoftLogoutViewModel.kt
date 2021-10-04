/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.signout.soft

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.hasUnsavedKeys
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.login.LoginMode
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber

/**
 * TODO Test push: disable the pushers?
 */
class SoftLogoutViewModel @AssistedInject constructor(
        @Assisted initialState: SoftLogoutViewState,
        private val session: Session,
        private val activeSessionHolder: ActiveSessionHolder,
        private val authenticationService: AuthenticationService
) : VectorViewModel<SoftLogoutViewState, SoftLogoutAction, SoftLogoutViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SoftLogoutViewState): SoftLogoutViewModel
    }

    companion object : MvRxViewModelFactory<SoftLogoutViewModel, SoftLogoutViewState> {

        override fun initialState(viewModelContext: ViewModelContext): SoftLogoutViewState? {
            val activity: SoftLogoutActivity = (viewModelContext as ActivityViewModelContext).activity()
            val userId = activity.session.myUserId
            return SoftLogoutViewState(
                    homeServerUrl = activity.session.sessionParams.homeServerUrl,
                    userId = userId,
                    deviceId = activity.session.sessionParams.deviceId ?: "",
                    userDisplayName = activity.session.getUser(userId)?.displayName ?: userId,
                    hasUnsavedKeys = activity.session.hasUnsavedKeys()
            )
        }

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SoftLogoutViewState): SoftLogoutViewModel? {
            val activity: SoftLogoutActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.softLogoutViewModelFactory.create(state)
        }
    }

    init {
        // Get the supported login flow
        getSupportedLoginFlow()
    }

    private fun getSupportedLoginFlow() {
        viewModelScope.launch {
            authenticationService.cancelPendingLoginOrRegistration()

            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Loading()
                )
            }

            val data = try {
                authenticationService.getLoginFlowOfSession(session.sessionId)
            } catch (failure: Throwable) {
                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Fail(failure)
                    )
                }
                null
            }

            data ?: return@launch

            val loginMode = when {
                // SSO login is taken first
                data.supportedLoginTypes.contains(LoginFlowTypes.SSO)
                        && data.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.SsoAndPassword(data.ssoIdentityProviders)
                data.supportedLoginTypes.contains(LoginFlowTypes.SSO)                 -> LoginMode.Sso(data.ssoIdentityProviders)
                data.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD)            -> LoginMode.Password
                else                                                                  -> LoginMode.Unsupported
            }

            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Success(loginMode)
                )
            }
        }
    }

    override fun handle(action: SoftLogoutAction) {
        when (action) {
            is SoftLogoutAction.RetryLoginFlow  -> getSupportedLoginFlow()
            is SoftLogoutAction.PasswordChanged -> handlePasswordChange(action)
            is SoftLogoutAction.SignInAgain     -> handleSignInAgain(action)
            is SoftLogoutAction.WebLoginSuccess -> handleWebLoginSuccess(action)
            is SoftLogoutAction.ClearData       -> handleClearData()
        }
    }

    private fun handleClearData() {
        // Notify the Activity
        _viewEvents.post(SoftLogoutViewEvents.ClearData)
    }

    private fun handlePasswordChange(action: SoftLogoutAction.PasswordChanged) {
        setState {
            copy(
                    asyncLoginAction = Uninitialized,
                    enteredPassword = action.password
            )
        }
    }

    private fun handleWebLoginSuccess(action: SoftLogoutAction.WebLoginSuccess) {
        // User may have been connected with SSO with another userId
        // We have to check this
        withState { softLogoutViewState ->
            if (softLogoutViewState.userId != action.credentials.userId) {
                Timber.w("User login again with SSO, but using another account")
                _viewEvents.post(SoftLogoutViewEvents.ErrorNotSameUser(
                        softLogoutViewState.userId,
                        action.credentials.userId))
            } else {
                setState {
                    copy(
                            asyncLoginAction = Loading()
                    )
                }
                viewModelScope.launch {
                    try {
                        session.updateCredentials(action.credentials)
                        onSessionRestored()
                    } catch (failure: Throwable) {
                        _viewEvents.post(SoftLogoutViewEvents.Failure(failure))
                        setState {
                            copy(
                                    asyncLoginAction = Uninitialized
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSignInAgain(action: SoftLogoutAction.SignInAgain) {
        setState {
            copy(
                    asyncLoginAction = Loading()
            )
        }
        viewModelScope.launch {
            try {
                session.signInAgain(action.password)
                onSessionRestored()
            } catch (failure: Throwable) {
                setState {
                    copy(
                            asyncLoginAction = Fail(failure)
                    )
                }
            }
        }
    }

    private fun onSessionRestored() {
        activeSessionHolder.setActiveSession(session)
        // Start the sync
        session.startSync(true)

        // TODO Configure and start ? Check that the push still works...
        setState {
            copy(
                    asyncLoginAction = Success(Unit)
            )
        }
    }
}
