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

package im.vector.app.features.settings.devices.v2.rename

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.overview.GetDeviceFullInfoUseCase
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

private const val A_SESSION_ID = "session-id"
private const val A_SESSION_NAME = "session-name"
private const val AN_EDITED_SESSION_NAME = "edited-session-name"

class RenameSessionViewModelTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule(testDispatcher = testDispatcher)

    private val args = RenameSessionArgs(
            deviceId = A_SESSION_ID
    )
    private val getDeviceFullInfoUseCase = mockk<GetDeviceFullInfoUseCase>()
    private val renameSessionUseCase = mockk<RenameSessionUseCase>()

    private fun createViewModel() = RenameSessionViewModel(
            initialState = RenameSessionViewState(args),
            getDeviceFullInfoUseCase = getDeviceFullInfoUseCase,
            renameSessionUseCase = renameSessionUseCase,
    )

    @Test
    fun `given the viewModel has been initialized then viewState is updated with current session name`() {
        // Given
        givenSessionWithName(A_SESSION_NAME)
        val expectedState = RenameSessionViewState(
                deviceId = A_SESSION_ID,
                editedDeviceName = A_SESSION_NAME,
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.test()
                .assertLatestState { state -> state == expectedState }
                .finish()
        verify {
            getDeviceFullInfoUseCase.execute(A_SESSION_ID)
        }
    }

    @Test
    fun `given a new edited name when handling edit name locally action then view state is updated accordingly`() {
        // Given
        givenSessionWithName(A_SESSION_NAME)
        val action = RenameSessionAction.EditLocally(AN_EDITED_SESSION_NAME)
        val expectedState = RenameSessionViewState(
                deviceId = A_SESSION_ID,
                editedDeviceName = AN_EDITED_SESSION_NAME,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given current edited name when handling save modifications action with success then correct view event is posted`() {
        // Given
        givenSessionWithName(A_SESSION_NAME)
        coEvery { renameSessionUseCase.execute(A_SESSION_ID, A_SESSION_NAME) } returns Result.success(Unit)
        val action = RenameSessionAction.SaveModifications

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event is RenameSessionViewEvent.SessionRenamed }
                .finish()
    }

    @Test
    fun `given current edited name when handling save modifications action with error then correct view event is posted`() {
        // Given
        givenSessionWithName(A_SESSION_NAME)
        val error = Exception()
        coEvery { renameSessionUseCase.execute(A_SESSION_ID, A_SESSION_NAME) } returns Result.failure(error)
        val action = RenameSessionAction.SaveModifications

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event is RenameSessionViewEvent.Failure && event.throwable == error }
                .finish()
    }

    private fun givenSessionWithName(sessionName: String) {
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.deviceInfo.displayName } returns sessionName
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID) } returns flowOf(deviceFullInfo)
    }
}
