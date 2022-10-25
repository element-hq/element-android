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
import im.vector.app.test.fixtures.aDeviceFullInfo
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
private const val A_DEVICE_ID = "device-id"

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

    private fun givenVerificationService(): FakeVerificationService {
        val fakeVerificationService = fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
        fakeVerificationService.givenAddListenerSucceeds()
        fakeVerificationService.givenRemoveListenerSucceeds()
        return fakeVerificationService
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel has been initialized then viewState is updated with devices list`() {
        // Given
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
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

    @Test
    fun `given filter devices action when handling the action then viewState is updated with filter option and devices are filtered`() {
        // Given
        val filterType = DeviceManagerFilterType.UNVERIFIED
        val devices = mockk<List<DeviceFullInfo>>()
        val filteredDevices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        givenGetDeviceFullInfoListReturns(filterType = filterType, filteredDevices)
        val expectedState = OtherSessionsViewState(
                devices = Success(filteredDevices),
                currentFilter = filterType,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.FilterDevices(filterType))

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
        verifyAll {
            fakeGetDeviceFullInfoListUseCase.execute(defaultArgs.defaultFilter, defaultArgs.excludeCurrentDevice)
            fakeGetDeviceFullInfoListUseCase.execute(filterType, defaultArgs.excludeCurrentDevice)
        }
    }

    @Test
    fun `given enable select mode action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo = aDeviceFullInfo(A_DEVICE_ID, isSelected = false)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo.copy(isSelected = true))),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = true,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.EnableSelectMode(A_DEVICE_ID))

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given disable select mode action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID, isSelected = true)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1.copy(isSelected = false), deviceFullInfo2.copy(isSelected = false))),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.DisableSelectMode)

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given toggle selection for device action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo = aDeviceFullInfo(A_DEVICE_ID, isSelected = false)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo.copy(isSelected = true))),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.ToggleSelectionForDevice(A_DEVICE_ID))

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given select all action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1.copy(isSelected = true), deviceFullInfo2.copy(isSelected = true))),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.SelectAll)

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given deselect all action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val expectedState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1.copy(isSelected = false), deviceFullInfo2.copy(isSelected = false))),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = false,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.DeselectAll)

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    private fun givenGetDeviceFullInfoListReturns(
            filterType: DeviceManagerFilterType,
            devices: List<DeviceFullInfo>,
    ) {
        every { fakeGetDeviceFullInfoListUseCase.execute(filterType, any()) } returns flowOf(devices)
    }
}
