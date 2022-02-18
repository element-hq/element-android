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
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.DefaultVectorOverrides
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeHomeServerConnectionConfigFactory
import im.vector.app.test.fakes.FakeHomeServerHistoryService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeUri
import im.vector.app.test.fakes.FakeUriFilenameResolver
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.test
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val A_DISPLAY_NAME = "a display name"
private const val A_PICTURE_FILENAME = "a-picture.png"
private val AN_ERROR = RuntimeException("an error!")

class OnboardingViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeUri = FakeUri()
    private val fakeContext = FakeContext()
    private val initialState = OnboardingViewState()
    private val fakeSession = FakeSession()
    private val fakeUriFilenameResolver = FakeUriFilenameResolver()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder(fakeSession)

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
    fun `when handling display name update then updates upstream user display name`() = runBlockingTest {
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(
                        initialState,
                        initialState.copy(asyncDisplayName = Loading()),
                        initialState.copy(
                                asyncDisplayName = Success(Unit),
                                personalizationState = initialState.personalizationState.copy(displayName = A_DISPLAY_NAME)
                        )
                )
                .assertEvents(OnboardingViewEvents.OnDisplayNameUpdated)
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
                FakeAuthenticationService(),
                fakeActiveSessionHolder.instance,
                FakeHomeServerConnectionConfigFactory().instance,
                ReAuthHelper(),
                FakeStringProvider().instance,
                FakeHomeServerHistoryService(),
                FakeVectorFeatures(),
                FakeAnalyticsTracker(),
                fakeUriFilenameResolver.instance,
                DefaultVectorOverrides()
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
}
