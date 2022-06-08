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

package im.vector.app.features.onboarding

import android.content.Context
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.cancelCurrentOnSet
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.inferNoConnectivity
import im.vector.app.core.extensions.vectorStore
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.StringProvider
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
import im.vector.app.features.onboarding.OnboardingAction.AuthenticateAction
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import im.vector.app.features.onboarding.ftueauth.MatrixOrgRegistrationStagesComparator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import org.matrix.android.sdk.api.session.Session
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
        private val registrationActionHandler: RegistrationActionHandler,
        private val directLoginUseCase: DirectLoginUseCase,
        private val startAuthenticationFlowUseCase: StartAuthenticationFlowUseCase,
        private val vectorOverrides: VectorOverrides,
        private val buildMeta: BuildMeta
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

    // Store the last action, to redo it after user has trusted the untrusted certificate
    private var lastAction: OnboardingAction? = null
    private var currentHomeServerConnectionConfig: HomeServerConnectionConfig? = null

    private val matrixOrgUrl = stringProvider.getString(R.string.matrix_org_server_url).ensureTrailingSlash()
    private val defaultHomeserverUrl = matrixOrgUrl

    private val registrationWizard: RegistrationWizard
        get() = authenticationService.getRegistrationWizard()

    val currentThreePid: String?
        get() = registrationWizard.getCurrentThreePid()

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
        get() = authenticationService.isRegistrationStarted()

    private val loginWizard: LoginWizard
        get() = authenticationService.getLoginWizard()

    private var loginConfig: LoginConfig? = null

    private var emailVerificationPollingJob: Job? by cancelCurrentOnSet()
    private var currentJob: Job? by cancelCurrentOnSet()

    override fun handle(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SplashAction -> handleSplashAction(action)
            is OnboardingAction.UpdateUseCase -> handleUpdateUseCase(action)
            OnboardingAction.ResetUseCase -> resetUseCase()
            is OnboardingAction.UpdateServerType -> handleUpdateServerType(action)
            is OnboardingAction.UpdateSignMode -> handleUpdateSignMode(action)
            is OnboardingAction.InitWith -> handleInitWith(action)
            is OnboardingAction.HomeServerChange -> withAction(action) { handleHomeserverChange(action) }
            is AuthenticateAction -> withAction(action) { handleAuthenticateAction(action) }
            is OnboardingAction.LoginWithToken -> handleLoginWithToken(action)
            is OnboardingAction.WebLoginSuccess -> handleWebLoginSuccess(action)
            is OnboardingAction.ResetPassword -> handleResetPassword(action)
            is OnboardingAction.ResetPasswordMailConfirmed -> handleResetPasswordMailConfirmed()
            is OnboardingAction.PostRegisterAction -> handleRegisterAction(action.registerAction, ::emitFlowResultViewEvent)
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

    private fun withAction(action: OnboardingAction, block: (OnboardingAction) -> Unit) {
        lastAction = action
        block(action)
    }

    private fun handleAuthenticateAction(action: AuthenticateAction) {
        when (action) {
            is AuthenticateAction.Register -> handleRegisterWith(action)
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
        when (val finalLastAction = lastAction) {
            is OnboardingAction.HomeServerChange.SelectHomeServer -> {
                currentHomeServerConnectionConfig
                        ?.let { it.copy(allowedFingerprints = it.allowedFingerprints + action.fingerprint) }
                        ?.let { startAuthenticationFlow(finalLastAction, it, serverTypeOverride = null) }
            }
            is AuthenticateAction.LoginDirect ->
                handleDirectLogin(
                        finalLastAction,
                        HomeServerConnectionConfig.Builder()
                                // Will be replaced by the task
                                .withHomeServerUri("https://dummy.org")
                                .withAllowedFingerPrints(listOf(action.fingerprint))
                                .build()
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

    private fun handleRegisterAction(action: RegisterAction, onNextRegistrationStepAction: (FlowResult) -> Unit) {
        val job = viewModelScope.launch {
            if (action.hasLoadingState()) {
                setState { copy(isLoading = true) }
            }
            internalRegisterAction(action, onNextRegistrationStepAction)
            setState { copy(isLoading = false) }
        }

        // Allow email verification polling to coexist with other jobs
        when (action) {
            is RegisterAction.CheckIfEmailHasBeenValidated -> emailVerificationPollingJob = job
            else -> currentJob = job
        }
    }

    private suspend fun internalRegisterAction(action: RegisterAction, onNextRegistrationStepAction: (FlowResult) -> Unit) {
        runCatching { registrationActionHandler.handleRegisterAction(registrationWizard, action) }
                .fold(
                        onSuccess = {
                            when {
                                action.ignoresResult() -> {
                                    // do nothing
                                }
                                else -> when (it) {
                                    is RegistrationResult.Complete -> onSessionCreated(
                                            it.session,
                                            authenticationDescription = awaitState().selectedAuthenticationState.description
                                                    ?: AuthenticationDescription.Register(AuthenticationDescription.AuthenticationType.Other)
                                    )
                                    is RegistrationResult.NextStep -> onFlowResponse(it.flowResult, onNextRegistrationStepAction)
                                    is RegistrationResult.SendEmailSuccess -> _viewEvents.post(OnboardingViewEvents.OnSendEmailSuccess(it.email))
                                    is RegistrationResult.Error -> _viewEvents.post(OnboardingViewEvents.Failure(it.cause))
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

    private fun emitFlowResultViewEvent(flowResult: FlowResult) {
        withState { state ->
            val orderedResult = when {
                state.hasSelectedMatrixOrg() && vectorFeatures.isOnboardingCombinedRegisterEnabled() -> flowResult.copy(
                        missingStages = flowResult.missingStages.sortedWith(MatrixOrgRegistrationStagesComparator())
                )
                else -> flowResult
            }
            _viewEvents.post(OnboardingViewEvents.RegistrationFlowResult(orderedResult, isRegistrationStarted))
        }
    }

    private fun OnboardingViewState.hasSelectedMatrixOrg() = selectedHomeserver.userFacingUrl == matrixOrgUrl

    private fun handleRegisterWith(action: AuthenticateAction.Register) {
        setState {
            val authDescription = AuthenticationDescription.Register(AuthenticationDescription.AuthenticationType.Password)
            copy(selectedAuthenticationState = SelectedAuthenticationState(authDescription))
        }
        reAuthHelper.data = action.password
        handleRegisterAction(
                RegisterAction.CreateAccount(
                        action.username,
                        action.password,
                        action.initialDeviceName
                ),
                ::emitFlowResultViewEvent
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
                    setState { copy(isLoading = false) }
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
        }
    }

    private fun handleUpdateSignMode(action: OnboardingAction.UpdateSignMode) {
        updateSignMode(action.signMode)
        when (action.signMode) {
            SignMode.SignUp -> handleRegisterAction(RegisterAction.StartRegistration, ::emitFlowResultViewEvent)
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
        // If there is a pending email validation continue on this step
        try {
            if (registrationWizard.isRegistrationStarted()) {
                currentThreePid?.let {
                    handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnSendEmailSuccess(it)))
                }
            }
        } catch (e: Throwable) {
            // NOOP. API is designed to use wizards in a login/registration flow,
            // but we need to check the state anyway.
        }
    }

    private fun handleResetPassword(action: OnboardingAction.ResetPassword) {
        val safeLoginWizard = loginWizard
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            runCatching { safeLoginWizard.resetPassword(action.email) }.fold(
                    onSuccess = {
                        setState {
                            copy(
                                    isLoading = false,
                                    resetState = ResetState(email = action.email, newPassword = action.newPassword)
                            )
                        }
                        _viewEvents.post(OnboardingViewEvents.OnResetPasswordSendThreePidDone)
                    },
                    onFailure = {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(OnboardingViewEvents.Failure(it))
                    }
            )
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
                else -> {
                    runCatching { loginWizard.resetPasswordMailConfirmed(newPassword) }.fold(
                            onSuccess = {
                                setState {
                                    copy(
                                            isLoading = false,
                                            resetState = ResetState()
                                    )
                                }
                                _viewEvents.post(OnboardingViewEvents.OnResetPasswordMailConfirmationSuccess)
                            },
                            onFailure = {
                                setState { copy(isLoading = false) }
                                _viewEvents.post(OnboardingViewEvents.Failure(it))
                            }
                    )
                }
            }
        }
    }

    private fun handleDirectLogin(action: AuthenticateAction.LoginDirect, homeServerConnectionConfig: HomeServerConnectionConfig?) {
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            directLoginUseCase.execute(action, homeServerConnectionConfig).fold(
                    onSuccess = { onSessionCreated(it, authenticationDescription = AuthenticationDescription.Login) },
                    onFailure = {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(OnboardingViewEvents.Failure(it))
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

    private suspend fun onFlowResponse(flowResult: FlowResult, onNextRegistrationStepAction: (FlowResult) -> Unit) {
        // If dummy stage is mandatory, and password is already sent, do the dummy stage now
        if (isRegistrationStarted && flowResult.missingStages.any { it is Stage.Dummy && it.mandatory }) {
            handleRegisterDummy(onNextRegistrationStepAction)
        } else {
            onNextRegistrationStepAction(flowResult)
        }
    }

    private suspend fun handleRegisterDummy(onNextRegistrationStepAction: (FlowResult) -> Unit) {
        internalRegisterAction(RegisterAction.RegisterDummy, onNextRegistrationStepAction)
    }

    private suspend fun onSessionCreated(session: Session, authenticationDescription: AuthenticationDescription) {
        val state = awaitState()
        state.useCase?.let { useCase ->
            session.vectorStore(applicationContext).setUseCase(useCase)
            analyticsTracker.updateUserProperties(UserProperties(ftueUseCaseSelection = useCase.toTrackingValue()))
        }
        activeSessionHolder.setActiveSession(session)

        authenticationService.reset()
        session.configureAndStart(applicationContext)

        when (authenticationDescription) {
            is AuthenticationDescription.Register -> {
                val personalizationState = createPersonalizationState(session, state)
                setState {
                    copy(isLoading = false, personalizationState = personalizationState)
                }
                _viewEvents.post(OnboardingViewEvents.OnAccountCreated)
            }
            AuthenticationDescription.Login -> {
                setState { copy(isLoading = false) }
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

    private fun handleHomeserverChange(action: OnboardingAction.HomeServerChange, serverTypeOverride: ServerType? = null) {
        val homeServerConnectionConfig = homeServerConnectionConfigFactory.create(action.homeServerUrl)
        if (homeServerConnectionConfig == null) {
            // This is invalid
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Unable to create a HomeServerConnectionConfig")))
        } else {
            startAuthenticationFlow(action, homeServerConnectionConfig, serverTypeOverride)
        }
    }

    private fun startAuthenticationFlow(
            trigger: OnboardingAction.HomeServerChange,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            serverTypeOverride: ServerType?
    ) {
        currentHomeServerConnectionConfig = homeServerConnectionConfig

        currentJob = viewModelScope.launch {
            setState { copy(isLoading = true) }
            runCatching { startAuthenticationFlowUseCase.execute(homeServerConnectionConfig) }.fold(
                    onSuccess = { onAuthenticationStartedSuccess(trigger, homeServerConnectionConfig, it, serverTypeOverride) },
                    onFailure = { onAuthenticationStartError(it, trigger) }
            )
            setState { copy(isLoading = false) }
        }
    }

    private fun onAuthenticationStartError(error: Throwable, trigger: OnboardingAction.HomeServerChange) {
        when {
            error.isHomeserverUnavailable() && applicationContext.inferNoConnectivity(buildMeta) -> _viewEvents.post(
                    OnboardingViewEvents.Failure(error)
            )
            deeplinkUrlIsUnavailable(error, trigger) -> _viewEvents.post(
                    OnboardingViewEvents.DeeplinkAuthenticationFailure(
                            retryAction = (trigger as OnboardingAction.HomeServerChange.SelectHomeServer).resetToDefaultUrl()
                    )
            )
            else -> _viewEvents.post(
                    OnboardingViewEvents.Failure(error)
            )
        }
    }

    private fun deeplinkUrlIsUnavailable(error: Throwable, trigger: OnboardingAction.HomeServerChange) = error.isHomeserverUnavailable() &&
            loginConfig != null &&
            trigger is OnboardingAction.HomeServerChange.SelectHomeServer

    private fun OnboardingAction.HomeServerChange.SelectHomeServer.resetToDefaultUrl() = copy(homeServerUrl = defaultHomeserverUrl)

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
                    internalRegisterAction(RegisterAction.StartRegistration, ::emitFlowResultViewEvent)
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
            OnboardingFlow.SignUp -> internalRegisterAction(RegisterAction.StartRegistration) {
                updateServerSelection(config, serverTypeOverride, authResult)
                _viewEvents.post(OnboardingViewEvents.OnHomeserverEdited)
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

    fun fetchSsoUrl(redirectUrl: String, deviceId: String?, provider: SsoIdentityProvider?): String? {
        setState {
            val authDescription = AuthenticationDescription.Register(provider.toAuthenticationType())
            copy(selectedAuthenticationState = SelectedAuthenticationState(authDescription))
        }
        return authenticationService.getSsoUrl(redirectUrl, deviceId, provider?.id)
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
        is LoginMode.Sso,
        LoginMode.Unknown,
        LoginMode.Unsupported -> false
    }
}
