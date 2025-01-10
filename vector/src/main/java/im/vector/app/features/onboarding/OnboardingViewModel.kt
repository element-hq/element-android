/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.content.Context
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.config.Config
import im.vector.app.config.SunsetConfig
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.cancelCurrentOnSet
import im.vector.app.core.extensions.inferNoConnectivity
import im.vector.app.core.extensions.isMatrixId
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.extensions.vectorStore
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.session.ConfigureAndStartSessionUseCase
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.features.VectorFeatures
import im.vector.app.features.VectorOverrides
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toTrackingValue
import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.login.HomeServerConnectionConfigFactory
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.mdm.MdmData
import im.vector.app.features.mdm.MdmService
import im.vector.app.features.onboarding.OnboardingAction.AuthenticateAction
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.auth.SSOAction
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationAvailability
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isHomeserverConnectionError
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import org.matrix.android.sdk.api.failure.isUnrecognisedCertificate
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 *
 */
class OnboardingViewModel @AssistedInject constructor(
        @Assisted initialState: OnboardingViewState,
        private val applicationContext: Context,
        private val authenticationService: AuthenticationService,
        private val activeSessionHolder: ActiveSessionHolder,
        private val homeServerConnectionConfigFactory: HomeServerConnectionConfigFactory,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider,
        private val homeServerHistoryService: HomeServerHistoryService,
        private val vectorFeatures: VectorFeatures,
        private val analyticsTracker: AnalyticsTracker,
        private val uriFilenameResolver: UriFilenameResolver,
        private val directLoginUseCase: DirectLoginUseCase,
        private val startAuthenticationFlowUseCase: StartAuthenticationFlowUseCase,
        private val vectorOverrides: VectorOverrides,
        private val registrationActionHandler: RegistrationActionHandler,
        private val sdkIntProvider: BuildVersionSdkIntProvider,
        private val configureAndStartSessionUseCase: ConfigureAndStartSessionUseCase,
        mdmService: MdmService,
) : VectorViewModel<OnboardingViewState, OnboardingAction, OnboardingViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<OnboardingViewModel, OnboardingViewState> {
        override fun create(initialState: OnboardingViewState): OnboardingViewModel
    }

    companion object : MavericksViewModelFactory<OnboardingViewModel, OnboardingViewState> by hiltMavericksViewModelFactory()

    init {
        getKnownCustomHomeServersUrls()
        observeDataStore()
    }

    private fun getKnownCustomHomeServersUrls() {
        setState {
            copy(knownCustomHomeServersUrls = homeServerHistoryService.getKnownServersUrls())
        }
    }

    private fun observeDataStore() = viewModelScope.launch {
        vectorOverrides.forceLoginFallback.setOnEach { isForceLoginFallbackEnabled ->
            copy(isForceLoginFallbackEnabled = isForceLoginFallbackEnabled)
        }
    }

    private val matrixOrgUrl = stringProvider.getString(im.vector.app.config.R.string.matrix_org_server_url).ensureTrailingSlash()
    private val defaultHomeserverUrl = mdmService.getData(MdmData.DefaultHomeserverUrl, matrixOrgUrl)

    private val registrationWizard: RegistrationWizard
        get() = authenticationService.getRegistrationWizard()

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
        get() = authenticationService.isRegistrationStarted()

    private val loginWizard: LoginWizard
        get() = authenticationService.getLoginWizard()

    private var loginConfig: LoginConfig? = null

    private var emailVerificationPollingJob: Job? by cancelCurrentOnSet()
    private var currentJob: Job? by cancelCurrentOnSet()

    private val trustedFingerprints = mutableListOf<Fingerprint>()

    override fun handle(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SplashAction -> handleSplashAction(action)
            is OnboardingAction.UpdateUseCase -> handleUpdateUseCase(action)
            OnboardingAction.ResetUseCase -> resetUseCase()
            is OnboardingAction.UpdateServerType -> handleUpdateServerType(action)
            is OnboardingAction.UpdateSignMode -> handleUpdateSignMode(action)
            is OnboardingAction.InitWith -> handleInitWith(action)
            is OnboardingAction.HomeServerChange -> handleHomeserverChange(action)
            is OnboardingAction.UserNameEnteredAction -> handleUserNameEntered(action)
            is AuthenticateAction -> handleAuthenticateAction(action)
            is OnboardingAction.LoginWithToken -> handleLoginWithToken(action)
            is OnboardingAction.WebLoginSuccess -> handleWebLoginSuccess(action)
            is OnboardingAction.ResetPassword -> handleResetPassword(action)
            OnboardingAction.ResendResetPassword -> handleResendResetPassword()
            is OnboardingAction.ConfirmNewPassword -> handleResetPasswordConfirmed(action)
            is OnboardingAction.ResetPasswordMailConfirmed -> handleResetPasswordMailConfirmed()
            is OnboardingAction.PostRegisterAction -> handleRegisterAction(action.registerAction)
            is OnboardingAction.ResetAction -> handleResetAction(action)
            is OnboardingAction.UserAcceptCertificate -> handleUserAcceptCertificate(action)
            OnboardingAction.ClearHomeServerHistory -> handleClearHomeServerHistory()
            is OnboardingAction.UpdateDisplayName -> updateDisplayName(action.displayName)
            OnboardingAction.UpdateDisplayNameSkipped -> handleDisplayNameStepComplete()
            OnboardingAction.UpdateProfilePictureSkipped -> completePersonalization()
            OnboardingAction.PersonalizeProfile -> handlePersonalizeProfile()
            is OnboardingAction.ProfilePictureSelected -> handleProfilePictureSelected(action)
            OnboardingAction.SaveSelectedProfilePicture -> updateProfilePicture()
            is OnboardingAction.PostViewEvent -> _viewEvents.post(action.viewEvent)
            OnboardingAction.StopEmailValidationCheck -> cancelWaitForEmailValidation()
        }
    }

    private fun handleUserNameEntered(action: OnboardingAction.UserNameEnteredAction) {
        when (action) {
            is OnboardingAction.UserNameEnteredAction.Login -> maybeUpdateHomeserver(action.userId)
            is OnboardingAction.UserNameEnteredAction.Registration -> maybeUpdateHomeserver(action.userId, continuation = { userName ->
                checkUserNameAvailability(userName)
            })
        }
    }

    private fun maybeUpdateHomeserver(userNameOrMatrixId: String, continuation: suspend (String) -> Unit = {}) {
        val isFullMatrixId = MatrixPatterns.isUserId(userNameOrMatrixId)
        if (isFullMatrixId) {
            val domain = userNameOrMatrixId.getServerName().substringBeforeLast(":").ensureProtocol()
            handleHomeserverChange(OnboardingAction.HomeServerChange.EditHomeServer(domain), postAction = {
                val userName = MatrixPatterns.extractUserNameFromId(userNameOrMatrixId) ?: throw IllegalStateException("unexpected non matrix id")
                continuation(userName)
            })
        } else {
            currentJob = viewModelScope.launch { continuation(userNameOrMatrixId) }
        }
    }

    private suspend fun checkUserNameAvailability(userName: String) {
        runCatching { registrationWizard.registrationAvailable(userName) }.fold(
                onSuccess = { result ->
                    when (result) {
                        RegistrationAvailability.Available -> {
                            setState {
                                copy(
                                        registrationState = RegistrationState(
                                                isUserNameAvailable = true,
                                                selectedMatrixId = when {
                                                    userName.isMatrixId() -> userName
                                                    else -> "@$userName:${selectedHomeserver.userFacingUrl.toReducedUrl()}"
                                                },
                                        )
                                )
                            }
                        }

                        is RegistrationAvailability.NotAvailable -> {
                            _viewEvents.post(OnboardingViewEvents.Failure(result.failure))
                        }
                    }
                },
                onFailure = {
                    _viewEvents.post(OnboardingViewEvents.Failure(it))
                }
        )
    }

    private fun handleAuthenticateAction(action: AuthenticateAction) {
        when (action) {
            is AuthenticateAction.Register -> handleRegisterWith(action.username, action.password, action.initialDeviceName)
            is AuthenticateAction.RegisterWithMatrixId -> handleRegisterWith(
                    MatrixPatterns.extractUserNameFromId(action.matrixId) ?: throw IllegalStateException("unexpected non matrix id"),
                    action.password,
                    action.initialDeviceName
            )
            is AuthenticateAction.Login -> handleLogin(action)
            is AuthenticateAction.LoginDirect -> handleDirectLogin(action, homeServerConnectionConfig = null)
        }
    }

    private fun handleSplashAction(action: OnboardingAction.SplashAction) {
        setState { copy(onboardingFlow = action.onboardingFlow) }
        continueToPageAfterSplash(action.onboardingFlow)
    }

    private fun continueToPageAfterSplash(onboardingFlow: OnboardingFlow) {
        when (onboardingFlow) {
            OnboardingFlow.SignUp -> {
                _viewEvents.post(
                        if (vectorFeatures.isOnboardingUseCaseEnabled()) {
                            OnboardingViewEvents.OpenUseCaseSelection
                        } else {
                            OnboardingViewEvents.OpenServerSelection
                        }
                )
            }
            OnboardingFlow.SignIn -> when {
                vectorFeatures.isOnboardingCombinedLoginEnabled() -> {
                    handle(OnboardingAction.HomeServerChange.SelectHomeServer(deeplinkOrDefaultHomeserverUrl()))
                }
                else -> openServerSelectionOrDeeplinkToOther()
            }

            OnboardingFlow.SignInSignUp -> openServerSelectionOrDeeplinkToOther()
        }
    }

    private fun openServerSelectionOrDeeplinkToOther() {
        when (loginConfig) {
            null -> _viewEvents.post(OnboardingViewEvents.OpenServerSelection)
            else -> handleHomeserverChange(OnboardingAction.HomeServerChange.SelectHomeServer(deeplinkOrDefaultHomeserverUrl()), ServerType.Other)
        }
    }

    private fun handleUserAcceptCertificate(action: OnboardingAction.UserAcceptCertificate) {
        // It happens when we get the login flow, or during direct authentication.
        // So alter the homeserver config and retrieve again the login flow
        trustedFingerprints.add(action.fingerprint)
        when (action.retryAction) {
            is OnboardingAction.HomeServerChange -> handleHomeserverChange(action.retryAction, fingerprints = trustedFingerprints)
            is AuthenticateAction.LoginDirect ->
                handleDirectLogin(
                        action.retryAction,
                        // Will be replaced by the task
                        homeServerConnectionConfigFactory.create("https://dummy.org", trustedFingerprints)
                )
            else -> Unit
        }
    }

    private fun rememberHomeServer(homeServerUrl: String) {
        homeServerHistoryService.addHomeServerToHistory(homeServerUrl)
        getKnownCustomHomeServersUrls()
    }

    private fun handleClearHomeServerHistory() {
        homeServerHistoryService.clearHistory()
        getKnownCustomHomeServersUrls()
    }

    private fun handleLoginWithToken(action: OnboardingAction.LoginWithToken) {
        val safeLoginWizard = loginWizard
        setState { copy(isLoading = true) }

        currentJob = viewModelScope.launch {
            try {
                val result = safeLoginWizard.loginWithToken(action.loginToken)
                onSessionCreated(result, authenticationDescription = AuthenticationDescription.Login)
            } catch (failure: Throwable) {
                setState { copy(isLoading = false) }
                _viewEvents.post(OnboardingViewEvents.Failure(failure))
            }
        }
    }

    private fun handleRegisterAction(action: RegisterAction) {
        val job = viewModelScope.launch {
            if (action.hasLoadingState()) {
                setState { copy(isLoading = true) }
            }
            internalRegisterAction(action)
            setState { copy(isLoading = false) }
        }

        // Allow email verification polling to coexist with other jobs
        when (action) {
            is RegisterAction.CheckIfEmailHasBeenValidated -> emailVerificationPollingJob = job
            else -> currentJob = job
        }
    }

    private suspend fun internalRegisterAction(action: RegisterAction, overrideNextStage: (() -> Unit)? = null) {
        runCatching { registrationActionHandler.processAction(awaitState().selectedHomeserver, action) }.fold(
                onSuccess = {
                    when (it) {
                        RegistrationActionHandler.Result.Ignored -> {
                            // do nothing
                        }
                        is RegistrationActionHandler.Result.NextStage -> {
                            overrideNextStage?.invoke() ?: _viewEvents.post(OnboardingViewEvents.DisplayRegistrationStage(it.stage))
                        }
                        is RegistrationActionHandler.Result.RegistrationComplete -> onSessionCreated(
                                it.session,
                                authenticationDescription = awaitState().selectedAuthenticationState.description
                                        ?: AuthenticationDescription.Register(AuthenticationDescription.AuthenticationType.Other)
                        )
                        RegistrationActionHandler.Result.StartRegistration -> {
                            overrideNextStage?.invoke() ?: _viewEvents.post(OnboardingViewEvents.DisplayStartRegistration)
                        }
                        RegistrationActionHandler.Result.UnsupportedStage -> _viewEvents.post(OnboardingViewEvents.DisplayRegistrationFallback)
                        is RegistrationActionHandler.Result.SendEmailSuccess -> {
                            _viewEvents.post(OnboardingViewEvents.OnSendEmailSuccess(it.email, isRestoredSession = false))
                            setState { copy(registrationState = registrationState.copy(email = it.email)) }
                        }
                        is RegistrationActionHandler.Result.SendMsisdnSuccess -> _viewEvents.post(OnboardingViewEvents.OnSendMsisdnSuccess(it.msisdn.msisdn))
                        is RegistrationActionHandler.Result.Error -> _viewEvents.post(OnboardingViewEvents.Failure(it.cause))
                        RegistrationActionHandler.Result.MissingNextStage -> {
                            _viewEvents.post(OnboardingViewEvents.Failure(IllegalStateException("No next registration stage found")))
                        }
                    }
                },
                onFailure = {
                    if (it !is CancellationException) {
                        _viewEvents.post(OnboardingViewEvents.Failure(it))
                    }
                }
        )
    }

    private fun handleRegisterWith(userName: String, password: String, initialDeviceName: String) {
        setState {
            val authDescription = AuthenticationDescription.Register(AuthenticationDescription.AuthenticationType.Password)
            copy(selectedAuthenticationState = SelectedAuthenticationState(authDescription))
        }
        reAuthHelper.data = password
        handleRegisterAction(
                RegisterAction.CreateAccount(
                        userName,
                        password,
                        initialDeviceName
                )
        )
    }

    private fun handleResetAction(action: OnboardingAction.ResetAction) {
        // Cancel any request
        currentJob = null
        emailVerificationPollingJob = null

        when (action) {
            OnboardingAction.ResetHomeServerType -> {
                setState { copy(serverType = ServerType.Unknown) }
            }
            OnboardingAction.ResetHomeServerUrl -> {
                viewModelScope.launch {
                    authenticationService.reset()
                    setState {
                        copy(
                                isLoading = false,
                                selectedHomeserver = SelectedHomeserverState(),
                        )
                    }
                }
            }
            OnboardingAction.ResetSignMode -> {
                setState {
                    copy(
                            isLoading = false,
                            signMode = SignMode.Unknown,
                    )
                }
            }
            OnboardingAction.ResetAuthenticationAttempt -> {
                viewModelScope.launch {
                    authenticationService.cancelPendingLoginOrRegistration()
                    setState {
                        copy(
                                isLoading = false,
                                registrationState = RegistrationState(),
                        )
                    }
                }
            }
            OnboardingAction.ResetResetPassword -> {
                setState {
                    copy(
                            isLoading = false,
                            resetState = ResetState()
                    )
                }
            }
            OnboardingAction.ResetDeeplinkConfig -> loginConfig = null
            OnboardingAction.ResetSelectedRegistrationUserName -> {
                setState {
                    copy(registrationState = RegistrationState())
                }
            }
        }
    }

    private fun handleUpdateSignMode(action: OnboardingAction.UpdateSignMode) {
        updateSignMode(action.signMode)
        when (action.signMode) {
            SignMode.SignUp -> handleRegisterAction(RegisterAction.StartRegistration)
            SignMode.SignIn -> startAuthenticationFlow()
            SignMode.SignInWithMatrixId -> _viewEvents.post(OnboardingViewEvents.OnSignModeSelected(SignMode.SignInWithMatrixId))
            SignMode.Unknown -> Unit
        }
    }

    private fun updateSignMode(signMode: SignMode) {
        setState { copy(signMode = signMode) }
    }

    private fun handleUpdateUseCase(action: OnboardingAction.UpdateUseCase) {
        setState { copy(useCase = action.useCase) }
        when (vectorFeatures.isOnboardingCombinedRegisterEnabled()) {
            true -> handle(OnboardingAction.HomeServerChange.SelectHomeServer(deeplinkOrDefaultHomeserverUrl()))
            false -> _viewEvents.post(OnboardingViewEvents.OpenServerSelection)
        }
    }

    private fun deeplinkOrDefaultHomeserverUrl() = loginConfig?.homeServerUrl?.ensureProtocol() ?: defaultHomeserverUrl

    private fun resetUseCase() {
        setState { copy(useCase = null) }
    }

    private fun handleUpdateServerType(action: OnboardingAction.UpdateServerType) {
        setState {
            copy(
                    serverType = action.serverType
            )
        }

        when (action.serverType) {
            ServerType.Unknown -> Unit /* Should not happen */
            ServerType.MatrixOrg ->
                // Request login flow here
                handle(OnboardingAction.HomeServerChange.SelectHomeServer(matrixOrgUrl))
            ServerType.EMS,
            ServerType.Other -> _viewEvents.post(OnboardingViewEvents.OnServerSelectionDone(action.serverType))
        }
    }

    private fun handleInitWith(action: OnboardingAction.InitWith) {
        loginConfig = action.loginConfig
    }

    private fun handleResetPassword(action: OnboardingAction.ResetPassword) {
        startResetPasswordFlow(action.email) {
            setState { copy(isLoading = false, resetState = createResetState(action, selectedHomeserver)) }
            _viewEvents.post(OnboardingViewEvents.OnResetPasswordEmailConfirmationSent(action.email))
        }
    }

    private fun createResetState(action: OnboardingAction.ResetPassword, selectedHomeserverState: SelectedHomeserverState) = ResetState(
            email = action.email,
            newPassword = action.newPassword,
            supportsLogoutAllDevices = selectedHomeserverState.isLogoutDevicesSupported
    )

    private fun handleResendResetPassword() {
        withState { state ->
            val resetState = state.resetState
            when (resetState.email) {
                null -> _viewEvents.post(OnboardingViewEvents.Failure(IllegalStateException("Developer error - No reset email has been set")))
                else -> {
                    startResetPasswordFlow(resetState.email) {
                        setState { copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun startResetPasswordFlow(email: String, onSuccess: suspend () -> Unit) {
        val safeLoginWizard = loginWizard
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            runCatching { safeLoginWizard.resetPassword(email) }.fold(
                    onSuccess = { onSuccess.invoke() },
                    onFailure = {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(OnboardingViewEvents.Failure(it))
                    }
            )
        }
    }

    private fun handleResetPasswordConfirmed(action: OnboardingAction.ConfirmNewPassword) {
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            confirmPasswordReset(action.newPassword, action.signOutAllDevices)
        }
    }

    private fun handleResetPasswordMailConfirmed() {
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            val resetState = awaitState().resetState
            when (val newPassword = resetState.newPassword) {
                null -> {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(IllegalStateException("Developer error - No new password has been set")))
                }
                else -> confirmPasswordReset(newPassword, logoutAllDevices = true)
            }
        }
    }

    private suspend fun confirmPasswordReset(newPassword: String, logoutAllDevices: Boolean) {
        runCatching { loginWizard.resetPasswordMailConfirmed(newPassword, logoutAllDevices = logoutAllDevices) }.fold(
                onSuccess = {
                    setState { copy(isLoading = false, resetState = ResetState()) }
                    val nextEvent = when {
                        vectorFeatures.isOnboardingCombinedLoginEnabled() -> OnboardingViewEvents.OnResetPasswordComplete
                        else -> OnboardingViewEvents.OpenResetPasswordComplete
                    }
                    _viewEvents.post(nextEvent)
                },
                onFailure = {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(it))
                }
        )
    }

    private fun handleDirectLogin(action: AuthenticateAction.LoginDirect, homeServerConnectionConfig: HomeServerConnectionConfig?) {
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            directLoginUseCase.execute(action, homeServerConnectionConfig).fold(
                    onSuccess = { onSessionCreated(it, authenticationDescription = AuthenticationDescription.Login) },
                    onFailure = { error ->
                        setState { copy(isLoading = false) }
                        when {
                            error.isUnrecognisedCertificate() -> {
                                _viewEvents.post(
                                        OnboardingViewEvents.UnrecognisedCertificateFailure(
                                                retryAction = action,
                                                cause = error as Failure.UnrecognizedCertificateFailure
                                        )
                                )
                            }
                            else -> _viewEvents.post(OnboardingViewEvents.Failure(error))
                        }
                    }
            )
        }
    }

    private fun handleLogin(action: AuthenticateAction.Login) {
        val safeLoginWizard = loginWizard
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            try {
                val result = safeLoginWizard.login(
                        action.username,
                        action.password,
                        action.initialDeviceName
                )
                reAuthHelper.data = action.password
                onSessionCreated(result, authenticationDescription = AuthenticationDescription.Login)
            } catch (failure: Throwable) {
                setState { copy(isLoading = false) }
                _viewEvents.post(OnboardingViewEvents.Failure(failure))
            }
        }
    }

    private fun startAuthenticationFlow() {
        // Ensure Wizard is ready
        loginWizard

        _viewEvents.post(OnboardingViewEvents.OnSignModeSelected(SignMode.SignIn))
    }

    private suspend fun onSessionCreated(session: Session, authenticationDescription: AuthenticationDescription) {
        val state = awaitState()
        state.useCase?.let { useCase ->
            session.vectorStore(applicationContext).setUseCase(useCase)
            analyticsTracker.updateUserProperties(UserProperties(ftueUseCaseSelection = useCase.toTrackingValue()))
        }
        activeSessionHolder.setActiveSession(session)

        authenticationService.reset()
        configureAndStartSessionUseCase.execute(session)

        when (authenticationDescription) {
            is AuthenticationDescription.Register -> {
                val personalizationState = createPersonalizationState(session, state)
                setState {
                    copy(isLoading = false, personalizationState = personalizationState)
                }
                _viewEvents.post(OnboardingViewEvents.OnAccountCreated)
            }
            AuthenticationDescription.Login -> {
                setState { copy(isLoading = false, selectedAuthenticationState = SelectedAuthenticationState(authenticationDescription)) }
                awaitState()
                _viewEvents.post(OnboardingViewEvents.OnAccountSignedIn)
            }
        }
    }

    private suspend fun createPersonalizationState(session: Session, state: OnboardingViewState): PersonalizationState {
        return when {
            vectorFeatures.isOnboardingPersonalizeEnabled() -> {
                val homeServerCapabilities = session.homeServerCapabilitiesService().getHomeServerCapabilities()
                val capabilityOverrides = vectorOverrides.forceHomeserverCapabilities?.firstOrNull()
                state.personalizationState.copy(
                        userId = session.myUserId,
                        displayName = state.registrationState.selectedMatrixId?.let { MatrixPatterns.extractUserNameFromId(it) },
                        supportsChangingDisplayName = capabilityOverrides?.canChangeDisplayName ?: homeServerCapabilities.canChangeDisplayName,
                        supportsChangingProfilePicture = capabilityOverrides?.canChangeAvatar ?: homeServerCapabilities.canChangeAvatar
                )
            }
            else -> state.personalizationState
        }
    }

    private fun handleWebLoginSuccess(action: OnboardingAction.WebLoginSuccess) = withState { state ->
        val homeServerConnectionConfigFinal = homeServerConnectionConfigFactory.create(state.selectedHomeserver.upstreamUrl)

        if (homeServerConnectionConfigFinal == null) {
            // Should not happen
            Timber.w("homeServerConnectionConfig is null")
        } else {
            currentJob = viewModelScope.launch {
                try {
                    val result = authenticationService.createSessionFromSso(homeServerConnectionConfigFinal, action.credentials)
                    onSessionCreated(result, authenticationDescription = AuthenticationDescription.Login)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun handleHomeserverChange(
            action: OnboardingAction.HomeServerChange,
            serverTypeOverride: ServerType? = null,
            fingerprints: List<Fingerprint>? = null,
            postAction: suspend () -> Unit = {},
    ) {
        val homeServerConnectionConfig = homeServerConnectionConfigFactory.create(action.homeServerUrl, fingerprints)
        if (homeServerConnectionConfig == null) {
            // This is invalid
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Unable to create a HomeServerConnectionConfig")))
        } else {
            startAuthenticationFlow(action, homeServerConnectionConfig, serverTypeOverride, suspend {
                postAction()
            })
        }
    }

    private fun startAuthenticationFlow(
            trigger: OnboardingAction.HomeServerChange,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            serverTypeOverride: ServerType?,
            postAction: suspend () -> Unit = {},
    ) {
        currentJob = viewModelScope.launch {
            setState { copy(isLoading = true) }
            runCatching { startAuthenticationFlowUseCase.execute(homeServerConnectionConfig) }.fold(
                    onSuccess = {
                        onAuthenticationStartedSuccess(trigger, homeServerConnectionConfig, it, serverTypeOverride)
                        postAction()
                    },
                    onFailure = { onAuthenticationStartError(it, trigger) }
            )
            setState { copy(isLoading = false) }
        }
    }

    private fun onAuthenticationStartError(error: Throwable, trigger: OnboardingAction.HomeServerChange) {
        when {
            error.isHomeserverUnavailable() && applicationContext.inferNoConnectivity(sdkIntProvider) -> _viewEvents.post(OnboardingViewEvents.Failure(error))
            isUnableToSelectServer(error, trigger) -> {
                withState { state ->
                    when {
                        canEditServerSelectionError(state) -> handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.EditServerSelection))
                        else -> _viewEvents.post(OnboardingViewEvents.Failure(error))
                    }
                }
            }
            error.isUnrecognisedCertificate() -> {
                _viewEvents.post(OnboardingViewEvents.UnrecognisedCertificateFailure(trigger, error as Failure.UnrecognizedCertificateFailure))
            }
            else -> _viewEvents.post(OnboardingViewEvents.Failure(error))
        }
    }

    private fun canEditServerSelectionError(state: OnboardingViewState) =
            (state.onboardingFlow == OnboardingFlow.SignIn && vectorFeatures.isOnboardingCombinedLoginEnabled()) ||
                    (state.onboardingFlow == OnboardingFlow.SignUp && vectorFeatures.isOnboardingCombinedRegisterEnabled())

    private fun isUnableToSelectServer(error: Throwable, trigger: OnboardingAction.HomeServerChange) =
            trigger is OnboardingAction.HomeServerChange.SelectHomeServer && error.isHomeserverConnectionError()

    private suspend fun onAuthenticationStartedSuccess(
            trigger: OnboardingAction.HomeServerChange,
            config: HomeServerConnectionConfig,
            authResult: StartAuthenticationResult,
            serverTypeOverride: ServerType?
    ) {
        rememberHomeServer(config.homeServerUri.toString())
        if (authResult.isHomeserverOutdated) {
            _viewEvents.post(OnboardingViewEvents.OutdatedHomeserver)
        }

        when (trigger) {
            is OnboardingAction.HomeServerChange.SelectHomeServer -> {
                onHomeServerSelected(config, serverTypeOverride, authResult)
            }
            is OnboardingAction.HomeServerChange.EditHomeServer -> {
                onHomeServerEdited(config, serverTypeOverride, authResult)
            }
        }
    }

    private suspend fun onHomeServerSelected(config: HomeServerConnectionConfig, serverTypeOverride: ServerType?, authResult: StartAuthenticationResult) {
        updateServerSelection(config, serverTypeOverride, authResult)
        if (authResult.selectedHomeserver.preferredLoginMode.supportsSignModeScreen()) {
            when (awaitState().onboardingFlow) {
                OnboardingFlow.SignIn -> {
                    updateSignMode(SignMode.SignIn)
                    when (vectorFeatures.isOnboardingCombinedLoginEnabled()) {
                        true -> _viewEvents.post(OnboardingViewEvents.OpenCombinedLogin)
                        false -> _viewEvents.post(OnboardingViewEvents.OnSignModeSelected(SignMode.SignIn))
                    }
                }
                OnboardingFlow.SignUp -> {
                    updateSignMode(SignMode.SignUp)
                    if (authResult.selectedHomeserver.hasOidcCompatibilityFlow && Config.sunsetConfig is SunsetConfig.Enabled) {
                        // Navigate to the screen to create an account, it will show the error
                        setState { copy(isLoading = false) }
                        _viewEvents.post(OnboardingViewEvents.OpenCombinedRegister)
                    } else {
                        internalRegisterAction(RegisterAction.StartRegistration)
                    }
                }
                OnboardingFlow.SignInSignUp,
                null -> {
                    _viewEvents.post(OnboardingViewEvents.OnLoginFlowRetrieved)
                }
            }
        } else {
            _viewEvents.post(OnboardingViewEvents.OnLoginFlowRetrieved)
        }
    }

    private suspend fun onHomeServerEdited(config: HomeServerConnectionConfig, serverTypeOverride: ServerType?, authResult: StartAuthenticationResult) {
        when (awaitState().onboardingFlow) {
            OnboardingFlow.SignUp -> {
                if (authResult.selectedHomeserver.hasOidcCompatibilityFlow && Config.sunsetConfig is SunsetConfig.Enabled) {
                    // An error is displayed now
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(MasSupportRequiredException()))
                } else {
                    internalRegisterAction(RegisterAction.StartRegistration) {
                        updateServerSelection(config, serverTypeOverride, authResult)
                        _viewEvents.post(OnboardingViewEvents.OnHomeserverEdited)
                    }
                }
            }
            OnboardingFlow.SignIn -> {
                updateServerSelection(config, serverTypeOverride, authResult)
                _viewEvents.post(OnboardingViewEvents.OnHomeserverEdited)
            }
            else -> throw IllegalArgumentException("developer error")
        }
    }

    private fun updateServerSelection(config: HomeServerConnectionConfig, serverTypeOverride: ServerType?, authResult: StartAuthenticationResult) {
        setState {
            copy(
                    serverType = alignServerTypeAfterSubmission(config, serverTypeOverride),
                    selectedHomeserver = authResult.selectedHomeserver,
            )
        }
    }

    /**
     * If user has entered https://matrix.org, ensure that server type is ServerType.MatrixOrg.
     * It is also useful to set the value again in the case of a certificate error on matrix.org.
     **/
    private fun OnboardingViewState.alignServerTypeAfterSubmission(config: HomeServerConnectionConfig, serverTypeOverride: ServerType?): ServerType {
        return if (config.homeServerUri.toString() == matrixOrgUrl) {
            ServerType.MatrixOrg
        } else {
            serverTypeOverride ?: serverType
        }
    }

    fun getInitialHomeServerUrl(): String? {
        return loginConfig?.homeServerUrl
    }

    fun getDefaultHomeserverUrl() = defaultHomeserverUrl

    fun fetchSsoUrl(redirectUrl: String, deviceId: String?, provider: SsoIdentityProvider?, action: SSOAction): String? {
        setState {
            val authDescription = AuthenticationDescription.Register(provider.toAuthenticationType())
            copy(selectedAuthenticationState = SelectedAuthenticationState(authDescription))
        }
        return authenticationService.getSsoUrl(redirectUrl, deviceId, provider?.id, action)
    }

    fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String? {
        return authenticationService.getFallbackUrl(forSignIn, deviceId)
    }

    private fun updateDisplayName(displayName: String) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            val activeSession = activeSessionHolder.getActiveSession()
            try {
                activeSession.profileService().setDisplayName(activeSession.myUserId, displayName)
                setState {
                    copy(
                            isLoading = false,
                            personalizationState = personalizationState.copy(displayName = displayName)
                    )
                }
                handleDisplayNameStepComplete()
            } catch (error: Throwable) {
                setState { copy(isLoading = false) }
                _viewEvents.post(OnboardingViewEvents.Failure(error))
            }
        }
    }

    private fun handlePersonalizeProfile() {
        withPersonalisationState {
            when {
                it.supportsChangingDisplayName -> _viewEvents.post(OnboardingViewEvents.OnChooseDisplayName)
                it.supportsChangingProfilePicture -> _viewEvents.post(OnboardingViewEvents.OnChooseProfilePicture)
                else -> {
                    throw IllegalStateException("It should not be possible to personalize without supporting display name or avatar changing")
                }
            }
        }
    }

    private fun handleDisplayNameStepComplete() {
        withPersonalisationState {
            when {
                it.supportsChangingProfilePicture -> _viewEvents.post(OnboardingViewEvents.OnChooseProfilePicture)
                else -> completePersonalization()
            }
        }
    }

    private fun handleProfilePictureSelected(action: OnboardingAction.ProfilePictureSelected) {
        setState {
            copy(personalizationState = personalizationState.copy(selectedPictureUri = action.uri))
        }
    }

    private fun withPersonalisationState(block: (PersonalizationState) -> Unit) {
        withState { block(it.personalizationState) }
    }

    private fun updateProfilePicture() {
        withState { state ->
            when (val pictureUri = state.personalizationState.selectedPictureUri) {
                null -> _viewEvents.post(OnboardingViewEvents.Failure(NullPointerException("picture uri is missing from state")))
                else -> {
                    setState { copy(isLoading = true) }
                    viewModelScope.launch {
                        val activeSession = activeSessionHolder.getActiveSession()
                        try {
                            activeSession.profileService().updateAvatar(
                                    activeSession.myUserId,
                                    pictureUri,
                                    uriFilenameResolver.getFilenameFromUri(pictureUri) ?: UUID.randomUUID().toString()
                            )
                            setState {
                                copy(
                                        isLoading = false,
                                )
                            }
                            onProfilePictureSaved()
                        } catch (error: Throwable) {
                            setState { copy(isLoading = false) }
                            _viewEvents.post(OnboardingViewEvents.Failure(error))
                        }
                    }
                }
            }
        }
    }

    private fun onProfilePictureSaved() {
        completePersonalization()
    }

    private fun completePersonalization() {
        _viewEvents.post(OnboardingViewEvents.OnPersonalizationComplete)
    }

    private fun cancelWaitForEmailValidation() {
        emailVerificationPollingJob = null
    }
}

private fun LoginMode.supportsSignModeScreen(): Boolean {
    return when (this) {
        LoginMode.Password,
        is LoginMode.SsoAndPassword -> true
        is LoginMode.Sso -> {
            // In this case, an error will be displayed in the next screen
            hasOidcCompatibilityFlow
        }
        LoginMode.Unknown,
        LoginMode.Unsupported -> false
    }
}
