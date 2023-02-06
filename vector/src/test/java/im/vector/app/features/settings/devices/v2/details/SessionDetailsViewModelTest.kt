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

package im.vector.app.features.settings.devices.v2.details

import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.utils.CopyToClipboardUseCase
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.overview.GetDeviceFullInfoUseCase
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

private const val A_SESSION_ID = "session-id"
private const val A_TEXT = "text"

class SessionDetailsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val args = SessionDetailsArgs(
            deviceId = A_SESSION_ID
    )
    private val getDeviceFullInfoUseCase = mockk<GetDeviceFullInfoUseCase>()
    private val copyToClipboardUseCase = mockk<CopyToClipboardUseCase>()

    private fun createViewModel() = SessionDetailsViewModel(
            initialState = SessionDetailsViewState(args),
            getDeviceFullInfoUseCase = getDeviceFullInfoUseCase,
            copyToClipboardUseCase = copyToClipboardUseCase,
    )

    @Test
    fun `given the viewModel has been initialized then viewState is updated with session info`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID) } returns flowOf(deviceFullInfo)
        val expectedState = SessionDetailsViewState(
                deviceId = A_SESSION_ID,
                deviceFullInfo = Success(deviceFullInfo)
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.test()
                .assertLatestState { state -> state == expectedState }
                .finish()
        verify { getDeviceFullInfoUseCase.execute(A_SESSION_ID) }
    }

    @Test
    fun `given copyToClipboard action when viewModel handle it then related use case is executed and viewEvent is updated`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        val deviceInfo = mockk<DeviceInfo>()
        every { deviceFullInfo.deviceInfo } returns deviceInfo
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID) } returns flowOf(deviceFullInfo)
        val action = SessionDetailsAction.CopyToClipboard(A_TEXT)
        every { copyToClipboardUseCase.execute(any()) } just runs

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { it is SessionDetailsViewEvent.ContentCopiedToClipboard }
                .finish()
        verify { copyToClipboardUseCase.execute(A_TEXT) }
    }
}
