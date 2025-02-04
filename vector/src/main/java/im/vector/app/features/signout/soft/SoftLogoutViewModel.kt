/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.signout.soft

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.hasUnsavedKeys
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.toSsoState
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber

class SoftLogoutViewModel @AssistedInject constructor(
        @Assisted initialState: SoftLogoutViewState,
        private val session: Session,
        private val activeSessionHolder: ActiveSessionHolder,
        private val authenticationService: AuthenticationService
) : VectorViewModel<SoftLogoutViewState, SoftLogoutAction, SoftLogoutViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SoftLogoutViewModel, SoftLogoutViewState> {
        override fun create(initialState: SoftLogoutViewState): SoftLogoutViewModel
    }

    companion object : MavericksViewModelFactory<SoftLogoutViewModel, SoftLogoutViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): SoftLogoutViewState {
            val sessionHolder = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java)
                    .activeSessionHolder()

            return if (sessionHolder.hasActiveSession()) {
                val session = sessionHolder.getActiveSession()
                val userId = session.myUserId

                SoftLogoutViewState(
                        homeServerUrl = session.sessionParams.homeServerUrl,
                        userId = userId,
                        deviceId = session.sessionParams.deviceId.orEmpty(),
                        userDisplayName = session.getUserOrDefault(userId).toMatrixItem().getBestName(),
                        hasUnsavedKeys = Loading(),
                        loginType = session.sessionParams.loginType,
                )
            } else {
                SoftLogoutViewState(
                        homeServerUrl = "",
                        userId = "",
                        deviceId = "",
                        userDisplayName = "",
                        hasUnsavedKeys = Success(false),
                        loginType = LoginType.UNKNOWN,
                )
            }
        }
    }

    init {
        checkHasUnsavedKeys()
        // Get the supported login flow
        getSupportedLoginFlow()
    }

    private fun checkHasUnsavedKeys() {
        suspend {
            session.hasUnsavedKeys()
        }.execute {
            copy(hasUnsavedKeys = it)
        }
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
                data.supportedLoginTypes.contains(LoginFlowTypes.SSO) &&
                        data.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.SsoAndPassword(
                            data.ssoIdentityProviders.toSsoState(),
                            data.hasOidcCompatibilityFlow
                        )
                data.supportedLoginTypes.contains(LoginFlowTypes.SSO) -> LoginMode.Sso(data.ssoIdentityProviders.toSsoState(), data.hasOidcCompatibilityFlow)
                data.supportedLoginTypes.contains(LoginFlowTypes.PASSWORD) -> LoginMode.Password
                else -> LoginMode.Unsupported
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
            is SoftLogoutAction.RetryLoginFlow -> getSupportedLoginFlow()
            is SoftLogoutAction.PasswordChanged -> handlePasswordChange(action)
            is SoftLogoutAction.SignInAgain -> handleSignInAgain(action)
            is SoftLogoutAction.WebLoginSuccess -> handleWebLoginSuccess(action)
            is SoftLogoutAction.ClearData -> handleClearData()
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
                _viewEvents.post(
                        SoftLogoutViewEvents.ErrorNotSameUser(
                                softLogoutViewState.userId,
                                action.credentials.userId
                        )
                )
            } else {
                setState {
                    copy(
                            asyncLoginAction = Loading()
                    )
                }
                viewModelScope.launch {
                    try {
                        session.signOutService().updateCredentials(action.credentials)
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
                session.signOutService().signInAgain(action.password)
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
        session.syncService().startSync(true)

        // TODO Configure and start ? Check that the push still works...
        setState {
            copy(
                    asyncLoginAction = Success(Unit)
            )
        }
    }
}
