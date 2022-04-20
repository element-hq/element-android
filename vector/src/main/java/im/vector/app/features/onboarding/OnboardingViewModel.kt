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
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.vectorStore
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
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
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.registration.Stage
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
        private val vectorOverrides: VectorOverrides
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
        get() = registrationWizard.currentThreePid

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
        get() = authenticationService.isRegistrationStarted

    private val loginWizard: LoginWizard?
        get() = authenticationService.getLoginWizard()

    private var loginConfig: LoginConfig? = null

    private var currentJob: Job? = null
        set(value) {
            // Cancel any previous Job
            field?.cancel()
            field = value
        }

    override fun handle(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.OnGetStarted               -> handleSplashAction(action.resetLoginConfig, action.onboardingFlow)
            is OnboardingAction.OnIAlreadyHaveAnAccount    -> handleSplashAction(action.resetLoginConfig, action.onboardingFlow)
            is OnboardingAction.UpdateUseCase              -> handleUpdateUseCase(action)
            OnboardingAction.ResetUseCase                  -> resetUseCase()
            is OnboardingAction.UpdateServerType           -> handleUpdateServerType(action)
            is OnboardingAction.UpdateSignMode             -> handleUpdateSignMode(action)
            is OnboardingAction.InitWith                   -> handleInitWith(action)
            is OnboardingAction.HomeServerChange           -> withAction(action) { handleHomeserverChange(action) }
            is OnboardingAction.LoginOrRegister            -> handleLoginOrRegister(action).also { lastAction = action }
            is OnboardingAction.Register                   -> handleRegisterWith(action).also { lastAction = action }
            is OnboardingAction.LoginWithToken             -> handleLoginWithToken(action)
            is OnboardingAction.WebLoginSuccess            -> handleWebLoginSuccess(action)
            is OnboardingAction.ResetPassword              -> handleResetPassword(action)
            is OnboardingAction.ResetPasswordMailConfirmed -> handleResetPasswordMailConfirmed()
            is OnboardingAction.PostRegisterAction         -> handleRegisterAction(action.registerAction, ::emitFlowResultViewEvent)
            is OnboardingAction.ResetAction                -> handleResetAction(action)
            is OnboardingAction.UserAcceptCertificate      -> handleUserAcceptCertificate(action)
            OnboardingAction.ClearHomeServerHistory        -> handleClearHomeServerHistory()
            is OnboardingAction.UpdateDisplayName          -> updateDisplayName(action.displayName)
            OnboardingAction.UpdateDisplayNameSkipped      -> handleDisplayNameStepComplete()
            OnboardingAction.UpdateProfilePictureSkipped   -> completePersonalization()
            OnboardingAction.PersonalizeProfile            -> handlePersonalizeProfile()
            is OnboardingAction.ProfilePictureSelected     -> handleProfilePictureSelected(action)
            OnboardingAction.SaveSelectedProfilePicture    -> updateProfilePicture()
            is OnboardingAction.PostViewEvent              -> _viewEvents.post(action.viewEvent)
            OnboardingAction.StopEmailValidationCheck      -> cancelWaitForEmailValidation()
        }
    }

    private fun withAction(action: OnboardingAction, block: (OnboardingAction) -> Unit) {
        lastAction = action
        block(action)
    }

    private fun handleSplashAction(resetConfig: Boolean, onboardingFlow: OnboardingFlow) {
        if (resetConfig) {
            loginConfig = null
        }
        setState { copy(onboardingFlow = onboardingFlow) }

        return when (val config = loginConfig.toHomeserverConfig()) {
            null -> continueToPageAfterSplash(onboardingFlow)
            else -> startAuthenticationFlow(trigger = null, config, ServerType.Other)
        }
    }

    private fun LoginConfig?.toHomeserverConfig(): HomeServerConnectionConfig? {
        return this?.homeServerUrl?.takeIf { it.isNotEmpty() }?.let { url ->
            homeServerConnectionConfigFactory.create(url).also {
                if (it == null) {
                    Timber.w("Url from config url was invalid: $url")
                }
            }
        }
    }

    private fun continueToPageAfterSplash(onboardingFlow: OnboardingFlow) {
        val nextOnboardingStep = when (onboardingFlow) {
            OnboardingFlow.SignUp       -> if (vectorFeatures.isOnboardingUseCaseEnabled()) {
                OnboardingViewEvents.OpenUseCaseSelection
            } else {
                OnboardingViewEvents.OpenServerSelection
            }
            OnboardingFlow.SignIn,
            OnboardingFlow.SignInSignUp -> OnboardingViewEvents.OpenServerSelection
        }
        _viewEvents.post(nextOnboardingStep)
    }

    private fun handleUserAcceptCertificate(action: OnboardingAction.UserAcceptCertificate) {
        // It happens when we get the login flow, or during direct authentication.
        // So alter the homeserver config and retrieve again the login flow
        when (val finalLastAction = lastAction) {
            is OnboardingAction.HomeServerChange.SelectHomeServer -> {
                currentHomeServerConnectionConfig
                        ?.let { it.copy(allowedFingerprints = it.allowedFingerprints + action.fingerprint) }
                        ?.let { startAuthenticationFlow(finalLastAction, it) }
            }
            is OnboardingAction.LoginOrRegister                   ->
                handleDirectLogin(
                        finalLastAction,
                        HomeServerConnectionConfig.Builder()
                                // Will be replaced by the task
                                .withHomeServerUri("https://dummy.org")
                                .withAllowedFingerPrints(listOf(action.fingerprint))
                                .build()
                )
            else                                                  -> Unit
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

        if (safeLoginWizard == null) {
            setState { copy(isLoading = false) }
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Bad configuration")))
        } else {
            setState { copy(isLoading = true) }

            currentJob = viewModelScope.launch {
                try {
                    val result = safeLoginWizard.loginWithToken(action.loginToken)
                    onSessionCreated(result, isAccountCreated = false)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun handleRegisterAction(action: RegisterAction, onNextRegistrationStepAction: (FlowResult) -> Unit) {
        currentJob = viewModelScope.launch {
            if (action.hasLoadingState()) {
                setState { copy(isLoading = true) }
            }
            internalRegisterAction(action, onNextRegistrationStepAction)
            setState { copy(isLoading = false) }
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
                                else                   -> when (it) {
                                    is RegistrationResult.Success      -> onSessionCreated(it.session, isAccountCreated = true)
                                    is RegistrationResult.FlowResponse -> onFlowResponse(it.flowResult, onNextRegistrationStepAction)
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
        _viewEvents.post(OnboardingViewEvents.RegistrationFlowResult(flowResult, isRegistrationStarted))
    }

    private fun handleRegisterWith(action: OnboardingAction.Register) {
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

        when (action) {
            OnboardingAction.ResetHomeServerType        -> {
                setState { copy(serverType = ServerType.Unknown) }
            }
            OnboardingAction.ResetHomeServerUrl         -> {
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
            OnboardingAction.ResetSignMode              -> {
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
            OnboardingAction.ResetResetPassword         -> {
                setState {
                    copy(
                            isLoading = false,
                            resetPasswordEmail = null
                    )
                }
            }
        }
    }

    private fun handleUpdateSignMode(action: OnboardingAction.UpdateSignMode) {
        updateSignMode(action.signMode)
        when (action.signMode) {
            SignMode.SignUp             -> handleRegisterAction(RegisterAction.StartRegistration, ::emitFlowResultViewEvent)
            SignMode.SignIn             -> startAuthenticationFlow()
            SignMode.SignInWithMatrixId -> _viewEvents.post(OnboardingViewEvents.OnSignModeSelected(SignMode.SignInWithMatrixId))
            SignMode.Unknown            -> Unit
        }
    }

    private fun updateSignMode(signMode: SignMode) {
        setState { copy(signMode = signMode) }
    }

    private fun handleUpdateUseCase(action: OnboardingAction.UpdateUseCase) {
        setState { copy(useCase = action.useCase) }
        when (vectorFeatures.isOnboardingCombinedRegisterEnabled()) {
            true  -> handle(OnboardingAction.HomeServerChange.SelectHomeServer(defaultHomeserverUrl))
            false -> _viewEvents.post(OnboardingViewEvents.OpenServerSelection)
        }
    }

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
            ServerType.Unknown   -> Unit /* Should not happen */
            ServerType.MatrixOrg ->
                // Request login flow here
                handle(OnboardingAction.HomeServerChange.SelectHomeServer(matrixOrgUrl))
            ServerType.EMS,
            ServerType.Other     -> _viewEvents.post(OnboardingViewEvents.OnServerSelectionDone(action.serverType))
        }
    }

    private fun handleInitWith(action: OnboardingAction.InitWith) {
        loginConfig = action.loginConfig

        // If there is a pending email validation continue on this step
        try {
            if (registrationWizard.isRegistrationStarted) {
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

        if (safeLoginWizard == null) {
            setState { copy(isLoading = false) }
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Bad configuration")))
        } else {
            setState { copy(isLoading = true) }

            currentJob = viewModelScope.launch {
                try {
                    safeLoginWizard.resetPassword(action.email, action.newPassword)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(failure))
                    return@launch
                }

                setState {
                    copy(
                            isLoading = false,
                            resetPasswordEmail = action.email
                    )
                }

                _viewEvents.post(OnboardingViewEvents.OnResetPasswordSendThreePidDone)
            }
        }
    }

    private fun handleResetPasswordMailConfirmed() {
        val safeLoginWizard = loginWizard

        if (safeLoginWizard == null) {
            setState { copy(isLoading = false) }
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Bad configuration")))
        } else {
            setState { copy(isLoading = false) }

            currentJob = viewModelScope.launch {
                try {
                    safeLoginWizard.resetPasswordMailConfirmed()
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(failure))
                    return@launch
                }
                setState {
                    copy(
                            isLoading = false,
                            resetPasswordEmail = null
                    )
                }

                _viewEvents.post(OnboardingViewEvents.OnResetPasswordMailConfirmationSuccess)
            }
        }
    }

    private fun handleLoginOrRegister(action: OnboardingAction.LoginOrRegister) = withState { state ->
        when (state.signMode) {
            SignMode.Unknown            -> error("Developer error, invalid sign mode")
            SignMode.SignIn             -> handleLogin(action)
            SignMode.SignUp             -> handleRegisterWith(OnboardingAction.Register(action.username, action.password, action.initialDeviceName))
            SignMode.SignInWithMatrixId -> handleDirectLogin(action, null)
        }
    }

    private fun handleDirectLogin(action: OnboardingAction.LoginOrRegister, homeServerConnectionConfig: HomeServerConnectionConfig?) {
        setState { copy(isLoading = true) }
        currentJob = viewModelScope.launch {
            directLoginUseCase.execute(action, homeServerConnectionConfig).fold(
                    onSuccess = { onSessionCreated(it, isAccountCreated = false) },
                    onFailure = {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(OnboardingViewEvents.Failure(it))
                    }
            )
        }
    }

    private fun handleLogin(action: OnboardingAction.LoginOrRegister) {
        val safeLoginWizard = loginWizard

        if (safeLoginWizard == null) {
            setState { copy(isLoading = false) }
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Bad configuration")))
        } else {
            setState { copy(isLoading = true) }
            currentJob = viewModelScope.launch {
                try {
                    val result = safeLoginWizard.login(
                            action.username,
                            action.password,
                            action.initialDeviceName
                    )
                    reAuthHelper.data = action.password
                    onSessionCreated(result, isAccountCreated = false)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(OnboardingViewEvents.Failure(failure))
                }
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

    private suspend fun onSessionCreated(session: Session, isAccountCreated: Boolean) {
        val state = awaitState()
        state.useCase?.let { useCase ->
            session.vectorStore(applicationContext).setUseCase(useCase)
            analyticsTracker.updateUserProperties(UserProperties(ftueUseCaseSelection = useCase.toTrackingValue()))
        }
        activeSessionHolder.setActiveSession(session)

        authenticationService.reset()
        session.configureAndStart(applicationContext)

        when (isAccountCreated) {
            true  -> {
                val personalizationState = createPersonalizationState(session, state)
                setState {
                    copy(isLoading = false, personalizationState = personalizationState)
                }
                _viewEvents.post(OnboardingViewEvents.OnAccountCreated)
            }
            false -> {
                setState { copy(isLoading = false) }
                _viewEvents.post(OnboardingViewEvents.OnAccountSignedIn)
            }
        }
    }

    private suspend fun createPersonalizationState(session: Session, state: OnboardingViewState): PersonalizationState {
        return when {
            vectorFeatures.isOnboardingPersonalizeEnabled() -> {
                val homeServerCapabilities = session.getHomeServerCapabilities()
                val capabilityOverrides = vectorOverrides.forceHomeserverCapabilities?.firstOrNull()
                state.personalizationState.copy(
                        supportsChangingDisplayName = capabilityOverrides?.canChangeDisplayName ?: homeServerCapabilities.canChangeDisplayName,
                        supportsChangingProfilePicture = capabilityOverrides?.canChangeAvatar ?: homeServerCapabilities.canChangeAvatar
                )
            }
            else                                            -> state.personalizationState
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
                    onSessionCreated(result, isAccountCreated = false)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun handleHomeserverChange(action: OnboardingAction.HomeServerChange) {
        val homeServerConnectionConfig = homeServerConnectionConfigFactory.create(action.homeServerUrl)
        if (homeServerConnectionConfig == null) {
            // This is invalid
            _viewEvents.post(OnboardingViewEvents.Failure(Throwable("Unable to create a HomeServerConnectionConfig")))
        } else {
            startAuthenticationFlow(action, homeServerConnectionConfig)
        }
    }

    private fun startAuthenticationFlow(
            trigger: OnboardingAction?,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            serverTypeOverride: ServerType? = null
    ) {
        currentHomeServerConnectionConfig = homeServerConnectionConfig

        currentJob = viewModelScope.launch {
            setState { copy(isLoading = true) }
            runCatching { startAuthenticationFlowUseCase.execute(homeServerConnectionConfig) }.fold(
                    onSuccess = { onAuthenticationStartedSuccess(trigger, homeServerConnectionConfig, it, serverTypeOverride) },
                    onFailure = { _viewEvents.post(OnboardingViewEvents.Failure(it)) }
            )
            setState { copy(isLoading = false) }
        }
    }

    private suspend fun onAuthenticationStartedSuccess(
            trigger: OnboardingAction?,
            config: HomeServerConnectionConfig,
            authResult: StartAuthenticationResult,
            serverTypeOverride: ServerType?
    ) {
        rememberHomeServer(config.homeServerUri.toString())
        if (authResult.isHomeserverOutdated) {
            _viewEvents.post(OnboardingViewEvents.OutdatedHomeserver)
        }

        when (trigger) {
            is OnboardingAction.HomeServerChange.EditHomeServer   -> {
                when (awaitState().onboardingFlow) {
                    OnboardingFlow.SignUp -> internalRegisterAction(RegisterAction.StartRegistration) { _ ->
                        updateServerSelection(config, serverTypeOverride, authResult)
                        _viewEvents.post(OnboardingViewEvents.OnHomeserverEdited)
                    }
                    else                  -> throw IllegalArgumentException("developer error")
                }
            }
            is OnboardingAction.HomeServerChange.SelectHomeServer -> {
                updateServerSelection(config, serverTypeOverride, authResult)
                if (authResult.selectedHomeserver.preferredLoginMode.supportsSignModeScreen()) {
                    when (awaitState().onboardingFlow) {
                        OnboardingFlow.SignIn -> {
                            updateSignMode(SignMode.SignIn)
                            internalRegisterAction(RegisterAction.StartRegistration, ::emitFlowResultViewEvent)
                        }
                        OnboardingFlow.SignUp -> {
                            updateSignMode(SignMode.SignUp)
                            internalRegisterAction(RegisterAction.StartRegistration, ::emitFlowResultViewEvent)
                        }
                        OnboardingFlow.SignInSignUp,
                        null                  -> {
                            _viewEvents.post(OnboardingViewEvents.OnLoginFlowRetrieved)
                        }
                    }
                } else {
                    _viewEvents.post(OnboardingViewEvents.OnLoginFlowRetrieved)
                }
            }
            else                                                  -> {
                updateServerSelection(config, serverTypeOverride, authResult)
                _viewEvents.post(OnboardingViewEvents.OnLoginFlowRetrieved)
            }
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
     * If user has entered https://matrix.org, ensure that server type is ServerType.MatrixOrg
     * It is also useful to set the value again in the case of a certificate error on matrix.org
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

    fun getSsoUrl(redirectUrl: String, deviceId: String?, providerId: String?): String? {
        return authenticationService.getSsoUrl(redirectUrl, deviceId, providerId)
    }

    fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String? {
        return authenticationService.getFallbackUrl(forSignIn, deviceId)
    }

    private fun updateDisplayName(displayName: String) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            val activeSession = activeSessionHolder.getActiveSession()
            try {
                activeSession.setDisplayName(activeSession.myUserId, displayName)
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
                it.supportsChangingDisplayName    -> _viewEvents.post(OnboardingViewEvents.OnChooseDisplayName)
                it.supportsChangingProfilePicture -> _viewEvents.post(OnboardingViewEvents.OnChooseProfilePicture)
                else                              -> {
                    throw IllegalStateException("It should not be possible to personalize without supporting display name or avatar changing")
                }
            }
        }
    }

    private fun handleDisplayNameStepComplete() {
        withPersonalisationState {
            when {
                it.supportsChangingProfilePicture -> _viewEvents.post(OnboardingViewEvents.OnChooseProfilePicture)
                else                              -> completePersonalization()
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
                            activeSession.updateAvatar(
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
        currentJob = null
    }
}

private fun LoginMode.supportsSignModeScreen(): Boolean {
    return when (this) {
        LoginMode.Password,
        is LoginMode.SsoAndPassword -> true
        is LoginMode.Sso,
        LoginMode.Unknown,
        LoginMode.Unsupported       -> false
    }
}
