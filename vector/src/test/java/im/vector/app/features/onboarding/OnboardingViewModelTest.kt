/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.net.Uri
import android.os.Build
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.session.ConfigureAndStartSessionUseCase
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.mdm.NoOpMdmService
import im.vector.app.features.onboarding.RegistrationStateFixture.aRegistrationState
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import im.vector.app.test.TestBuildVersionSdkIntProvider
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeDirectLoginUseCase
import im.vector.app.test.fakes.FakeHomeServerConnectionConfigFactory
import im.vector.app.test.fakes.FakeHomeServerHistoryService
import im.vector.app.test.fakes.FakeLoginWizard
import im.vector.app.test.fakes.FakeRegistrationActionHandler
import im.vector.app.test.fakes.FakeRegistrationWizard
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStartAuthenticationFlowUseCase
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFilenameResolver
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorOverrides
import im.vector.app.test.fakes.toTestString
import im.vector.app.test.fixtures.a401ServerError
import im.vector.app.test.fixtures.aHomeServerCapabilities
import im.vector.app.test.fixtures.anUnrecognisedCertificateError
import im.vector.app.test.test
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.SSOAction
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

private const val A_DISPLAY_NAME = "a display name"
private const val A_PICTURE_FILENAME = "a-picture.png"
private val A_SERVER_ERROR = a401ServerError()
private val AN_ERROR = RuntimeException("an error!")
private val AN_UNRECOGNISED_CERTIFICATE_ERROR = anUnrecognisedCertificateError()
private val A_LOADABLE_REGISTER_ACTION = RegisterAction.StartRegistration
private val A_NON_LOADABLE_REGISTER_ACTION = RegisterAction.CheckIfEmailHasBeenValidated(delayMillis = -1L)
private val A_RESULT_IGNORED_REGISTER_ACTION = RegisterAction.SendAgainThreePid
private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canChangeDisplayName = true, canChangeAvatar = true)
private val A_FINGERPRINT = Fingerprint(ByteArray(1), Fingerprint.HashType.SHA1)
private val ANY_CONTINUING_REGISTRATION_RESULT = RegistrationActionHandler.Result.NextStage(Stage.Dummy(mandatory = true))
private val A_DIRECT_LOGIN = OnboardingAction.AuthenticateAction.LoginDirect("@a-user:id.org", "a-password", "a-device-name")
private const val A_HOMESERVER_URL = "https://edited-homeserver.org"
private val A_DEFAULT_HOMESERVER_URL = "${im.vector.app.config.R.string.matrix_org_server_url.toTestString()}/"
private val A_HOMESERVER_CONFIG = HomeServerConnectionConfig(FakeUri().instance)
private val SELECTED_HOMESERVER_STATE = SelectedHomeserverState(preferredLoginMode = LoginMode.Password, userFacingUrl = A_HOMESERVER_URL)
private val SELECTED_HOMESERVER_STATE_SUPPORTED_LOGOUT_DEVICES = SelectedHomeserverState(isLogoutDevicesSupported = true)
private val DEFAULT_SELECTED_HOMESERVER_STATE = SELECTED_HOMESERVER_STATE.copy(userFacingUrl = A_DEFAULT_HOMESERVER_URL)
private val DEFAULT_SELECTED_HOMESERVER_STATE_WITH_QR_SUPPORTED = DEFAULT_SELECTED_HOMESERVER_STATE.copy(isLoginWithQrSupported = true)
private const val AN_EMAIL = "hello@example.com"
private const val A_PASSWORD = "a-password"
private const val A_USERNAME = "hello-world"
private const val A_DEVICE_NAME = "a-device-name"
private const val A_MATRIX_ID = "@$A_USERNAME:matrix.org"
private const val A_LOGIN_TOKEN = "a-login-token"
private val A_REGISTRATION_STATE = aRegistrationState(email = AN_EMAIL)
private const val A_SSO_URL = "https://a-sso.url"
private const val A_REDIRECT_URI = "https://a-redirect.uri"
private const val A_DEVICE_ID = "a-device-id"
private val SSO_REGISTRATION_DESCRIPTION = AuthenticationDescription.Register(AuthenticationDescription.AuthenticationType.SSO)

class OnboardingViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val fakeUri = FakeUri()
    private val fakeContext = FakeContext()
    private val fakeSession = FakeSession()
    private val fakeUriFilenameResolver = FakeUriFilenameResolver()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder(fakeSession)
    private val fakeAuthenticationService = FakeAuthenticationService()
    private val fakeRegistrationActionHandler = FakeRegistrationActionHandler()
    private val fakeDirectLoginUseCase = FakeDirectLoginUseCase()
    private val fakeVectorFeatures = FakeVectorFeatures()
    private val fakeHomeServerConnectionConfigFactory = FakeHomeServerConnectionConfigFactory()
    private val fakeStartAuthenticationFlowUseCase = FakeStartAuthenticationFlowUseCase()
    private val fakeHomeServerHistoryService = FakeHomeServerHistoryService()
    private val fakeLoginWizard = FakeLoginWizard()
    private val fakeConfigureAndStartSessionUseCase = mockk<ConfigureAndStartSessionUseCase>()

    private var initialState = OnboardingViewState()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModelWith(initialState)
    }

    @Test
    fun `given usecase screen enabled, when handling sign up splash action, then emits OpenUseCaseSelection`() = runTest {
        val test = viewModel.test()
        fakeVectorFeatures.givenOnboardingUseCaseEnabled()

        viewModel.handle(OnboardingAction.SplashAction.OnGetStarted(OnboardingFlow.SignUp))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(onboardingFlow = OnboardingFlow.SignUp) }
                )
                .assertEvents(OnboardingViewEvents.OpenUseCaseSelection)
                .finish()
    }

    @Test
    fun `given combined login enabled, when handling sign in splash action, then emits OpenCombinedLogin with default homeserver`() = runTest {
        val test = viewModel.test()
        fakeVectorFeatures.givenCombinedLoginEnabled()
        givenCanSuccessfullyUpdateHomeserver(A_DEFAULT_HOMESERVER_URL, DEFAULT_SELECTED_HOMESERVER_STATE)

        viewModel.handle(OnboardingAction.SplashAction.OnIAlreadyHaveAnAccount(OnboardingFlow.SignIn))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(onboardingFlow = OnboardingFlow.SignIn) },
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = DEFAULT_SELECTED_HOMESERVER_STATE) },
                        { copy(signMode = SignMode.SignIn) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.OpenCombinedLogin)
                .finish()
    }

    @Test
    fun `given can successfully login in with token, when logging in with token, then emits AccountSignedIn`() = runTest {
        val test = viewModel.test()
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)
        fakeLoginWizard.givenLoginWithTokenResult(A_LOGIN_TOKEN, fakeSession)
        givenInitialisesSession(fakeSession)

        viewModel.handle(OnboardingAction.LoginWithToken(A_LOGIN_TOKEN))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, selectedAuthenticationState = SelectedAuthenticationState(description = AuthenticationDescription.Login)) }
                )
                .assertEvents(OnboardingViewEvents.OnAccountSignedIn)
                .finish()
    }

    @Test
    fun `given can login with username and password, when logging in, then emits AccountSignedIn`() = runTest {
        val test = viewModel.test()
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)
        fakeLoginWizard.givenLoginSuccess(A_USERNAME, A_PASSWORD, A_DEVICE_NAME, fakeSession)
        givenInitialisesSession(fakeSession)

        viewModel.handle(OnboardingAction.AuthenticateAction.Login(A_USERNAME, A_PASSWORD, A_DEVICE_NAME))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, selectedAuthenticationState = SelectedAuthenticationState(description = AuthenticationDescription.Login)) }
                )
                .assertEvents(OnboardingViewEvents.OnAccountSignedIn)
                .finish()
    }

    @Test
    fun `given registration not started, when handling InitWith, then does nothing`() = runTest {
        val test = viewModel.test()
        fakeAuthenticationService.givenRegistrationWizard(FakeRegistrationWizard().also { it.givenRegistrationStarted(hasStarted = false) })

        viewModel.handle(OnboardingAction.InitWith(LoginConfig(A_HOMESERVER_URL, identityServerUrl = null)))

        test
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given registration started without currentThreePid, when handling InitWith, then does nothing`() = runTest {
        val test = viewModel.test()
        fakeAuthenticationService.givenRegistrationWizard(FakeRegistrationWizard().also {
            it.givenRegistrationStarted(hasStarted = true)
            it.givenCurrentThreePid(threePid = null)
        })

        viewModel.handle(OnboardingAction.InitWith(LoginConfig(A_HOMESERVER_URL, identityServerUrl = null)))

        test
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `when handling PostViewEvent, then emits contents as view event`() = runTest {
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome))

        test
                .assertEvents(OnboardingViewEvents.OnTakeMeHome)
                .finish()
    }

    @Test
    fun `given supports changing display name, when handling PersonalizeProfile, then emits contents choose display name`() = runTest {
        viewModelWith(
                initialState.copy(
                        personalizationState = PersonalizationState(
                                supportsChangingDisplayName = true,
                                supportsChangingProfilePicture = false
                        )
                )
        )
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseDisplayName)
                .finish()
    }

    @Test
    fun `given only supports changing profile picture, when handling PersonalizeProfile, then emits contents choose profile picture`() = runTest {
        viewModelWith(
                initialState.copy(
                        personalizationState = PersonalizationState(
                                supportsChangingDisplayName = false,
                                supportsChangingProfilePicture = true
                        )
                )
        )
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                .finish()
    }

    @Test
    fun `given has sign in with matrix id sign mode, when handling login or register action, then logs in directly`() = runTest {
        viewModelWith(initialState.copy(signMode = SignMode.SignInWithMatrixId))
        fakeDirectLoginUseCase.givenSuccessResult(A_DIRECT_LOGIN, config = null, result = fakeSession)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(A_DIRECT_LOGIN)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, selectedAuthenticationState = SelectedAuthenticationState(description = AuthenticationDescription.Login)) }
                )
                .assertEvents(OnboardingViewEvents.OnAccountSignedIn)
                .finish()
    }

    @Test
    fun `given has sign in with matrix id sign mode, when handling login or register action fails, then emits error`() = runTest {
        viewModelWith(initialState.copy(signMode = SignMode.SignInWithMatrixId))
        fakeDirectLoginUseCase.givenFailureResult(A_DIRECT_LOGIN, config = null, cause = AN_ERROR)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(A_DIRECT_LOGIN)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given has sign in with matrix id sign mode, when handling login or register action fails with certificate error, then emits error`() = runTest {
        viewModelWith(initialState.copy(signMode = SignMode.SignInWithMatrixId))
        fakeDirectLoginUseCase.givenFailureResult(A_DIRECT_LOGIN, config = null, cause = AN_UNRECOGNISED_CERTIFICATE_ERROR)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(A_DIRECT_LOGIN)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.UnrecognisedCertificateFailure(A_DIRECT_LOGIN, AN_UNRECOGNISED_CERTIFICATE_ERROR))
                .finish()
    }

    @Test
    fun `when handling SignUp then sets sign mode to sign up and starts registration`() = runTest {
        givenRegistrationResultFor(RegisterAction.StartRegistration, ANY_CONTINUING_REGISTRATION_RESULT)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignUp))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(signMode = SignMode.SignUp) },
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.DisplayRegistrationStage(ANY_CONTINUING_REGISTRATION_RESULT.stage))
                .finish()
    }

    @Test
    fun `given register action requires more steps, when handling action, then posts next steps`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_LOADABLE_REGISTER_ACTION, ANY_CONTINUING_REGISTRATION_RESULT)

        viewModel.handle(OnboardingAction.PostRegisterAction(A_LOADABLE_REGISTER_ACTION))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.DisplayRegistrationStage(ANY_CONTINUING_REGISTRATION_RESULT.stage))
                .finish()
    }

    @Test
    fun `given register action is non loadable, when handling action, then posts next steps without loading`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_NON_LOADABLE_REGISTER_ACTION, ANY_CONTINUING_REGISTRATION_RESULT)

        viewModel.handle(OnboardingAction.PostRegisterAction(A_NON_LOADABLE_REGISTER_ACTION))

        test
                .assertState(initialState)
                .assertEvents(OnboardingViewEvents.DisplayRegistrationStage(ANY_CONTINUING_REGISTRATION_RESULT.stage))
                .finish()
    }

    @Test
    fun `given register action ignores result, when handling action, then does nothing on success`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_RESULT_IGNORED_REGISTER_ACTION, RegistrationActionHandler.Result.Ignored)

        viewModel.handle(OnboardingAction.PostRegisterAction(A_RESULT_IGNORED_REGISTER_ACTION))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given register action returns email success, when handling action, then updates registration state and emits email success`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_LOADABLE_REGISTER_ACTION, RegistrationActionHandler.Result.SendEmailSuccess(AN_EMAIL))

        viewModel.handle(OnboardingAction.PostRegisterAction(A_LOADABLE_REGISTER_ACTION))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(registrationState = RegistrationState(email = AN_EMAIL)) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.OnSendEmailSuccess(AN_EMAIL, isRestoredSession = false))
                .finish()
    }

    @Test
    fun `given in sign in flow, when selecting homeserver fails with network error, then emits Failure`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignIn))
        fakeVectorFeatures.givenCombinedLoginEnabled()
        givenHomeserverSelectionFailsWith(AN_ERROR)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given in sign in flow, when selecting homeserver fails with network error, then emits EditServerSelection`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignIn))
        fakeVectorFeatures.givenCombinedLoginEnabled()
        givenHomeserverSelectionFailsWithNetworkError()
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.EditServerSelection)
                .finish()
    }

    @Test
    fun `given in sign up flow, when selecting homeserver fails with network error, then emits EditServerSelection`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        fakeVectorFeatures.givenCombinedRegisterEnabled()
        givenHomeserverSelectionFailsWithNetworkError()
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.EditServerSelection)
                .finish()
    }

    @Test
    fun `given in the sign up flow, when editing homeserver, then updates selected homeserver state and emits edited event`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        givenCanSuccessfullyUpdateHomeserver(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(isLoading = false) }

                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited)
                .finish()
    }

    @Test
    fun `given a full matrix id, when a login username is entered, then updates selected homeserver state and emits edited event`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignIn))
        givenCanSuccessfullyUpdateHomeserver(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE)
        val test = viewModel.test()
        val fullMatrixId = "@a-user:${A_HOMESERVER_URL.removePrefix("https://")}"

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Login(fullMatrixId))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(isLoading = false) }

                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited)
                .finish()
    }

    @Test
    fun `given a username, when a login username is entered, then does nothing`() = runTest {
        val test = viewModel.test()
        val onlyUsername = "a-username"

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Login(onlyUsername))

        test
                .assertStates(initialState)
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given available username throws, when a register username is entered, then emits error`() = runTest {
        viewModelWith(initialRegistrationState(A_HOMESERVER_URL))
        fakeAuthenticationService.givenRegistrationWizard(FakeRegistrationWizard().also { it.givenUserNameIsAvailableThrows(A_USERNAME, AN_ERROR) })
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(A_USERNAME))

        test
                .assertStates(initialState)
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given available username, when a register username is entered, then emits available registration state`() = runTest {
        viewModelWith(initialRegistrationState(A_HOMESERVER_URL))
        val onlyUsername = "a-username"
        givenUserNameIsAvailable(onlyUsername)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(onlyUsername))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(registrationState = availableRegistrationState(onlyUsername, A_HOMESERVER_URL)) }
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given unavailable username, when a register username is entered, then emits availability error`() = runTest {
        viewModelWith(initialRegistrationState(A_HOMESERVER_URL))
        val onlyUsername = "a-username"
        givenUserNameIsUnavailable(onlyUsername, A_SERVER_ERROR)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(onlyUsername))

        test
                .assertState(initialState)
                .assertEvents(OnboardingViewEvents.Failure(A_SERVER_ERROR))
                .finish()
    }

    @Test
    fun `given available full matrix id, when a register username is entered, then changes homeserver and emits available registration state`() = runTest {
        viewModelWith(initialRegistrationState("ignored-url"))
        givenCanSuccessfullyUpdateHomeserver(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE)
        val userName = "a-user"
        val fullMatrixId = "@$userName:${A_HOMESERVER_URL.removePrefix("https://")}"
        givenUserNameIsAvailable(userName)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(fullMatrixId))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(registrationState = availableRegistrationState(userName, A_HOMESERVER_URL)) },
                        { copy(isLoading = false) },
                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited)
                .finish()
    }

    @Test
    fun `when editing homeserver errors with certificate error, then emits error`() = runTest {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, fingerprints = null, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenErrors(A_HOMESERVER_CONFIG, AN_UNRECOGNISED_CERTIFICATE_ERROR)
        val editAction = OnboardingAction.HomeServerChange.EditHomeServer(A_HOMESERVER_URL)
        val test = viewModel.test()

        viewModel.handle(editAction)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.UnrecognisedCertificateFailure(editAction, AN_UNRECOGNISED_CERTIFICATE_ERROR))
                .finish()
    }

    @Test
    fun `when selecting homeserver errors with certificate error, then emits error`() = runTest {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, fingerprints = null, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenErrors(A_HOMESERVER_CONFIG, AN_UNRECOGNISED_CERTIFICATE_ERROR)
        val selectAction = OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL)
        val test = viewModel.test()

        viewModel.handle(selectAction)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.UnrecognisedCertificateFailure(selectAction, AN_UNRECOGNISED_CERTIFICATE_ERROR))
                .finish()
    }

    @Test
    fun `given unavailable full matrix id, when a register username is entered, then emits availability error`() = runTest {
        viewModelWith(initialRegistrationState("ignored-url"))
        givenCanSuccessfullyUpdateHomeserver(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE)
        val userName = "a-user"
        val fullMatrixId = "@$userName:${A_HOMESERVER_URL.removePrefix("https://")}"
        givenUserNameIsUnavailable(userName, A_SERVER_ERROR)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(fullMatrixId))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(isLoading = false) },
                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited, OnboardingViewEvents.Failure(A_SERVER_ERROR))
                .finish()
    }

    @Test
    fun `given in the sign up flow, when editing homeserver errors, then does not update the selected homeserver state and emits error`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        givenUpdatingHomeserverErrors(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE, AN_ERROR)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given matrix id and personalisation enabled, when registering account, then updates state and emits account created event`() = runTest {
        viewModelWith(initialState.copy(registrationState = RegistrationState(selectedMatrixId = A_MATRIX_ID)))
        fakeVectorFeatures.givenPersonalisationEnabled()
        givenSuccessfullyCreatesAccount(A_HOMESERVER_CAPABILITIES)
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.RegistrationComplete(fakeSession))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.StartRegistration))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, personalizationState = A_HOMESERVER_CAPABILITIES.toPersonalisationState("@fake:server.fake", A_USERNAME)) }
                )
                .assertEvents(OnboardingViewEvents.OnAccountCreated)
                .finish()
    }

    @Test
    fun `given changing profile avatar is supported, when updating display name, then updates upstream user display name and moves to choose profile avatar`() {
        runTest {
            viewModelWith(initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = true)))
            val test = viewModel.test()

            viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

            test
                    .assertStatesChanges(initialState, expectedSuccessfulDisplayNameUpdateStates())
                    .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                    .finish()
            fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
        }
    }

    @Test
    fun `given changing profile avatar is not supported, when updating display name, then updates upstream user display name and completes personalization`() {
        runTest {
            viewModelWith(initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = false)))
            val test = viewModel.test()

            viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

            test
                    .assertStatesChanges(initialState, expectedSuccessfulDisplayNameUpdateStates())
                    .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                    .finish()
            fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
        }
    }

    @Test
    fun `given upstream failure, when handling display name update, then emits failure event`() = runTest {
        val test = viewModel.test()
        fakeSession.fakeProfileService.givenSetDisplayNameErrors(AN_ERROR)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) },
                )
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `when handling profile picture selected, then updates selected picture state`() = runTest {
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.ProfilePictureSelected(fakeUri.instance))

        test
                .assertStates(
                        initialState,
                        initialState.copy(personalizationState = initialState.personalizationState.copy(selectedPictureUri = fakeUri.instance))
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given a selected picture, when handling save selected profile picture, then updates upstream avatar and completes personalization`() = runTest {
        viewModelWith(givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureSuccessStates(initialState))
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
        fakeSession.fakeProfileService.verifyAvatarUpdated(fakeSession.myUserId, fakeUri.instance, A_PICTURE_FILENAME)
    }

    @Test
    fun `given upstream update avatar fails, when saving selected profile picture, then emits failure event`() = runTest {
        fakeSession.fakeProfileService.givenUpdateAvatarErrors(AN_ERROR)
        viewModelWith(givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureFailureStates(initialState))
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given no selected picture, when saving selected profile picture, then emits failure event`() = runTest {
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(initialState)
                .assertEvent { it is OnboardingViewEvents.Failure && it.throwable is NullPointerException }
                .finish()
    }

    @Test
    fun `when handling profile skipped, then completes personalization`() = runTest {
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UpdateProfilePictureSkipped)

        test
                .assertStates(initialState)
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
    }

    @Test
    fun `given in sign in mode, when accepting user certificate with SelectHomeserver retry action, then emits OnHomeserverEdited`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignIn))
        val test = viewModel.test()
        fakeVectorFeatures.givenCombinedLoginEnabled()
        givenCanSuccessfullyUpdateHomeserver(
                A_HOMESERVER_URL,
                SELECTED_HOMESERVER_STATE,
                config = A_HOMESERVER_CONFIG.copy(allowedFingerprints = listOf(A_FINGERPRINT)),
                fingerprints = listOf(A_FINGERPRINT),
        )

        viewModel.handle(OnboardingAction.UserAcceptCertificate(A_FINGERPRINT, OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL)))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(signMode = SignMode.SignIn) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.OpenCombinedLogin)
                .finish()
    }

    @Test
    fun `given in sign up mode, when accepting user certificate with EditHomeserver retry action, then emits OnHomeserverEdited`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        givenCanSuccessfullyUpdateHomeserver(
                A_HOMESERVER_URL,
                SELECTED_HOMESERVER_STATE,
                config = A_HOMESERVER_CONFIG.copy(allowedFingerprints = listOf(A_FINGERPRINT)),
                fingerprints = listOf(A_FINGERPRINT),
        )
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserAcceptCertificate(A_FINGERPRINT, OnboardingAction.HomeServerChange.EditHomeServer(A_HOMESERVER_URL)))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                        { copy(isLoading = false) }

                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited)
                .finish()
    }

    @Test
    fun `given DirectLogin retry action, when accepting user certificate, then logs in directly`() = runTest {
        fakeHomeServerConnectionConfigFactory.givenConfigFor("https://dummy.org", listOf(A_FINGERPRINT), A_HOMESERVER_CONFIG)
        fakeDirectLoginUseCase.givenSuccessResult(A_DIRECT_LOGIN, config = A_HOMESERVER_CONFIG, result = fakeSession)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UserAcceptCertificate(A_FINGERPRINT, A_DIRECT_LOGIN))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, selectedAuthenticationState = SelectedAuthenticationState(description = AuthenticationDescription.Login)) }
                )
                .assertEvents(OnboardingViewEvents.OnAccountSignedIn)
                .finish()
    }

    @Test
    fun `given can successfully start password reset, when resetting password, then emits confirmation email sent`() = runTest {
        viewModelWith(initialState.copy(selectedHomeserver = SELECTED_HOMESERVER_STATE_SUPPORTED_LOGOUT_DEVICES))
        val test = viewModel.test()
        fakeLoginWizard.givenResetPasswordSuccess(AN_EMAIL)
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)

        viewModel.handle(OnboardingAction.ResetPassword(email = AN_EMAIL, newPassword = A_PASSWORD))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        {
                            val resetState = ResetState(AN_EMAIL, A_PASSWORD, supportsLogoutAllDevices = true)
                            copy(isLoading = false, resetState = resetState)
                        }
                )
                .assertEvents(OnboardingViewEvents.OnResetPasswordEmailConfirmationSent(AN_EMAIL))
                .finish()
    }

    @Test
    fun `given existing reset state, when resending reset password email, then triggers reset password and emits nothing`() = runTest {
        viewModelWith(initialState.copy(resetState = ResetState(AN_EMAIL, A_PASSWORD)))
        val test = viewModel.test()
        fakeLoginWizard.givenResetPasswordSuccess(AN_EMAIL)
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)

        viewModel.handle(OnboardingAction.ResendResetPassword)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertNoEvents()
                .finish()
        fakeLoginWizard.verifyResetPassword(AN_EMAIL)
    }

    @Test
    fun `given combined login disabled, when confirming password reset, then opens reset password complete`() = runTest {
        viewModelWith(initialState.copy(resetState = ResetState(AN_EMAIL, A_PASSWORD)))
        val test = viewModel.test()
        fakeVectorFeatures.givenCombinedLoginDisabled()
        fakeLoginWizard.givenConfirmResetPasswordSuccess(A_PASSWORD)
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)

        viewModel.handle(OnboardingAction.ResetPasswordMailConfirmed)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, resetState = ResetState()) }
                )
                .assertEvents(OnboardingViewEvents.OpenResetPasswordComplete)
                .finish()
    }

    @Test
    fun `given combined login enabled, when confirming password reset, then emits reset password complete`() = runTest {
        viewModelWith(initialState.copy(resetState = ResetState(AN_EMAIL, A_PASSWORD)))
        val test = viewModel.test()
        fakeVectorFeatures.givenCombinedLoginEnabled()
        fakeLoginWizard.givenConfirmResetPasswordSuccess(A_PASSWORD)
        fakeAuthenticationService.givenLoginWizard(fakeLoginWizard)

        viewModel.handle(OnboardingAction.ResetPasswordMailConfirmed)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, resetState = ResetState()) }
                )
                .assertEvents(OnboardingViewEvents.OnResetPasswordComplete)
                .finish()
    }

    @Test
    fun `given homeserver state, when resetting homeserver url, then resets auth service and state`() = runTest {
        viewModelWith(initialState.copy(isLoading = true, selectedHomeserver = SELECTED_HOMESERVER_STATE))
        val test = viewModel.test()
        fakeAuthenticationService.expectReset()

        viewModel.handle(OnboardingAction.ResetHomeServerUrl)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = false, selectedHomeserver = SelectedHomeserverState()) },
                )
                .assertNoEvents()
                .finish()
        fakeAuthenticationService.verifyReset()
    }

    @Test
    fun `given server type, when resetting homeserver type, then resets state`() = runTest {
        viewModelWith(initialState.copy(serverType = ServerType.EMS))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.ResetHomeServerType)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(serverType = ServerType.Unknown) },
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given sign mode, when resetting sign mode, then resets state`() = runTest {
        viewModelWith(initialState.copy(isLoading = true, signMode = SignMode.SignIn))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.ResetSignMode)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = false, signMode = SignMode.Unknown) },
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given registration state, when resetting authentication attempt, then cancels pending logic or registration and resets state`() = runTest {
        viewModelWith(initialState.copy(isLoading = true, registrationState = A_REGISTRATION_STATE))
        val test = viewModel.test()
        fakeAuthenticationService.expectedCancelsPendingLogin()

        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = false, registrationState = RegistrationState()) },
                )
                .assertNoEvents()
                .finish()
        fakeAuthenticationService.verifyCancelsPendingLogin()
    }

    @Test
    fun `given reset state, when resetting reset state, then resets state`() = runTest {
        viewModelWith(initialState.copy(isLoading = true, resetState = ResetState(AN_EMAIL)))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.ResetResetPassword)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = false, resetState = ResetState()) },
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given registration state, when resetting user name, then resets state`() = runTest {
        viewModelWith(initialState.copy(registrationState = A_REGISTRATION_STATE))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.ResetSelectedRegistrationUserName)

        test
                .assertStatesChanges(
                        initialState,
                        { copy(registrationState = RegistrationState()) },
                )
                .assertNoEvents()
                .finish()
    }

    @Test
    fun `given returns Sso url, when fetching Sso url, then updates authentication state and returns supplied Sso url`() = runTest {
        val test = viewModel.test()
        val provider = SsoIdentityProvider(id = "provider_id", null, null, null)
        fakeAuthenticationService.givenSsoUrl(A_REDIRECT_URI, A_DEVICE_ID, provider.id, SSOAction.LOGIN, result = A_SSO_URL)

        val result = viewModel.fetchSsoUrl(A_REDIRECT_URI, A_DEVICE_ID, provider, SSOAction.LOGIN)

        result shouldBeEqualTo A_SSO_URL
        test
                .assertStatesChanges(
                        initialState,
                        { copy(selectedAuthenticationState = SelectedAuthenticationState(SSO_REGISTRATION_DESCRIPTION)) }
                )
                .finish()
    }

    private fun viewModelWith(state: OnboardingViewState) {
        OnboardingViewModel(
                state,
                fakeContext.instance,
                fakeAuthenticationService,
                fakeActiveSessionHolder.instance,
                fakeHomeServerConnectionConfigFactory.instance,
                ReAuthHelper(),
                FakeStringProvider().instance,
                fakeHomeServerHistoryService,
                fakeVectorFeatures,
                FakeAnalyticsTracker(),
                fakeUriFilenameResolver.instance,
                fakeDirectLoginUseCase.instance,
                fakeStartAuthenticationFlowUseCase.instance,
                FakeVectorOverrides(),
                fakeRegistrationActionHandler.instance,
                TestBuildVersionSdkIntProvider().also { it.value = Build.VERSION_CODES.O },
                fakeConfigureAndStartSessionUseCase,
                NoOpMdmService()
        ).also {
            viewModel = it
            initialState = state
        }
    }

    private fun givenPictureSelected(fileUri: Uri, filename: String): OnboardingViewState {
        val initialStateWithPicture = OnboardingViewState(personalizationState = PersonalizationState(selectedPictureUri = fileUri))
        fakeUriFilenameResolver.givenFilename(fileUri, name = filename)
        return initialStateWithPicture
    }

    private fun expectedProfilePictureSuccessStates(state: OnboardingViewState) = listOf(
            state,
            state.copy(isLoading = true),
            state.copy(isLoading = false)
    )

    private fun expectedProfilePictureFailureStates(state: OnboardingViewState) = listOf(
            state,
            state.copy(isLoading = true),
            state.copy(isLoading = false)
    )

    private fun expectedSuccessfulDisplayNameUpdateStates(): List<OnboardingViewState.() -> OnboardingViewState> {
        return listOf(
                { copy(isLoading = true) },
                { copy(isLoading = false, personalizationState = personalizationState.copy(displayName = A_DISPLAY_NAME)) }
        )
    }

    private fun givenSuccessfullyCreatesAccount(homeServerCapabilities: HomeServerCapabilities) {
        fakeSession.fakeHomeServerCapabilitiesService.givenCapabilities(homeServerCapabilities)
        givenInitialisesSession(fakeSession)
    }

    private fun givenInitialisesSession(session: Session) {
        fakeActiveSessionHolder.expectSetsActiveSession(session)
        fakeAuthenticationService.expectReset()
        fakeSession.expectStartsSyncing(fakeConfigureAndStartSessionUseCase)
    }

    private fun givenRegistrationResultFor(action: RegisterAction, result: RegistrationActionHandler.Result) {
        givenRegistrationResultsFor(listOf(action to result))
    }

    private fun givenRegistrationResultsFor(results: List<Pair<RegisterAction, RegistrationActionHandler.Result>>) {
        fakeRegistrationActionHandler.givenResultsFor(results)
    }

    private fun givenCanSuccessfullyUpdateHomeserver(
            homeserverUrl: String,
            resultingState: SelectedHomeserverState,
            config: HomeServerConnectionConfig = A_HOMESERVER_CONFIG,
            fingerprints: List<Fingerprint>? = null,
    ) {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(homeserverUrl, fingerprints, config)
        fakeStartAuthenticationFlowUseCase.givenResult(config, StartAuthenticationResult(isHomeserverOutdated = false, resultingState))
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.StartRegistration)
        fakeHomeServerHistoryService.expectUrlToBeAdded(config.homeServerUri.toString())
    }

    private fun givenUpdatingHomeserverErrors(homeserverUrl: String, resultingState: SelectedHomeserverState, error: Throwable) {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(homeserverUrl, fingerprints = null, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenResult(A_HOMESERVER_CONFIG, StartAuthenticationResult(isHomeserverOutdated = false, resultingState))
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.Error(error))
        fakeHomeServerHistoryService.expectUrlToBeAdded(A_HOMESERVER_CONFIG.homeServerUri.toString())
    }

    private fun givenUserNameIsAvailable(userName: String) {
        fakeAuthenticationService.givenRegistrationWizard(FakeRegistrationWizard().also { it.givenUserNameIsAvailable(userName) })
    }

    private fun givenUserNameIsUnavailable(userName: String, failure: Failure.ServerError) {
        fakeAuthenticationService.givenRegistrationWizard(FakeRegistrationWizard().also { it.givenUserNameIsUnavailable(userName, failure) })
    }

    private fun availableRegistrationState(userName: String, homeServerUrl: String) = RegistrationState(
            isUserNameAvailable = true,
            selectedMatrixId = "@$userName:${homeServerUrl.removePrefix("https://")}"
    )

    private fun initialRegistrationState(homeServerUrl: String) = initialState.copy(
            onboardingFlow = OnboardingFlow.SignUp, selectedHomeserver = SelectedHomeserverState(userFacingUrl = homeServerUrl)
    )

    private fun givenHomeserverSelectionFailsWithNetworkError() {
        fakeContext.givenHasConnection()
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, fingerprints = null, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenHomeserverUnavailable(A_HOMESERVER_CONFIG)
    }

    private fun givenHomeserverSelectionFailsWith(cause: Throwable) {
        fakeContext.givenHasConnection()
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, fingerprints = null, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenErrors(A_HOMESERVER_CONFIG, cause)
    }
}

private fun HomeServerCapabilities.toPersonalisationState(userId: String, displayName: String? = null) = PersonalizationState(
        userId = userId,
        supportsChangingDisplayName = canChangeDisplayName,
        supportsChangingProfilePicture = canChangeAvatar,
        displayName = displayName,
)
