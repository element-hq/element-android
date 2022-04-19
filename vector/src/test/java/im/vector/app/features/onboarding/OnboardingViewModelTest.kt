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
import im.vector.app.test.fakes.FakeRegisterActionHandler
import im.vector.app.test.fakes.FakeRegistrationWizard
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStartAuthenticationFlowUseCase
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFilenameResolver
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorOverrides
import im.vector.app.test.fixtures.aHomeServerCapabilities
import im.vector.app.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

private const val A_DISPLAY_NAME = "a display name"
private const val A_PICTURE_FILENAME = "a-picture.png"
private val AN_ERROR = RuntimeException("an error!")
private val A_LOADABLE_REGISTER_ACTION = RegisterAction.StartRegistration
private val A_NON_LOADABLE_REGISTER_ACTION = RegisterAction.CheckIfEmailHasBeenValidated(delayMillis = -1L)
private val A_RESULT_IGNORED_REGISTER_ACTION = RegisterAction.AddThreePid(RegisterThreePid.Email("an email"))
private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canChangeDisplayName = true, canChangeAvatar = true)
private val AN_IGNORED_FLOW_RESULT = FlowResult(missingStages = emptyList(), completedStages = emptyList())
private val ANY_CONTINUING_REGISTRATION_RESULT = RegistrationResult.FlowResponse(AN_IGNORED_FLOW_RESULT)
private val A_LOGIN_OR_REGISTER_ACTION = OnboardingAction.LoginOrRegister("@a-user:id.org", "a-password", "a-device-name")
private const val A_HOMESERVER_URL = "https://edited-homeserver.org"
private val A_HOMESERVER_CONFIG = HomeServerConnectionConfig(FakeUri().instance)
private val SELECTED_HOMESERVER_STATE = SelectedHomeserverState(preferredLoginMode = LoginMode.Password)

class OnboardingViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeUri = FakeUri()
    private val fakeContext = FakeContext()
    private val initialState = OnboardingViewState()
    private val fakeSession = FakeSession()
    private val fakeUriFilenameResolver = FakeUriFilenameResolver()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder(fakeSession)
    private val fakeAuthenticationService = FakeAuthenticationService()
    private val fakeRegisterActionHandler = FakeRegisterActionHandler()
    private val fakeDirectLoginUseCase = FakeDirectLoginUseCase()
    private val fakeVectorFeatures = FakeVectorFeatures()
    private val fakeHomeServerConnectionConfigFactory = FakeHomeServerConnectionConfigFactory()
    private val fakeStartAuthenticationFlowUseCase = FakeStartAuthenticationFlowUseCase()
    private val fakeHomeServerHistoryService = FakeHomeServerHistoryService()

    lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModel = createViewModel()
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
        val initialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingDisplayName = true, supportsChangingProfilePicture = false))
        viewModel = createViewModel(initialState)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseDisplayName)
                .finish()
    }

    @Test
    fun `given only supports changing profile picture, when handling PersonalizeProfile, then emits contents choose profile picture`() = runTest {
        val initialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingDisplayName = false, supportsChangingProfilePicture = true))
        viewModel = createViewModel(initialState)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                .finish()
    }

    @Test
    fun `given has sign in with matrix id sign mode, when handling login or register action, then logs in directly`() = runTest {
        val initialState = initialState.copy(signMode = SignMode.SignInWithMatrixId)
        viewModel = createViewModel(initialState)
        fakeDirectLoginUseCase.givenSuccessResult(A_LOGIN_OR_REGISTER_ACTION, config = null, result = fakeSession)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(A_LOGIN_OR_REGISTER_ACTION)

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
        val initialState = initialState.copy(signMode = SignMode.SignInWithMatrixId)
        viewModel = createViewModel(initialState)
        fakeDirectLoginUseCase.givenFailureResult(A_LOGIN_OR_REGISTER_ACTION, config = null, cause = AN_ERROR)
        givenInitialisesSession(fakeSession)
        val test = viewModel.test()

        viewModel.handle(A_LOGIN_OR_REGISTER_ACTION)

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
                .assertEvents(OnboardingViewEvents.RegistrationFlowResult(ANY_CONTINUING_REGISTRATION_RESULT.flowResult, isRegistrationStarted = true))
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
                .assertEvents(OnboardingViewEvents.RegistrationFlowResult(ANY_CONTINUING_REGISTRATION_RESULT.flowResult, isRegistrationStarted = true))
                .finish()
    }

    @Test
    fun `given register action is non loadable, when handling action, then posts next steps without loading`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_NON_LOADABLE_REGISTER_ACTION, ANY_CONTINUING_REGISTRATION_RESULT)

        viewModel.handle(OnboardingAction.PostRegisterAction(A_NON_LOADABLE_REGISTER_ACTION))

        test
                .assertState(initialState)
                .assertEvents(OnboardingViewEvents.RegistrationFlowResult(ANY_CONTINUING_REGISTRATION_RESULT.flowResult, isRegistrationStarted = true))
                .finish()
    }

    @Test
    fun `given register action ignores result, when handling action, then does nothing on success`() = runTest {
        val test = viewModel.test()
        givenRegistrationResultFor(A_RESULT_IGNORED_REGISTER_ACTION, RegistrationResult.FlowResponse(AN_IGNORED_FLOW_RESULT))

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
    fun `given when editing homeserver, then updates selected homeserver state and emits edited event`() = runTest {
        val test = viewModel.test()
        fakeHomeServerConnectionConfigFactory.givenConfigFor(A_HOMESERVER_URL, A_HOMESERVER_CONFIG)
        fakeStartAuthenticationFlowUseCase.givenResult(A_HOMESERVER_CONFIG, StartAuthenticationResult(false, SELECTED_HOMESERVER_STATE))
        fakeHomeServerHistoryService.expectUrlToBeAdded(A_HOMESERVER_CONFIG.homeServerUri.toString())

        viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(A_HOMESERVER_URL))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, selectedHomeserver = SELECTED_HOMESERVER_STATE) },
                )
                .assertEvents(OnboardingViewEvents.OnHomeserverEdited)
                .finish()
    }

    @Test
    fun `given personalisation enabled, when registering account, then updates state and emits account created event`() = runTest {
        fakeVectorFeatures.givenPersonalisationEnabled()
        givenRegistrationResultFor(A_LOADABLE_REGISTER_ACTION, RegistrationResult.Success(fakeSession))
        givenSuccessfullyCreatesAccount(A_HOMESERVER_CAPABILITIES)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PostRegisterAction(A_LOADABLE_REGISTER_ACTION))

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
    fun `given personalisation enabled and registration has started and has dummy step to do, when handling action, then ignores other steps and executes dummy`() = runTest {
        fakeVectorFeatures.givenPersonalisationEnabled()
        givenSuccessfulRegistrationForStartAndDummySteps(missingStages = listOf(Stage.Dummy(mandatory = true)))
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.PostRegisterAction(A_LOADABLE_REGISTER_ACTION))

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
    fun `given changing profile picture is supported, when updating display name, then updates upstream user display name and moves to choose profile picture`() = runTest {
        val personalisedInitialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = true))
        viewModel = createViewModel(personalisedInitialState)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStatesChanges(personalisedInitialState, expectedSuccessfulDisplayNameUpdateStates())
                .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                .finish()
        fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
    }

    @Test
    fun `given changing profile picture is not supported, when updating display name, then updates upstream user display name and completes personalization`() = runTest {
        val personalisedInitialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = false))
        viewModel = createViewModel(personalisedInitialState)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStatesChanges(personalisedInitialState, expectedSuccessfulDisplayNameUpdateStates())
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
        fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
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
        val initialStateWithPicture = givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME)
        viewModel = createViewModel(initialStateWithPicture)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureSuccessStates(initialStateWithPicture))
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
        fakeSession.fakeProfileService.verifyAvatarUpdated(fakeSession.myUserId, fakeUri.instance, A_PICTURE_FILENAME)
    }

    @Test
    fun `given upstream update avatar fails, when saving selected profile picture, then emits failure event`() = runTest {
        fakeSession.fakeProfileService.givenUpdateAvatarErrors(AN_ERROR)
        val initialStateWithPicture = givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME)
        viewModel = createViewModel(initialStateWithPicture)
        val test = viewModel.test()

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureFailureStates(initialStateWithPicture))
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

    private fun createViewModel(state: OnboardingViewState = initialState): OnboardingViewModel {
        return OnboardingViewModel(
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
                fakeRegisterActionHandler.instance,
                fakeDirectLoginUseCase.instance,
                fakeStartAuthenticationFlowUseCase.instance,
                FakeVectorOverrides()
        )
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

    private fun givenSuccessfulRegistrationForStartAndDummySteps(missingStages: List<Stage>) {
        val flowResult = FlowResult(missingStages = missingStages, completedStages = emptyList())
        givenRegistrationResultsFor(listOf(
                A_LOADABLE_REGISTER_ACTION to RegistrationResult.FlowResponse(flowResult),
                RegisterAction.RegisterDummy to RegistrationResult.Success(fakeSession)
        ))
        givenSuccessfullyCreatesAccount(A_HOMESERVER_CAPABILITIES)
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

    private fun givenRegistrationResultFor(action: RegisterAction, result: RegistrationResult) {
        givenRegistrationResultsFor(listOf(action to result))
    }

    private fun givenRegistrationResultsFor(results: List<Pair<RegisterAction, RegistrationResult>>) {
        fakeAuthenticationService.givenRegistrationStarted(true)
        val registrationWizard = FakeRegistrationWizard()
        fakeAuthenticationService.givenRegistrationWizard(registrationWizard)
        fakeRegisterActionHandler.givenResultsFor(registrationWizard, results)
    }
}

private fun HomeServerCapabilities.toPersonalisationState() = PersonalizationState(
        supportsChangingDisplayName = canChangeDisplayName,
        supportsChangingProfilePicture = canChangeAvatar
)
