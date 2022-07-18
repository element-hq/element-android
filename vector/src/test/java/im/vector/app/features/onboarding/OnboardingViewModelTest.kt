/*
 * Copyright (c) 2022 New Vector Ltd
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

import android.net.Uri
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.R
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.login.SignMode
import im.vector.app.features.onboarding.StartAuthenticationFlowUseCase.StartAuthenticationResult
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeDirectLoginUseCase
import im.vector.app.test.fakes.FakeHomeServerConnectionConfigFactory
import im.vector.app.test.fakes.FakeHomeServerHistoryService
import im.vector.app.test.fakes.FakeLoginWizard
import im.vector.app.test.fakes.FakeRegistrationActionHandler
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStartAuthenticationFlowUseCase
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFilenameResolver
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorOverrides
import im.vector.app.test.fakes.toTestString
import im.vector.app.test.fixtures.aBuildMeta
import im.vector.app.test.fixtures.aHomeServerCapabilities
import im.vector.app.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

private const val A_DISPLAY_NAME = "a display name"
private const val A_PICTURE_FILENAME = "a-picture.png"
private val AN_ERROR = RuntimeException("an error!")
private val A_LOADABLE_REGISTER_ACTION = RegisterAction.StartRegistration
private val A_NON_LOADABLE_REGISTER_ACTION = RegisterAction.CheckIfEmailHasBeenValidated(delayMillis = -1L)
private val A_RESULT_IGNORED_REGISTER_ACTION = RegisterAction.SendAgainThreePid
private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canChangeDisplayName = true, canChangeAvatar = true)
private val ANY_CONTINUING_REGISTRATION_RESULT = RegistrationActionHandler.Result.NextStage(Stage.Dummy(mandatory = true))
private val A_DIRECT_LOGIN = OnboardingAction.AuthenticateAction.LoginDirect("@a-user:id.org", "a-password", "a-device-name")
private const val A_HOMESERVER_URL = "https://edited-homeserver.org"
private val A_HOMESERVER_CONFIG = HomeServerConnectionConfig(FakeUri().instance)
private val SELECTED_HOMESERVER_STATE = SelectedHomeserverState(preferredLoginMode = LoginMode.Password)
private val SELECTED_HOMESERVER_STATE_SUPPORTED_LOGOUT_DEVICES = SelectedHomeserverState(isLogoutDevicesSupported = true)
private const val AN_EMAIL = "hello@example.com"
private const val A_PASSWORD = "a-password"

class OnboardingViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

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

    private var initialState = OnboardingViewState()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModelWith(initialState)
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
                        { copy(isLoading = false) }
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
    fun `given unavailable deeplink, when selecting homeserver, then emits failure with default homeserver as retry action`() = runTest {
        fakeContext.givenHasConnection()
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenHomeserverUnavailable(A_HOMESERVER_CONFIG)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.InitWith(LoginConfig(A_HOMESERVER_URL, null)))
        viewModel.handle(OnboardingAction.HomeServerChange.SelectHomeServer(A_HOMESERVER_URL))

        val expectedRetryAction = OnboardingAction.HomeServerChange.SelectHomeServer("${R.string.matrix_org_server_url.toTestString()}/")
        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvents(OnboardingViewEvents.DeeplinkAuthenticationFailure(expectedRetryAction))
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
    fun `given a full matrix id, when maybe updating homeserver, then updates selected homeserver state and emits edited event`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        givenCanSuccessfullyUpdateHomeserver(A_HOMESERVER_URL, SELECTED_HOMESERVER_STATE)
        val test = viewModel.test()
        val fullMatrixId = "@a-user:${A_HOMESERVER_URL.removePrefix("https://")}"

        viewModel.handle(OnboardingAction.MaybeUpdateHomeserverFromMatrixId(fullMatrixId))

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
    fun `given a username, when maybe updating homeserver, then does nothing`() = runTest {
        viewModelWith(initialState.copy(onboardingFlow = OnboardingFlow.SignUp))
        val test = viewModel.test()
        val onlyUsername = "a-username"

        viewModel.handle(OnboardingAction.MaybeUpdateHomeserverFromMatrixId(onlyUsername))

        test
                .assertStates(initialState)
                .assertNoEvents()
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
    fun `given personalisation enabled, when registering account, then updates state and emits account created event`() = runTest {
        fakeVectorFeatures.givenPersonalisationEnabled()
        givenSuccessfullyCreatesAccount(A_HOMESERVER_CAPABILITIES)
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.RegistrationComplete(fakeSession))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.StartRegistration))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, personalizationState = A_HOMESERVER_CAPABILITIES.toPersonalisationState()) }
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
                aBuildMeta(),
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
        fakeSession.expectStartsSyncing()
    }

    private fun givenRegistrationResultFor(action: RegisterAction, result: RegistrationActionHandler.Result) {
        givenRegistrationResultsFor(listOf(action to result))
    }

    private fun givenRegistrationResultsFor(results: List<Pair<RegisterAction, RegistrationActionHandler.Result>>) {
        fakeRegistrationActionHandler.givenResultsFor(results)
    }

    private fun givenCanSuccessfullyUpdateHomeserver(homeserverUrl: String, resultingState: SelectedHomeserverState) {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(homeserverUrl, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenResult(A_HOMESERVER_CONFIG, StartAuthenticationResult(isHomeserverOutdated = false, resultingState))
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.StartRegistration)
        fakeHomeServerHistoryService.expectUrlToBeAdded(A_HOMESERVER_CONFIG.homeServerUri.toString())
    }

    private fun givenUpdatingHomeserverErrors(homeserverUrl: String, resultingState: SelectedHomeserverState, error: Throwable) {
        fakeHomeServerConnectionConfigFactory.givenConfigFor(homeserverUrl, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenResult(A_HOMESERVER_CONFIG, StartAuthenticationResult(isHomeserverOutdated = false, resultingState))
        givenRegistrationResultFor(RegisterAction.StartRegistration, RegistrationActionHandler.Result.Error(error))
        fakeHomeServerHistoryService.expectUrlToBeAdded(A_HOMESERVER_CONFIG.homeServerUri.toString())
    }
}

private fun HomeServerCapabilities.toPersonalisationState() = PersonalizationState(
        supportsChangingDisplayName = canChangeDisplayName,
        supportsChangingProfilePicture = canChangeAvatar
)
