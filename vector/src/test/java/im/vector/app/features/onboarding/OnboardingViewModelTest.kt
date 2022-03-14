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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeHomeServerConnectionConfigFactory
import im.vector.app.test.fakes.FakeHomeServerHistoryService
import im.vector.app.test.fakes.FakeRegistrationWizard
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFilenameResolver
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorOverrides
import im.vector.app.test.test
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

private const val A_DISPLAY_NAME = "a display name"
private const val A_PICTURE_FILENAME = "a-picture.png"
private val AN_ERROR = RuntimeException("an error!")
private val AN_UNSUPPORTED_PERSONALISATION_STATE = PersonalizationState(
        supportsChangingDisplayName = false,
        supportsChangingProfilePicture = false
)

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

    lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModel = createViewModel()
    }

    @Test
    fun `when handling PostViewEvent then emits contents as view event`() = runBlockingTest {
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome))

        test
                .assertEvents(OnboardingViewEvents.OnTakeMeHome)
                .finish()
    }

    @Test
    fun `given supports changing display name when handling PersonalizeProfile then emits contents choose display name`() = runBlockingTest {
        val initialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingDisplayName = true, supportsChangingProfilePicture = false))
        viewModel = createViewModel(initialState)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseDisplayName)
                .finish()
    }

    @Test
    fun `given only supports changing profile picture when handling PersonalizeProfile then emits contents choose profile picture`() = runBlockingTest {
        val initialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingDisplayName = false, supportsChangingProfilePicture = true))
        viewModel = createViewModel(initialState)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.PersonalizeProfile)

        test
                .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                .finish()
    }

    @Test
    fun `given homeserver does not support personalisation when registering account then updates state and emits account created event`() = runBlockingTest {
        fakeSession.fakeHomeServerCapabilitiesService.givenCapabilities(HomeServerCapabilities(canChangeDisplayName = false, canChangeAvatar = false))
        givenSuccessfullyCreatesAccount()
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.RegisterDummy)

        test
                .assertStates(
                        initialState,
                        initialState.copy(asyncRegistration = Loading()),
                        initialState.copy(
                                asyncLoginAction = Success(Unit),
                                asyncRegistration = Loading(),
                                personalizationState = AN_UNSUPPORTED_PERSONALISATION_STATE
                        ),
                        initialState.copy(
                                asyncLoginAction = Success(Unit),
                                asyncRegistration = Uninitialized,
                                personalizationState = AN_UNSUPPORTED_PERSONALISATION_STATE
                        )
                )
                .assertEvents(OnboardingViewEvents.OnAccountCreated)
                .finish()
    }

    @Test
    fun `given changing profile picture is supported when updating display name then updates upstream user display name and moves to choose profile picture`() = runBlockingTest {
        val personalisedInitialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = true))
        viewModel = createViewModel(personalisedInitialState)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(expectedSuccessfulDisplayNameUpdateStates(personalisedInitialState))
                .assertEvents(OnboardingViewEvents.OnChooseProfilePicture)
                .finish()
        fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
    }

    @Test
    fun `given changing profile picture is not supported when updating display name then updates upstream user display name and completes personalization`() = runBlockingTest {
        val personalisedInitialState = initialState.copy(personalizationState = PersonalizationState(supportsChangingProfilePicture = false))
        viewModel = createViewModel(personalisedInitialState)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(expectedSuccessfulDisplayNameUpdateStates(personalisedInitialState))
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
        fakeSession.fakeProfileService.verifyUpdatedName(fakeSession.myUserId, A_DISPLAY_NAME)
    }

    @Test
    fun `given upstream failure when handling display name update then emits failure event`() = runBlockingTest {
        val test = viewModel.test(this)
        fakeSession.fakeProfileService.givenSetDisplayNameErrors(AN_ERROR)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(
                        initialState,
                        initialState.copy(asyncDisplayName = Loading()),
                        initialState.copy(asyncDisplayName = Fail(AN_ERROR)),
                )
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `when handling profile picture selected then updates selected picture state`() = runBlockingTest {
        val test = viewModel.test(this)

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
    fun `given a selected picture when handling save selected profile picture then updates upstream avatar and completes personalization`() = runBlockingTest {
        val initialStateWithPicture = givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME)
        viewModel = createViewModel(initialStateWithPicture)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureSuccessStates(initialStateWithPicture))
                .assertEvents(OnboardingViewEvents.OnPersonalizationComplete)
                .finish()
        fakeSession.fakeProfileService.verifyAvatarUpdated(fakeSession.myUserId, fakeUri.instance, A_PICTURE_FILENAME)
    }

    @Test
    fun `given upstream update avatar fails when saving selected profile picture then emits failure event`() = runBlockingTest {
        fakeSession.fakeProfileService.givenUpdateAvatarErrors(AN_ERROR)
        val initialStateWithPicture = givenPictureSelected(fakeUri.instance, A_PICTURE_FILENAME)
        viewModel = createViewModel(initialStateWithPicture)
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(expectedProfilePictureFailureStates(initialStateWithPicture, AN_ERROR))
                .assertEvents(OnboardingViewEvents.Failure(AN_ERROR))
                .finish()
    }

    @Test
    fun `given no selected picture when saving selected profile picture then emits failure event`() = runBlockingTest {
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)

        test
                .assertStates(initialState)
                .assertEvent { it is OnboardingViewEvents.Failure && it.throwable is NullPointerException }
                .finish()
    }

    @Test
    fun `when handling profile picture skipped then completes personalization`() = runBlockingTest {
        val test = viewModel.test(this)

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
                FakeHomeServerConnectionConfigFactory().instance,
                ReAuthHelper(),
                FakeStringProvider().instance,
                FakeHomeServerHistoryService(),
                FakeVectorFeatures(),
                FakeAnalyticsTracker(),
                fakeUriFilenameResolver.instance,
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
            state.copy(asyncProfilePicture = Loading()),
            state.copy(asyncProfilePicture = Success(Unit))
    )

    private fun expectedProfilePictureFailureStates(state: OnboardingViewState, cause: Exception) = listOf(
            state,
            state.copy(asyncProfilePicture = Loading()),
            state.copy(asyncProfilePicture = Fail(cause))
    )

    private fun givenSuccessfullyCreatesAccount() {
        fakeActiveSessionHolder.expectSetsActiveSession(fakeSession)
        val registrationWizard = FakeRegistrationWizard().also { it.givenSuccessfulDummy(fakeSession) }
        fakeAuthenticationService.givenRegistrationWizard(registrationWizard)
        fakeAuthenticationService.expectReset()
        fakeSession.expectStartsSyncing()
    }

    private fun expectedSuccessfulDisplayNameUpdateStates(personalisedInitialState: OnboardingViewState): List<OnboardingViewState> {
        return listOf(
                personalisedInitialState,
                personalisedInitialState.copy(asyncDisplayName = Loading()),
                personalisedInitialState.copy(
                        asyncDisplayName = Success(Unit),
                        personalizationState = personalisedInitialState.personalizationState.copy(displayName = A_DISPLAY_NAME)
                )
        )
    }
}
