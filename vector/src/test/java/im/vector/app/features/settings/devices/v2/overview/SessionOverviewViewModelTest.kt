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

package im.vector.app.features.settings.devices.v2.overview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PusherFixture.aPusher
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.SessionParams

private const val A_SESSION_ID = "session-id"

class SessionOverviewViewModelTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule(testDispatcher = testDispatcher)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val args = SessionOverviewArgs(
            deviceId = A_SESSION_ID
    )
    private val fakeSession = FakeSession()
    private val getDeviceFullInfoUseCase = mockk<GetDeviceFullInfoUseCase>(relaxed = true)

    private fun createViewModel() = SessionOverviewViewModel(
            initialState = SessionOverviewViewState(args),
            session = fakeSession,
            getDeviceFullInfoUseCase = getDeviceFullInfoUseCase
    )

    @Test
    fun `given the viewModel has been initialized then viewState is updated with session info`() {
        val sessionParams = givenIdForSession(A_SESSION_ID)
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID) } returns flowOf(deviceFullInfo)
        val expectedState = SessionOverviewViewState(
                deviceId = A_SESSION_ID,
                isCurrentSession = true,
                deviceInfo = Success(deviceFullInfo),
                pushers = Loading(),
        )

        val viewModel = createViewModel()

        viewModel.test()
                .assertLatestState { state -> state == expectedState }
                .finish()
        verify { sessionParams.deviceId }
        verify { getDeviceFullInfoUseCase.execute(A_SESSION_ID) }
    }

    private fun givenIdForSession(deviceId: String): SessionParams {
        val sessionParams = mockk<SessionParams>()
        every { sessionParams.deviceId } returns deviceId
        fakeSession.givenSessionParams(sessionParams)
        return sessionParams
    }

    @Test
    fun `when viewModel init, then observe pushers and emit to state`() {
        val pushers = listOf(aPusher(deviceId = A_SESSION_ID))
        fakeSession.pushersService().givenPushersLive(pushers)

        val viewModel = createViewModel()

        viewModel.test()
                .assertLatestState { state -> state.pushers.invoke() == pushers }
                .finish()
    }

    @Test
    fun `when handle TogglePushNotifications, then toggle enabled for device pushers`() {
        val pushers = listOf(
                aPusher(deviceId = A_SESSION_ID, enabled = false),
                aPusher(deviceId = "another id", enabled = false)
        )
        fakeSession.pushersService().givenPushersLive(pushers)

        val viewModel = createViewModel()
        viewModel.handle(SessionOverviewAction.TogglePushNotifications(A_SESSION_ID, true))

        fakeSession.pushersService().verifyOnlyGetPushersLiveAndTogglePusherCalled(pushers.first(), true)
    }
}
