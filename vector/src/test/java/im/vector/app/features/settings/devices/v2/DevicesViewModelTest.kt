/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.os.SystemClock
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.list.DeviceType
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakePendingAuthHandler
import im.vector.app.test.fakes.FakeSignoutSessionsUseCase
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth

private const val A_CURRENT_DEVICE_ID = "current-device-id"
private const val A_DEVICE_ID_1 = "device-id-1"
private const val A_DEVICE_ID_2 = "device-id-2"
private const val A_PASSWORD = "password"

class DevicesViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val getCurrentSessionCrossSigningInfoUseCase = mockk<GetCurrentSessionCrossSigningInfoUseCase>()
    private val getDeviceFullInfoListUseCase = mockk<GetDeviceFullInfoListUseCase>()
    private val refreshDevicesOnCryptoDevicesChangeUseCase = mockk<RefreshDevicesOnCryptoDevicesChangeUseCase>(relaxed = true)
    private val checkIfCurrentSessionCanBeVerifiedUseCase = mockk<CheckIfCurrentSessionCanBeVerifiedUseCase>()
    private val fakeSignoutSessionsUseCase = FakeSignoutSessionsUseCase()
    private val fakePendingAuthHandler = FakePendingAuthHandler()
    private val fakeRefreshDevicesUseCase = mockk<RefreshDevicesUseCase>(relaxUnitFun = true)
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val toggleIpAddressVisibilityUseCase = mockk<ToggleIpAddressVisibilityUseCase>()
    private val verifiedTransaction = mockk<VerificationTransaction>().apply {
        every { isSuccessful() } returns true
    }

    private fun createViewModel(): DevicesViewModel {
        return DevicesViewModel(
                initialState = DevicesViewState(),
                activeSessionHolder = fakeActiveSessionHolder.instance,
                getCurrentSessionCrossSigningInfoUseCase = getCurrentSessionCrossSigningInfoUseCase,
                getDeviceFullInfoListUseCase = getDeviceFullInfoListUseCase,
                refreshDevicesOnCryptoDevicesChangeUseCase = refreshDevicesOnCryptoDevicesChangeUseCase,
                checkIfCurrentSessionCanBeVerifiedUseCase = checkIfCurrentSessionCanBeVerifiedUseCase,
                signoutSessionsUseCase = fakeSignoutSessionsUseCase.instance,
                pendingAuthHandler = fakePendingAuthHandler.instance,
                refreshDevicesUseCase = fakeRefreshDevicesUseCase,
                vectorPreferences = fakeVectorPreferences.instance,
                toggleIpAddressVisibilityUseCase = toggleIpAddressVisibilityUseCase,
        )
    }

    @Before
    fun setup() {
        // Needed for internal usage of Flow<T>.throttleFirst() inside the ViewModel
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1234

        givenVerificationService()
        givenCurrentSessionCrossSigningInfo()
        givenDeviceFullInfoList(deviceId1 = A_DEVICE_ID_1, deviceId2 = A_DEVICE_ID_2)
        fakeActiveSessionHolder.fakeSession.fakeHomeServerCapabilitiesService.givenCapabilities(
                HomeServerCapabilities()
        )
        fakeVectorPreferences.givenSessionManagerShowIpAddress(false)
    }

    private fun givenVerificationService(): FakeVerificationService {
        return fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService.also {
                    it.givenEventFlow()
                }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel when initializing it then verification listener is added`() {
        // Given
        val fakeVerificationService = givenVerificationService()

        // When
        createViewModel()

        // Then
        verify {
            fakeVerificationService.requestEventFlow()
        }
    }

    @Test
    fun `given the viewModel when initializing it then view state is updated with current session cross signing info`() {
        // Given
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()

        // When
        val viewModelTest = createViewModel().test()

        // Then
        viewModelTest.assertLatestState { it.currentSessionCrossSigningInfo == currentSessionCrossSigningInfo }
        viewModelTest.finish()
    }

    @Test
    fun `given the viewModel when initializing it then view state is updated with current device full info list`() {
        // Given
        val deviceFullInfoList = givenDeviceFullInfoList(deviceId1 = A_DEVICE_ID_1, deviceId2 = A_DEVICE_ID_2)

        // When
        val viewModelTest = createViewModel().test()

        // Then
        viewModelTest.assertLatestState { it.devices is Success && it.devices.invoke() == deviceFullInfoList }
        viewModelTest.finish()
    }

    @Test
    fun `given the viewModel when initializing it then devices are refreshed on crypto devices change`() {
        // Given

        // When
        createViewModel()

        // Then
        coVerify { refreshDevicesOnCryptoDevicesChangeUseCase.execute() }
    }

    @Test
    fun `given current session can be verified when handling verify current session action then self verification event is posted`() {
        // Given
        val verifyCurrentSessionAction = DevicesAction.VerifyCurrentSession
        coEvery { checkIfCurrentSessionCanBeVerifiedUseCase.execute() } returns true

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(verifyCurrentSessionAction)

        // Then
        viewModelTest
                .assertEvent { it is DevicesViewEvent.SelfVerification }
                .finish()
        coVerify {
            checkIfCurrentSessionCanBeVerifiedUseCase.execute()
        }
    }

    @Test
    fun `given current session cannot be verified when handling verify current session action then reset secrets event is posted`() {
        // Given
        val verifyCurrentSessionAction = DevicesAction.VerifyCurrentSession
        coEvery { checkIfCurrentSessionCanBeVerifiedUseCase.execute() } returns false

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(verifyCurrentSessionAction)

        // Then
        viewModelTest
                .assertEvent { it is DevicesViewEvent.PromptResetSecrets }
                .finish()
        coVerify {
            checkIfCurrentSessionCanBeVerifiedUseCase.execute()
        }
    }

    @Test
    fun `given no reAuth is needed when handling multiSignout other sessions action then signout process is performed`() {
        // Given
        val expectedViewState = givenInitialViewState(deviceId1 = A_DEVICE_ID_1, deviceId2 = A_CURRENT_DEVICE_ID)
        // signout all devices except the current device
        fakeSignoutSessionsUseCase.givenSignoutSuccess(listOf(A_DEVICE_ID_1))

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.MultiSignoutOtherSessions)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is DevicesViewEvent.SignoutSuccess }
                .finish()
        coVerify {
            fakeRefreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given unexpected error during multiSignout when handling multiSignout action then signout process is performed`() {
        // Given
        val error = Exception()
        fakeSignoutSessionsUseCase.givenSignoutError(listOf(A_DEVICE_ID_1, A_DEVICE_ID_2), error)
        val expectedViewState = givenInitialViewState(deviceId1 = A_DEVICE_ID_1, deviceId2 = A_DEVICE_ID_2)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.MultiSignoutOtherSessions)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is DevicesViewEvent.SignoutError && it.error == error }
                .finish()
    }

    @Test
    fun `given reAuth is needed during multiSignout when handling multiSignout action then requestReAuth is sent and pending auth is stored`() {
        // Given
        val reAuthNeeded = fakeSignoutSessionsUseCase.givenSignoutReAuthNeeded(listOf(A_DEVICE_ID_1, A_DEVICE_ID_2))
        val expectedPendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        val expectedReAuthEvent = DevicesViewEvent.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.MultiSignoutOtherSessions)

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
        justRun { fakePendingAuthHandler.instance.ssoAuthDone() }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.SsoAuthDone)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.ssoAuthDone()
        }
    }

    @Test
    fun `given password auth has been done when handling passwordAuthDone action then corresponding method of pending auth handler is called`() {
        // Given
        justRun { fakePendingAuthHandler.instance.passwordAuthDone(any()) }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.PasswordAuthDone(A_PASSWORD))

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.passwordAuthDone(A_PASSWORD)
        }
    }

    @Test
    fun `given reAuth has been cancelled when handling reAuthCancelled action then corresponding method of pending auth handler is called`() {
        // Given
        justRun { fakePendingAuthHandler.instance.reAuthCancelled() }

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(DevicesAction.ReAuthCancelled)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.reAuthCancelled()
        }
    }

    @Test
    fun `given the viewModel when initializing it then view state of ip address visibility is false`() {
        // When
        val viewModelTest = createViewModel().test()

        // Then
        viewModelTest.assertLatestState { it.isShowingIpAddress == false }
        viewModelTest.finish()
    }

    @Test
    fun `given the viewModel when toggleIpAddressVisibility action is triggered then view state and preference change accordingly`() {
        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        every { toggleIpAddressVisibilityUseCase.execute() } just runs
        every { fakeVectorPreferences.instance.setIpAddressVisibilityInDeviceManagerScreens(true) } just runs
        every { fakeVectorPreferences.instance.showIpAddressInSessionManagerScreens() } returns true

        viewModel.handle(DevicesAction.ToggleIpAddressVisibility)
        viewModel.onSharedPreferenceChanged(null, null)

        // Then
        viewModelTest.assertLatestState { it.isShowingIpAddress == true }
        viewModelTest.finish()
    }

    @Test
    fun `given the view model when a verified transaction is updated then device list is refreshed`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.transactionUpdated(verifiedTransaction)

        // Then
        verify { viewModel.refreshDeviceList() }
    }

    private fun givenCurrentSessionCrossSigningInfo(): CurrentSessionCrossSigningInfo {
        val currentSessionCrossSigningInfo = mockk<CurrentSessionCrossSigningInfo>()
        every { currentSessionCrossSigningInfo.deviceId } returns A_CURRENT_DEVICE_ID
        every { getCurrentSessionCrossSigningInfoUseCase.execute() } returns flowOf(currentSessionCrossSigningInfo)
        return currentSessionCrossSigningInfo
    }

    /**
     * Generate mocked deviceFullInfo list with 1 unverified and inactive + 1 verified and active.
     */
    private fun givenDeviceFullInfoList(deviceId1: String, deviceId2: String): DeviceFullInfoList {
        val verifiedCryptoDeviceInfo = mockk<CryptoDeviceInfo>()
        every { verifiedCryptoDeviceInfo.trustLevel } returns DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
        val unverifiedCryptoDeviceInfo = mockk<CryptoDeviceInfo>()
        every { unverifiedCryptoDeviceInfo.trustLevel } returns DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)

        val deviceInfo1 = mockk<DeviceInfo>()
        every { deviceInfo1.deviceId } returns deviceId1
        val deviceInfo2 = mockk<DeviceInfo>()
        every { deviceInfo2.deviceId } returns deviceId2

        val deviceFullInfo1 = DeviceFullInfo(
                deviceInfo = deviceInfo1,
                cryptoDeviceInfo = verifiedCryptoDeviceInfo,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                isInactive = false,
                isCurrentDevice = true,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = MatrixClientInfoContent(),
        )
        val deviceFullInfo2 = DeviceFullInfo(
                deviceInfo = deviceInfo2,
                cryptoDeviceInfo = unverifiedCryptoDeviceInfo,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
                isInactive = true,
                isCurrentDevice = false,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = MatrixClientInfoContent(),
        )
        val devices = listOf(deviceFullInfo1, deviceFullInfo2)
        every { getDeviceFullInfoListUseCase.execute(DeviceManagerFilterType.ALL_SESSIONS, any()) } returns flowOf(devices)
        every { getDeviceFullInfoListUseCase.execute(DeviceManagerFilterType.UNVERIFIED, any()) } returns flowOf(listOf(deviceFullInfo2))
        every { getDeviceFullInfoListUseCase.execute(DeviceManagerFilterType.INACTIVE, any()) } returns flowOf(listOf(deviceFullInfo1))
        return DeviceFullInfoList(
                allSessions = devices,
                unverifiedSessionsCount = 1,
                inactiveSessionsCount = 1,
        )
    }

    private fun givenInitialViewState(deviceId1: String, deviceId2: String): DevicesViewState {
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        val deviceFullInfoList = givenDeviceFullInfoList(deviceId1, deviceId2)
        return DevicesViewState(
                currentSessionCrossSigningInfo = currentSessionCrossSigningInfo,
                devices = Success(deviceFullInfoList),
                isLoading = false,
        )
    }
}
