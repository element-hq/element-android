/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.SystemClock
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.GetDeviceFullInfoListUseCase
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.ToggleIpAddressVisibilityUseCase
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakePendingAuthHandler
import im.vector.app.test.fakes.FakeSignoutSessionsUseCase
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.fixtures.aDeviceFullInfo
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth

private const val A_DEVICE_ID_1 = "device-id-1"
private const val A_DEVICE_ID_2 = "device-id-2"
private const val A_PASSWORD = "password"

class OtherSessionsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val defaultArgs = OtherSessionsArgs(
            defaultFilter = DeviceManagerFilterType.ALL_SESSIONS,
            excludeCurrentDevice = false,
    )

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeGetDeviceFullInfoListUseCase = mockk<GetDeviceFullInfoListUseCase>()
    private val fakeRefreshDevicesUseCase = mockk<RefreshDevicesUseCase>(relaxed = true)
    private val fakeSignoutSessionsUseCase = FakeSignoutSessionsUseCase()
    private val fakePendingAuthHandler = FakePendingAuthHandler()
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val toggleIpAddressVisibilityUseCase = mockk<ToggleIpAddressVisibilityUseCase>()

    private fun createViewModel(viewState: OtherSessionsViewState = OtherSessionsViewState(defaultArgs)) =
            OtherSessionsViewModel(
                    initialState = viewState,
                    activeSessionHolder = fakeActiveSessionHolder.instance,
                    getDeviceFullInfoListUseCase = fakeGetDeviceFullInfoListUseCase,
                    signoutSessionsUseCase = fakeSignoutSessionsUseCase.instance,
                    pendingAuthHandler = fakePendingAuthHandler.instance,
                    refreshDevicesUseCase = fakeRefreshDevicesUseCase,
                    vectorPreferences = fakeVectorPreferences.instance,
                    toggleIpAddressVisibilityUseCase = toggleIpAddressVisibilityUseCase,
            )

    @Before
    fun setup() {
        // Needed for internal usage of Flow<T>.throttleFirst() inside the ViewModel
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1234
        fakeActiveSessionHolder.fakeSession.fakeHomeServerCapabilitiesService.givenCapabilities(
                HomeServerCapabilities()
        )
        givenVerificationService().givenEventFlow()
        fakeVectorPreferences.givenSessionManagerShowIpAddress(false)
    }

    private fun givenVerificationService(): FakeVerificationService {
        return fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel when initializing it then verification listener is added`() {
        // Given
        val fakeVerificationService = givenVerificationService()
                .also { it.givenEventFlow() }
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)

        // When
        createViewModel()

        // Then
        verify {
            fakeVerificationService.requestEventFlow()
        }
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
        val deviceFullInfo = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
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
        viewModel.handle(OtherSessionsAction.EnableSelectMode(A_DEVICE_ID_1))

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given disable select mode action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = true)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = true)
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
        val deviceFullInfo = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
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
        viewModel.handle(OtherSessionsAction.ToggleSelectionForDevice(A_DEVICE_ID_1))

        // Then
        viewModelTest
                .assertLatestState { state -> state == expectedState }
                .finish()
    }

    @Test
    fun `given select all action when handling the action then viewState is updated with correct info`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
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
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
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

    @Test
    fun `given no reAuth is needed and in selectMode when handling multiSignout action then signout process is performed`() {
        // Given
        val isSelectModeEnabled = true
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        // signout only selected devices
        fakeSignoutSessionsUseCase.givenSignoutSuccess(listOf(A_DEVICE_ID_2))
        val expectedViewState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1, deviceFullInfo2)),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = isSelectModeEnabled,
        )

        // When
        val viewModel = createViewModel(OtherSessionsViewState(defaultArgs).copy(isSelectModeEnabled = isSelectModeEnabled))
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.MultiSignout)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is OtherSessionsViewEvents.SignoutSuccess }
                .finish()
        coVerify {
            fakeRefreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given no reAuth is needed and NOT in selectMode when handling multiSignout action then signout process is performed`() {
        // Given
        val isSelectModeEnabled = false
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        // signout all devices
        fakeSignoutSessionsUseCase.givenSignoutSuccess(listOf(A_DEVICE_ID_1, A_DEVICE_ID_2))
        val expectedViewState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1, deviceFullInfo2)),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
                isSelectModeEnabled = isSelectModeEnabled,
        )

        // When
        val viewModel = createViewModel(OtherSessionsViewState(defaultArgs).copy(isSelectModeEnabled = isSelectModeEnabled))
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.MultiSignout)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is OtherSessionsViewEvents.SignoutSuccess }
                .finish()
        coVerify {
            fakeRefreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given unexpected error during multiSignout when handling multiSignout action then signout process is performed`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val error = Exception()
        fakeSignoutSessionsUseCase.givenSignoutError(listOf(A_DEVICE_ID_1, A_DEVICE_ID_2), error)
        val expectedViewState = OtherSessionsViewState(
                devices = Success(listOf(deviceFullInfo1, deviceFullInfo2)),
                currentFilter = defaultArgs.defaultFilter,
                excludeCurrentDevice = defaultArgs.excludeCurrentDevice,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.MultiSignout)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is OtherSessionsViewEvents.SignoutError && it.error == error }
                .finish()
    }

    @Test
    fun `given reAuth is needed during multiSignout when handling multiSignout action then requestReAuth is sent and pending auth is stored`() {
        // Given
        val deviceFullInfo1 = aDeviceFullInfo(A_DEVICE_ID_1, isSelected = false)
        val deviceFullInfo2 = aDeviceFullInfo(A_DEVICE_ID_2, isSelected = true)
        val devices: List<DeviceFullInfo> = listOf(deviceFullInfo1, deviceFullInfo2)
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        val reAuthNeeded = fakeSignoutSessionsUseCase.givenSignoutReAuthNeeded(listOf(A_DEVICE_ID_1, A_DEVICE_ID_2))
        val expectedPendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        val expectedReAuthEvent = OtherSessionsViewEvents.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.MultiSignout)

        // Then
        viewModelTest
                .assertEvent { it == expectedReAuthEvent }
                .finish()
        fakePendingAuthHandler.instance.pendingAuth shouldBeEqualTo expectedPendingAuth
        fakePendingAuthHandler.instance.uiaContinuation shouldBeEqualTo reAuthNeeded.uiaContinuation
    }

    @Test
    fun `given SSO auth has been done when handling ssoAuthDone action then corresponding method of pending auth handler is called`() {
        // Given
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        justRun { fakePendingAuthHandler.instance.ssoAuthDone() }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.SsoAuthDone)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.ssoAuthDone()
        }
    }

    @Test
    fun `given password auth has been done when handling passwordAuthDone action then corresponding method of pending auth handler is called`() {
        // Given
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        justRun { fakePendingAuthHandler.instance.passwordAuthDone(any()) }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.PasswordAuthDone(A_PASSWORD))

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.passwordAuthDone(A_PASSWORD)
        }
    }

    @Test
    fun `given reAuth has been cancelled when handling reAuthCancelled action then corresponding method of pending auth handler is called`() {
        // Given
        val devices = mockk<List<DeviceFullInfo>>()
        givenGetDeviceFullInfoListReturns(filterType = defaultArgs.defaultFilter, devices)
        justRun { fakePendingAuthHandler.instance.reAuthCancelled() }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(OtherSessionsAction.ReAuthCancelled)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.reAuthCancelled()
        }
    }

    private fun givenGetDeviceFullInfoListReturns(
            filterType: DeviceManagerFilterType,
            devices: List<DeviceFullInfo>,
    ) {
        every { fakeGetDeviceFullInfoListUseCase.execute(filterType, any()) } returns flowOf(devices)
    }
}
