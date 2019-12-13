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

package im.vector.riotx.features.signout.soft

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.api.auth.data.LoginFlowResult
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.hasUnsavedKeys
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.DataSource
import im.vector.riotx.core.utils.PublishDataSource
import im.vector.riotx.features.login.LoginMode
import timber.log.Timber

/**
 * TODO Test push: disable the pushers?
 */
class SoftLogoutViewModel @AssistedInject constructor(
        @Assisted initialState: SoftLogoutViewState,
        private val session: Session,
        private val activeSessionHolder: ActiveSessionHolder,
        private val authenticationService: AuthenticationService
) : VectorViewModel<SoftLogoutViewState, SoftLogoutAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SoftLogoutViewState): SoftLogoutViewModel
    }

    companion object : MvRxViewModelFactory<SoftLogoutViewModel, SoftLogoutViewState> {

        override fun initialState(viewModelContext: ViewModelContext): SoftLogoutViewState? {
            val activity: SoftLogoutActivity = (viewModelContext as ActivityViewModelContext).activity()
            val userId = activity.session.myUserId
            return SoftLogoutViewState(
                    homeServerUrl = activity.session.sessionParams.homeServerConnectionConfig.homeServerUri.toString(),
                    userId = userId,
                    deviceId = activity.session.sessionParams.credentials.deviceId ?: "",
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

    private var currentTask: Cancelable? = null

    private val _viewEvents = PublishDataSource<SoftLogoutViewEvents>()
    val viewEvents: DataSource<SoftLogoutViewEvents> = _viewEvents

    init {
        // Get the supported login flow
        getSupportedLoginFlow()
    }

    private fun getSupportedLoginFlow() {
        val homeServerConnectionConfig = session.sessionParams.homeServerConnectionConfig

        currentTask?.cancel()
        currentTask = null
        authenticationService.cancelPendingLoginOrRegistration()

        setState {
            copy(
                    asyncHomeServerLoginFlowRequest = Loading()
            )
        }

        currentTask = authenticationService.getLoginFlow(homeServerConnectionConfig, object : MatrixCallback<LoginFlowResult> {
            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Fail(failure)
                    )
                }
            }

            override fun onSuccess(data: LoginFlowResult) {
                when (data) {
                    is LoginFlowResult.Success            -> {
                        val loginMode = when {
                            // SSO login is taken first
                            data.loginFlowResponse.flows.any { it.type == LoginFlowTypes.SSO }      -> LoginMode.Sso
                            data.loginFlowResponse.flows.any { it.type == LoginFlowTypes.PASSWORD } -> LoginMode.Password
                            else                                                                    -> LoginMode.Unsupported
                        }

                        if ((loginMode == LoginMode.Password && !data.isLoginAndRegistrationSupported)
                                || loginMode == LoginMode.Unsupported) {
                            notSupported()
                        } else {
                            setState {
                                copy(
                                        asyncHomeServerLoginFlowRequest = Success(loginMode)
                                )
                            }
                        }
                    }
                    is LoginFlowResult.OutdatedHomeserver -> {
                        notSupported()
                    }
                }
            }

            private fun notSupported() {
                // Should not happen since it's a re-logout
                // Notify the UI
                // _viewEvents.post(LoginViewEvents.OutdatedHomeserver)

                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Fail(IllegalStateException("Should not happen"))
                    )
                }
            }
        })
    }

    override fun handle(action: SoftLogoutAction) {
        when (action) {
            is SoftLogoutAction.RetryLoginFlow  -> getSupportedLoginFlow()
            is SoftLogoutAction.SignInAgain     -> handleSignInAgain(action)
            is SoftLogoutAction.WebLoginSuccess -> handleWebLoginSuccess(action)
            is SoftLogoutAction.PasswordChanged -> handlePasswordChange(action)
            is SoftLogoutAction.TogglePassword  -> handleTogglePassword()
        }
    }

    private fun handlePasswordChange(action: SoftLogoutAction.PasswordChanged) {
        setState {
            copy(
                    asyncLoginAction = Uninitialized,
                    submitEnabled = action.password.isNotBlank()
            )
        }
    }

    private fun handleTogglePassword() {
        withState {
            setState {
                copy(
                        passwordShown = !this.passwordShown
                )
            }
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
                currentTask = session.updateCredentials(action.credentials,
                        object : MatrixCallback<Unit> {
                            override fun onFailure(failure: Throwable) {
                                _viewEvents.post(SoftLogoutViewEvents.Error(failure))
                                setState {
                                    copy(
                                            asyncLoginAction = Uninitialized
                                    )
                                }
                            }

                            override fun onSuccess(data: Unit) {
                                onSessionRestored()
                            }
                        }
                )
            }
        }
    }

    private fun handleSignInAgain(action: SoftLogoutAction.SignInAgain) {
        setState {
            copy(
                    asyncLoginAction = Loading(),
                    // Ensure password is hidden
                    passwordShown = false
            )
        }
        currentTask = session.signInAgain(action.password,
                object : MatrixCallback<Unit> {
                    override fun onFailure(failure: Throwable) {
                        setState {
                            copy(
                                    asyncLoginAction = Fail(failure)
                            )
                        }
                    }

                    override fun onSuccess(data: Unit) {
                        onSessionRestored()
                    }
                }
        )
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

    override fun onCleared() {
        super.onCleared()

        currentTask?.cancel()
    }
}
