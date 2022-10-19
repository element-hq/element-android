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

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.SystemClock
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.GetDeviceFullInfoListUseCase
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verifyAll
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val A_TITLE_RES_ID = 1

class OtherSessionsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val defaultArgs = OtherSessionsArgs(
            titleResourceId = A_TITLE_RES_ID,
            defaultFilter = DeviceManagerFilterType.ALL_SESSIONS,
            excludeCurrentDevice = false,
    )

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeGetDeviceFullInfoListUseCase = mockk<GetDeviceFullInfoListUseCase>()
    private val fakeRefreshDevicesUseCaseUseCase = mockk<RefreshDevicesUseCase>()

    private fun createViewModel(args: OtherSessionsArgs = defaultArgs) = OtherSessionsViewModel(
            initialState = OtherSessionsViewState(args),
            activeSessionHolder = fakeActiveSessionHolder.instance,
            getDeviceFullInfoListUseCase = fakeGetDeviceFullInfoListUseCase,
            refreshDevicesUseCase = fakeRefreshDevicesUseCaseUseCase,
    )

    @Before
    fun setup() {
        // Needed for internal usage of Flow<T>.throttleFirst() inside the ViewModel
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1234

        givenVerificationService()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel has been initialized then viewState is updated with devices list`() {
        // Given
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(devices),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.test()
                .assertLatestState { state -> state == expectedState }
                .finish()
        verifyAll { fakeGetDeviceFullInfoListUseCase.execute(defaultArgs.defaultFilter, defaultArgs.excludeCurrentDevice) }
    }

    private fun givenGetDeviceFullInfoListReturns(devices: List<DeviceFullInfo>) {
        every { fakeGetDeviceFullInfoListUseCase.execute(any(), any()) } returns flowOf(devices)
    }

    private fun givenVerificationService(): FakeVerificationService {
        val fakeVerificationService = fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
        fakeVerificationService.givenAddListenerSucceeds()
        fakeVerificationService.givenRemoveListenerSucceeds()
        return fakeVerificationService
    }
}
