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

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeHomeServerConnectionConfigFactory
import im.vector.app.test.fakes.FakeHomeServerHistoryService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.test
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val A_DISPLAY_NAME = "a display name"

class OnboardingViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeContext = FakeContext()
    lateinit var viewModel: OnboardingViewModel
    private val initialState = OnboardingViewState()
    private val fakeSession = FakeSession()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder(fakeSession)

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
    fun `when handling display name updates action then updates user display name and emits name updated event`() = runBlockingTest {
        val test = viewModel.test(this)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(
                        initialState,
                        initialState.copy(asyncDisplayName = Loading()),
                        initialState.copy(asyncDisplayName = Success(Unit)),
                )
                .assertEvents(OnboardingViewEvents.OnDisplayNameUpdated)
                .finish()
    }

    @Test
    fun `given failure when handling display name updates action then emits failure event`() = runBlockingTest {
        val test = viewModel.test(this)
        val errorCause = RuntimeException("an error!")
        fakeSession.fakeProfileService.givenSetDisplayNameErrors(errorCause)

        viewModel.handle(OnboardingAction.UpdateDisplayName(A_DISPLAY_NAME))

        test
                .assertStates(
                        initialState,
                        initialState.copy(asyncDisplayName = Loading()),
                        initialState.copy(asyncDisplayName = Fail(errorCause)),
                )
                .assertEvents(OnboardingViewEvents.Failure(errorCause))
                .finish()
    }

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel(
                initialState,
                fakeContext.instance,
                FakeAuthenticationService(),
                fakeActiveSessionHolder.instance,
                FakeHomeServerConnectionConfigFactory().instance,
                ReAuthHelper(),
                FakeStringProvider().instance,
                FakeHomeServerHistoryService(),
                FakeVectorFeatures(),
                FakeAnalyticsTracker()
        )
    }
}
