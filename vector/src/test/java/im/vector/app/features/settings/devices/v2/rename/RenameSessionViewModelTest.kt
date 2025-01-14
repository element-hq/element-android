/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.rename

import com.airbnb.mvrx.test.MavericksTestRule
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
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

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
    fun `given the original device name has not been retrieved when handling init with last edited name action then view state and view events are updated`() {
        // Given
        givenSessionWithName(A_SESSION_NAME)
        val action = RenameSessionAction.InitWithLastEditedName
        val expectedState = RenameSessionViewState(
                deviceId = A_SESSION_ID,
                editedDeviceName = A_SESSION_NAME,
        )
        val expectedEvent = RenameSessionViewEvent.Initialized(
                deviceName = A_SESSION_NAME,
        )
        val viewModel = createViewModel()
        viewModel.hasRetrievedOriginalDeviceName = false

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.assertLatestState { state -> state == expectedState }
                .assertEvent { event -> event == expectedEvent }
                .finish()
        verify {
            getDeviceFullInfoUseCase.execute(A_SESSION_ID)
        }
    }

    @Test
    fun `given the original device name has been retrieved when handling init with last edited name action then view state and view events are updated`() {
        // Given
        val action = RenameSessionAction.InitWithLastEditedName
        val expectedState = RenameSessionViewState(
                deviceId = A_SESSION_ID,
                editedDeviceName = AN_EDITED_SESSION_NAME,
        )
        val expectedEvent = RenameSessionViewEvent.Initialized(
                deviceName = AN_EDITED_SESSION_NAME,
        )
        val viewModel = createViewModel()
        viewModel.handle(RenameSessionAction.EditLocally(AN_EDITED_SESSION_NAME))
        viewModel.hasRetrievedOriginalDeviceName = true

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.assertLatestState { state -> state == expectedState }
                .assertEvent { event -> event == expectedEvent }
                .finish()
        verify(inverse = true) {
            getDeviceFullInfoUseCase.execute(A_SESSION_ID)
        }
    }

    @Test
    fun `given a new edited name when handling edit name locally action then view state is updated accordingly`() {
        // Given
        val action = RenameSessionAction.EditLocally(AN_EDITED_SESSION_NAME)
        val expectedState = RenameSessionViewState(
                deviceId = A_SESSION_ID,
                editedDeviceName = AN_EDITED_SESSION_NAME,
        )
        val viewModel = createViewModel()

        // When
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
        coEvery { renameSessionUseCase.execute(A_SESSION_ID, any()) } returns Result.success(Unit)
        val action = RenameSessionAction.SaveModifications
        val viewModel = createViewModel()

        // When
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
        val error = Exception()
        coEvery { renameSessionUseCase.execute(A_SESSION_ID, any()) } returns Result.failure(error)
        val action = RenameSessionAction.SaveModifications
        val viewModel = createViewModel()

        // When
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
