/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.login

import android.content.Context
import androidx.fragment.app.FragmentActivity
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
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.login.HomeServerConnectionConfigFactory
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.LoginViewEvents
import im.vector.app.features.login.LoginViewState
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.session.Session
import java.util.concurrent.CancellationException

/**
 *
 */
class TchapLoginViewModel @AssistedInject constructor(
        @Assisted initialState: LoginViewState,
        private val applicationContext: Context,
        private val authenticationService: AuthenticationService,
        private val activeSessionHolder: ActiveSessionHolder,
        private val homeServerConnectionConfigFactory: HomeServerConnectionConfigFactory,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider
) : VectorViewModel<LoginViewState, TchapLoginAction, TchapLoginViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: LoginViewState): TchapLoginViewModel
    }

    companion object : MvRxViewModelFactory<TchapLoginViewModel, LoginViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: LoginViewState): TchapLoginViewModel? {
            return when (val activity: FragmentActivity = (viewModelContext as ActivityViewModelContext).activity()) {
                is TchapLoginActivity      -> activity.tchapLoginViewModelFactory.create(state)
//                is SoftLogoutActivity -> activity.tchapLoginViewModelFactory.create(state)
                else                  -> error("Invalid Activity")
            }
        }
    }

    // Store the last action, to redo it after user has trusted the untrusted certificate
    private var lastAction: TchapLoginAction? = null
    private var currentHomeServerConnectionConfig: HomeServerConnectionConfig? = null

    val currentThreePid: String?
        get() = registrationWizard?.currentThreePid

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
        get() = authenticationService.isRegistrationStarted

    private val registrationWizard: RegistrationWizard?
        get() = authenticationService.getRegistrationWizard()

    private val loginWizard: LoginWizard?
        get() = authenticationService.getLoginWizard()

    private var loginConfig: LoginConfig? = null

    private var currentJob: Job? = null
        set(value) {
            // Cancel any previous Job
            field?.cancel()
            field = value
        }

    override fun handle(action: TchapLoginAction) {
        when (action) {
            is TchapLoginAction.UpdateSignMode             -> handleUpdateSignMode(action)
            is TchapLoginAction.InitWith                   -> handleInitWith(action)
            is TchapLoginAction.UpdateHomeServer           -> handleUpdateHomeserver(action).also { lastAction = action }
            is TchapLoginAction.LoginOrRegister            -> handleLoginOrRegister(action).also { lastAction = action }
            is TchapLoginAction.ResetPassword              -> handleResetPassword()
            is TchapLoginAction.ResetPasswordMailConfirmed -> handleResetPasswordMailConfirmed()
            is TchapLoginAction.RegisterAction             -> handleRegisterAction(action)
            is TchapLoginAction.ResetAction                -> handleResetAction(action)
            is TchapLoginAction.SetupSsoForSessionRecovery -> handleSetupSsoForSessionRecovery(action)
            is TchapLoginAction.PostViewEvent              -> _viewEvents.post(action.viewEvent)
        }.exhaustive
    }

    private fun handleSetupSsoForSessionRecovery(action: TchapLoginAction.SetupSsoForSessionRecovery) {
        setState {
            copy(
                    signMode = SignMode.SignIn,
                    loginMode = LoginMode.Sso(action.ssoIdentityProviders),
                    homeServerUrl = action.homeServerUrl,
                    deviceId = action.deviceId
            )
        }
    }

    private fun handleRegisterAction(action: TchapLoginAction.RegisterAction) {
        when (action) {
            is TchapLoginAction.CaptchaDone                  -> handleCaptchaDone(action)
            is TchapLoginAction.AcceptTerms                  -> handleAcceptTerms()
            is TchapLoginAction.RegisterDummy                -> handleRegisterDummy()
            is TchapLoginAction.AddThreePid                  -> handleAddThreePid(action)
            is TchapLoginAction.SendAgainThreePid            -> handleSendAgainThreePid()
            is TchapLoginAction.ValidateThreePid             -> handleValidateThreePid(action)
        }
    }

    private fun handleValidateThreePid(action: TchapLoginAction.ValidateThreePid) {
        currentJob = executeRegistrationStep {
            it.handleValidateThreePid(action.code)
        }
    }

    private fun executeRegistrationStep(withLoading: Boolean = true,
                                        block: suspend (RegistrationWizard) -> RegistrationResult): Job {
        if (withLoading) {
            setState { copy(asyncRegistration = Loading()) }
        }
        return viewModelScope.launch {
            try {
                registrationWizard?.let { block(it) }
                /*
                   // Simulate registration disabled
                   throw Failure.ServerError(MatrixError(
                           code = MatrixError.FORBIDDEN,
                           message = "Registration is disabled"
                   ), 403))
                */
            } catch (failure: Throwable) {
                if (failure !is CancellationException) {
                    _viewEvents.post(TchapLoginViewEvents.Failure(failure))
                }
                null
            }
                    ?.let { data ->
                        when (data) {
                            is RegistrationResult.Success      -> onSessionCreated(data.session)
                            is RegistrationResult.FlowResponse -> onFlowResponse(data.flowResult)
                        }
                    }

            setState {
                copy(
                        asyncRegistration = Uninitialized
                )
            }
        }
    }

    private fun handleAddThreePid(action: TchapLoginAction.AddThreePid) {
        setState { copy(asyncRegistration = Loading()) }
        currentJob = viewModelScope.launch {
            try {
                registrationWizard?.addThreePid(action.threePid)
            } catch (failure: Throwable) {
                _viewEvents.post(TchapLoginViewEvents.Failure(failure))
            }
            setState {
                copy(
                        asyncRegistration = Uninitialized
                )
            }
        }
    }

    private fun handleSendAgainThreePid() {
        setState { copy(asyncRegistration = Loading()) }
        currentJob = viewModelScope.launch {
            try {
                registrationWizard?.sendAgainThreePid()
            } catch (failure: Throwable) {
                _viewEvents.post(TchapLoginViewEvents.Failure(failure))
            }
            setState {
                copy(
                        asyncRegistration = Uninitialized
                )
            }
        }
    }

    private fun handleAcceptTerms() {
        currentJob = executeRegistrationStep {
            it.acceptTerms()
        }
    }

    private fun handleRegisterDummy() {
        currentJob = executeRegistrationStep {
            it.dummy()
        }
    }

    private fun handleRegisterWith(action: TchapLoginAction.LoginOrRegister) {
        reAuthHelper.data = action.password
        currentJob = executeRegistrationStep {
            // Tchap registration doesn't require userName.
            // The initialDeviceDisplayName is useless because the account will be actually created after the email validation (eventually on another device).
            // This first register request will link the account password with the returned session id (used in the following steps).
            it.createAccount(
                    null,
                    action.password,
                    null
            )
        }
    }

    private fun handleCaptchaDone(action: TchapLoginAction.CaptchaDone) {
        currentJob = executeRegistrationStep {
            it.performReCaptcha(action.captchaResponse)
        }
    }

    private fun handleResetAction(action: TchapLoginAction.ResetAction) {
        // Cancel any request
        currentJob = null

        when (action) {
            TchapLoginAction.ResetSignMode       -> {
                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Uninitialized,
                            signMode = SignMode.Unknown,
                            loginMode = LoginMode.Unknown,
                            loginModeSupportedTypes = emptyList()
                    )
                }
            }
            TchapLoginAction.ResetLogin          -> {
                viewModelScope.launch {
                    authenticationService.cancelPendingLoginOrRegistration()
                    setState {
                        copy(
                                asyncLoginAction = Uninitialized,
                                asyncRegistration = Uninitialized
                        )
                    }
                }
            }
            TchapLoginAction.ResetResetPassword  -> {
                setState {
                    copy(
                            asyncResetPassword = Uninitialized,
                            asyncResetMailConfirmed = Uninitialized,
                            resetPasswordEmail = null
                    )
                }
            }
        }
    }

    private fun handleUpdateSignMode(action: TchapLoginAction.UpdateSignMode) {
        setState {
            copy(
                    signMode = action.signMode
            )
        }

        when (action.signMode) {
            SignMode.SignUp             -> _viewEvents.post(TchapLoginViewEvents.OnSignModeSelected(SignMode.SignUp))
            SignMode.SignIn             -> _viewEvents.post(TchapLoginViewEvents.OnSignModeSelected(SignMode.SignIn))
            SignMode.SignInWithMatrixId -> Unit // Unsupported on Tchap
            SignMode.Unknown            -> Unit
        }
    }

    private fun handleInitWith(action: TchapLoginAction.InitWith) {
        loginConfig = action.loginConfig

        // If there is a pending email validation continue on this step
        try {
            if (registrationWizard?.isRegistrationStarted == true) {
                currentThreePid?.let {
                    handle(TchapLoginAction.PostViewEvent(TchapLoginViewEvents.OnSendEmailSuccess(it)))
                }
            }
        } catch (e: Throwable) {
            // NOOP. API is designed to use wizards in a login/registration flow,
            // but we need to check the state anyway.
        }
    }

    private fun handleResetPassword() {

    }

    private fun handleResetPasswordMailConfirmed() {

    }

    private fun handleLoginOrRegister(action: TchapLoginAction.LoginOrRegister) = withState { state ->
        when (state.signMode) {
            SignMode.Unknown            -> error("Developer error, invalid sign mode")
            SignMode.SignIn             -> handleLogin(action)
            SignMode.SignUp             -> handleRegisterWith(action)
            SignMode.SignInWithMatrixId -> Unit // Unsupported on Tchap
        }.exhaustive
    }

    private fun handleLogin(action: TchapLoginAction.LoginOrRegister) {
        val safeLoginWizard = loginWizard

        if (safeLoginWizard == null) {
            setState {
                copy(
                        asyncLoginAction = Fail(Throwable("Bad configuration"))
                )
            }
        } else {
            setState {
                copy(
                        asyncLoginAction = Loading()
                )
            }

            currentJob = viewModelScope.launch {
                try {
                    safeLoginWizard.login(
                            action.username,
                            action.password,
                            action.initialDeviceName
                    )
                } catch (failure: Throwable) {
                    setState {
                        copy(
                                asyncLoginAction = Fail(failure)
                        )
                    }
                    null
                }
                        ?.let {
                            reAuthHelper.data = action.password
                            onSessionCreated(it)
                        }
            }
        }
    }

    private fun onFlowResponse(flowResult: FlowResult) {
        // If dummy stage is mandatory, and password is already sent, do the dummy stage now
        if (isRegistrationStarted
                && flowResult.missingStages.any { it is Stage.Dummy && it.mandatory }) {
            handleRegisterDummy()
        } else {
            // Notify the user
            _viewEvents.post(TchapLoginViewEvents.RegistrationFlowResult(flowResult, isRegistrationStarted))
        }
    }

    private suspend fun onSessionCreated(session: Session) {
        activeSessionHolder.setActiveSession(session)

        authenticationService.reset()
        session.configureAndStart(applicationContext)
        setState {
            copy(
                    asyncLoginAction = Success(Unit)
            )
        }
    }

    private fun handleUpdateHomeserver(action: TchapLoginAction.UpdateHomeServer) {
        val homeServerConnectionConfig = homeServerConnectionConfigFactory.create(action.homeServerUrl)
        if (homeServerConnectionConfig == null) {
            // This is invalid
            _viewEvents.post(TchapLoginViewEvents.Failure(Throwable("Unable to create a HomeServerConnectionConfig")))
        } else {
            getLoginFlow(homeServerConnectionConfig)
        }
    }

    private fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig) {
        currentHomeServerConnectionConfig = homeServerConnectionConfig

        currentJob = viewModelScope.launch {
            authenticationService.cancelPendingLoginOrRegistration()

            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Loading()
                )
            }

            val data = try {
                authenticationService.getLoginFlow(homeServerConnectionConfig)
            } catch (failure: Throwable) {
                _viewEvents.post(TchapLoginViewEvents.Failure(failure))
                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Uninitialized
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

            // FIXME We should post a view event here normally?
            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Uninitialized,
                        homeServerUrl = data.homeServerUrl,
                        loginMode = loginMode,
                        loginModeSupportedTypes = data.supportedLoginTypes.toList()
                )
            }
            if ((loginMode == LoginMode.Password && !data.isLoginAndRegistrationSupported)
                    || data.isOutdatedHomeserver) {
                // Notify the UI
                _viewEvents.post(TchapLoginViewEvents.OutdatedHomeserver)
            } else {
                _viewEvents.post(TchapLoginViewEvents.OnLoginFlowRetrieved)
            }
        }
    }
}
