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

package im.vector.riotx.features.login

import arrow.core.Try
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.login.LoginWizard
import im.vector.matrix.android.api.auth.registration.FlowResult
import im.vector.matrix.android.api.auth.registration.RegistrationResult
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.auth.registration.Stage
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.MatrixCallbackDelegate
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.configureAndStart
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.DataSource
import im.vector.riotx.core.utils.PublishDataSource
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import im.vector.riotx.features.session.SessionListener
import timber.log.Timber
import java.util.concurrent.CancellationException

/**
 *
 */
class LoginViewModel @AssistedInject constructor(@Assisted initialState: LoginViewState,
                                                 private val authenticationService: AuthenticationService,
                                                 private val activeSessionHolder: ActiveSessionHolder,
                                                 private val pushRuleTriggerListener: PushRuleTriggerListener,
                                                 private val sessionListener: SessionListener)
    : VectorViewModel<LoginViewState, LoginAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: LoginViewState): LoginViewModel
    }

    companion object : MvRxViewModelFactory<LoginViewModel, LoginViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: LoginViewState): LoginViewModel? {
            val activity: LoginActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.loginViewModelFactory.create(state)
        }
    }

    val currentThreePid: String?
        get() = registrationWizard?.currentThreePid

    // True when login and password are sent with success to the homeserver
    var isRegistrationStarted: Boolean = false
        private set

    // True when email is sent with success to the homeserver
    val isResetPasswordStarted: Boolean
        get() = resetPasswordEmail.isNullOrBlank().not()

    private var registrationWizard: RegistrationWizard? = null
    private var loginWizard: LoginWizard? = null

    var serverType: ServerType = ServerType.MatrixOrg
        private set
    var signMode: SignMode = SignMode.Unknown
        private set
    var resetPasswordEmail: String? = null
        private set

    private var loginConfig: LoginConfig? = null

    private var homeServerConnectionConfig: HomeServerConnectionConfig? = null
    private var currentTask: Cancelable? = null

    private val _viewEvents = PublishDataSource<LoginViewEvents>()
    val viewEvents: DataSource<LoginViewEvents> = _viewEvents

    override fun handle(action: LoginAction) {
        when (action) {
            is LoginAction.UpdateServerType           -> handleUpdateServerType(action)
            is LoginAction.UpdateSignMode             -> handleUpdateSignMode(action)
            is LoginAction.InitWith                   -> handleInitWith(action)
            is LoginAction.UpdateHomeServer           -> handleUpdateHomeserver(action)
            is LoginAction.Login                      -> handleLogin(action)
            is LoginAction.WebLoginSuccess            -> handleWebLoginSuccess(action)
            is LoginAction.ResetPassword              -> handleResetPassword(action)
            is LoginAction.ResetPasswordMailConfirmed -> handleResetPasswordMailConfirmed()
            is LoginAction.RegisterAction             -> handleRegisterAction(action)
            is LoginAction.ResetAction                -> handleResetAction(action)
        }
    }

    private fun handleRegisterAction(action: LoginAction.RegisterAction) {
        when (action) {
            is LoginAction.RegisterWith                 -> handleRegisterWith(action)
            is LoginAction.CaptchaDone                  -> handleCaptchaDone(action)
            is LoginAction.AcceptTerms                  -> handleAcceptTerms()
            is LoginAction.RegisterDummy                -> handleRegisterDummy()
            is LoginAction.AddThreePid                  -> handleAddThreePid(action)
            is LoginAction.SendAgainThreePid            -> handleSendAgainThreePid()
            is LoginAction.ValidateThreePid             -> handleValidateThreePid(action)
            is LoginAction.CheckIfEmailHasBeenValidated -> handleCheckIfEmailHasBeenValidated(action)
            is LoginAction.StopEmailValidationCheck     -> handleStopEmailValidationCheck()
        }
    }

    private fun handleCheckIfEmailHasBeenValidated(action: LoginAction.CheckIfEmailHasBeenValidated) {
        // We do not want the common progress bar to be displayed, so we do not change asyncRegistration value in the state
        currentTask?.cancel()
        currentTask = null
        currentTask = registrationWizard?.checkIfEmailHasBeenValidated(action.delayMillis, registrationCallback)
    }

    private fun handleStopEmailValidationCheck() {
        currentTask?.cancel()
        currentTask = null
    }

    private fun handleValidateThreePid(action: LoginAction.ValidateThreePid) {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.handleValidateThreePid(action.code, registrationCallback)
    }

    private val registrationCallback = object : MatrixCallback<RegistrationResult> {
        override fun onSuccess(data: RegistrationResult) {
            /*
              // Simulate registration disabled
              onFailure(Failure.ServerError(MatrixError(
                      code = MatrixError.FORBIDDEN,
                      message = "Registration is disabled"
              ), 403))
            */

            setState {
                copy(
                        asyncRegistration = Uninitialized
                )
            }

            when (data) {
                is RegistrationResult.Success      -> onSessionCreated(data.session)
                is RegistrationResult.FlowResponse -> onFlowResponse(data.flowResult)
            }
        }

        override fun onFailure(failure: Throwable) {
            if (failure !is CancellationException) {
                _viewEvents.post(LoginViewEvents.RegistrationError(failure))
            }
            setState {
                copy(
                        asyncRegistration = Uninitialized
                )
            }
        }
    }

    private fun handleAddThreePid(action: LoginAction.AddThreePid) {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.addThreePid(action.threePid, object : MatrixCallback<RegistrationResult> {
            override fun onSuccess(data: RegistrationResult) {
                setState {
                    copy(
                            asyncRegistration = Uninitialized
                    )
                }
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(LoginViewEvents.RegistrationError(failure))
                setState {
                    copy(
                            asyncRegistration = Uninitialized
                    )
                }
            }
        })
    }

    private fun handleSendAgainThreePid() {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.sendAgainThreePid(object : MatrixCallback<RegistrationResult> {
            override fun onSuccess(data: RegistrationResult) {
                setState {
                    copy(
                            asyncRegistration = Uninitialized
                    )
                }
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(LoginViewEvents.RegistrationError(failure))
                setState {
                    copy(
                            asyncRegistration = Uninitialized
                    )
                }
            }
        })
    }

    private fun handleAcceptTerms() {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.acceptTerms(registrationCallback)
    }

    private fun handleRegisterDummy() {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.dummy(registrationCallback)
    }

    private fun handleRegisterWith(action: LoginAction.RegisterWith) {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.createAccount(
                action.username,
                action.password,
                action.initialDeviceName,
                object : MatrixCallbackDelegate<RegistrationResult>(registrationCallback) {
                    override fun onSuccess(data: RegistrationResult) {
                        isRegistrationStarted = true
                        // Not sure that this will work:
                        // super.onSuccess(data)
                        registrationCallback.onSuccess(data)
                    }
                })
    }

    private fun handleCaptchaDone(action: LoginAction.CaptchaDone) {
        setState { copy(asyncRegistration = Loading()) }
        currentTask = registrationWizard?.performReCaptcha(action.captchaResponse, registrationCallback)
    }

    private fun handleResetAction(action: LoginAction.ResetAction) {
        // Cancel any request
        currentTask?.cancel()
        currentTask = null

        when (action) {
            LoginAction.ResetLogin          -> {
                isRegistrationStarted = false

                setState {
                    copy(
                            asyncLoginAction = Uninitialized,
                            asyncRegistration = Uninitialized
                    )
                }
            }
            LoginAction.ResetHomeServerUrl  -> {
                homeServerConnectionConfig = null
                registrationWizard = null
                loginWizard = null

                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Uninitialized
                    )
                }
            }
            LoginAction.ResetHomeServerType -> {
                serverType = ServerType.MatrixOrg
            }
            LoginAction.ResetSignMode       -> {
                signMode = SignMode.Unknown
                setState {
                    copy(
                            asyncHomeServerLoginFlowRequest = Uninitialized
                    )
                }
            }
            LoginAction.ResetResetPassword  -> {
                resetPasswordEmail = null
                setState {
                    copy(
                            asyncResetPassword = Uninitialized,
                            asyncResetMailConfirmed = Uninitialized
                    )
                }
            }
        }
    }

    private fun handleUpdateSignMode(action: LoginAction.UpdateSignMode) {
        signMode = action.signMode

        if (signMode == SignMode.SignUp) {
            startRegistrationFlow()
        } else if (signMode == SignMode.SignIn) {
            startAuthenticationFlow()
        }
    }

    private fun handleUpdateServerType(action: LoginAction.UpdateServerType) {
        serverType = action.serverType
    }

    private fun handleInitWith(action: LoginAction.InitWith) {
        loginConfig = action.loginConfig
    }

    private fun handleResetPassword(action: LoginAction.ResetPassword) {
        val safeLoginWizard = loginWizard

        if (safeLoginWizard == null) {
            setState {
                copy(
                        asyncResetPassword = Fail(Throwable("Bad configuration"))
                )
            }
        } else {
            setState {
                copy(
                        asyncResetPassword = Loading()
                )
            }

            currentTask = safeLoginWizard.resetPassword(action.email, action.newPassword, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    resetPasswordEmail = action.email

                    setState {
                        copy(
                                asyncResetPassword = Success(data)
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    // TODO Handled JobCancellationException
                    setState {
                        copy(
                                asyncResetPassword = Fail(failure)
                        )
                    }
                }
            })
        }
    }

    private fun handleResetPasswordMailConfirmed() {
        val safeLoginWizard = loginWizard

        if (safeLoginWizard == null) {
            setState {
                copy(
                        asyncResetMailConfirmed = Fail(Throwable("Bad configuration"))
                )
            }
        } else {
            setState {
                copy(
                        asyncResetMailConfirmed = Loading()
                )
            }

            currentTask = safeLoginWizard.resetPasswordMailConfirmed(object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    resetPasswordEmail = null

                    setState {
                        copy(
                                asyncResetMailConfirmed = Success(data)
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    // TODO Handled JobCancellationException
                    setState {
                        copy(
                                asyncResetMailConfirmed = Fail(failure)
                        )
                    }
                }
            })
        }
    }

    private fun handleLogin(action: LoginAction.Login) {
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

            currentTask = safeLoginWizard.login(
                    action.login,
                    action.password,
                    action.initialDeviceName,
                    object : MatrixCallback<Session> {
                        override fun onSuccess(data: Session) {
                            onSessionCreated(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            // TODO Handled JobCancellationException
                            setState {
                                copy(
                                        asyncLoginAction = Fail(failure)
                                )
                            }
                        }
                    })
        }
    }

    private fun startRegistrationFlow() {
        setState {
            copy(
                    asyncRegistration = Loading()
            )
        }

        registrationWizard = authenticationService.getOrCreateRegistrationWizard()

        currentTask = registrationWizard?.getRegistrationFlow(registrationCallback)
    }

    private fun startAuthenticationFlow() {
        loginWizard = authenticationService.createLoginWizard()
    }

    private fun onFlowResponse(flowResult: FlowResult) {
        // If dummy stage is mandatory, and password is already sent, do the dummy stage now
        if (isRegistrationStarted
                && flowResult.missingStages.any { it is Stage.Dummy && it.mandatory }) {
            handleRegisterDummy()
        } else {
            // Notify the user
            _viewEvents.post(LoginViewEvents.RegistrationFlowResult(flowResult))
        }
    }

    private fun onSessionCreated(session: Session) {
        activeSessionHolder.setActiveSession(session)
        session.configureAndStart(pushRuleTriggerListener, sessionListener)
        setState {
            copy(
                    asyncLoginAction = Success(Unit)
            )
        }
    }

    private fun handleWebLoginSuccess(action: LoginAction.WebLoginSuccess) {
        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            // Should not happen
            Timber.w("homeServerConnectionConfig is null")
        } else {
            authenticationService.createSessionFromSso(homeServerConnectionConfigFinal, action.credentials, object : MatrixCallback<Session> {
                override fun onSuccess(data: Session) {
                    onSessionCreated(data)
                }

                override fun onFailure(failure: Throwable) = setState {
                    copy(asyncLoginAction = Fail(failure))
                }
            })
        }
    }

    private fun handleUpdateHomeserver(action: LoginAction.UpdateHomeServer) = withState { state ->
        var newConfig: HomeServerConnectionConfig? = null
        Try {
            val homeServerUri = action.homeServerUrl
            newConfig = HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(homeServerUri)
                    .build()
        }

        // Do not retry if we already have flows for this config -> causes infinite focus loop
        if (newConfig?.homeServerUri?.toString() == homeServerConnectionConfig?.homeServerUri?.toString()
                && state.asyncHomeServerLoginFlowRequest is Success) return@withState

        currentTask?.cancel()
        currentTask = null
        homeServerConnectionConfig = newConfig
        loginWizard = null
        registrationWizard = null

        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            // This is invalid
            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Fail(Throwable("Bad format"))
                )
            }
        } else {
            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Loading()
                )
            }

            currentTask = authenticationService.getLoginFlow(homeServerConnectionConfigFinal, object : MatrixCallback<LoginFlowResponse> {
                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                asyncHomeServerLoginFlowRequest = Fail(failure)
                        )
                    }
                }

                override fun onSuccess(data: LoginFlowResponse) {
                    val loginMode = when {
                        // SSO login is taken first
                        data.flows.any { it.type == LoginFlowTypes.SSO }      -> LoginMode.Sso
                        data.flows.any { it.type == LoginFlowTypes.PASSWORD } -> LoginMode.Password
                        else                                                  -> LoginMode.Unsupported(data.flows.mapNotNull { it.type }.toList())
                    }

                    setState {
                        copy(
                                asyncHomeServerLoginFlowRequest = Success(loginMode)
                        )
                    }
                }
            })
        }
    }

    override fun onCleared() {
        super.onCleared()

        currentTask?.cancel()
    }

    fun getInitialHomeServerUrl(): String? {
        return loginConfig?.homeServerUrl
    }

    fun getHomeServerUrl(): String {
        return homeServerConnectionConfig?.homeServerUri?.toString() ?: ""
    }

    /**
     * Ex: "https://matrix.org/" -> "matrix.org"
     */
    fun getHomeServerUrlSimple(): String {
        return getHomeServerUrl()
                .substringAfter("://")
                .trim { it == '/' }
    }
}
