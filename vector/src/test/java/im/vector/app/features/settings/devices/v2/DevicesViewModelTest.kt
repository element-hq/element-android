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

package im.vector.app.features.settings.devices.v2

import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class DevicesViewModelTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule(testDispatcher = testDispatcher)

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val getCurrentSessionCrossSigningInfoUseCase = mockk<GetCurrentSessionCrossSigningInfoUseCase>()
    private val getDeviceFullInfoListUseCase = mockk<GetDeviceFullInfoListUseCase>()
    private val refreshDevicesUseCase = mockk<RefreshDevicesUseCase>()
    private val refreshDevicesOnCryptoDevicesChangeUseCase = mockk<RefreshDevicesOnCryptoDevicesChangeUseCase>()

    private fun createViewModel(): DevicesViewModel {
        return DevicesViewModel(
                DevicesViewState(),
                fakeActiveSessionHolder.instance,
                getCurrentSessionCrossSigningInfoUseCase,
                getDeviceFullInfoListUseCase,
                refreshDevicesOnCryptoDevicesChangeUseCase,
                refreshDevicesUseCase,
        )
    }

    @Test
    fun `given the viewModel when initializing it then verification listener is added`() {
        // Given
        val fakeVerificationService = givenVerificationService()
        givenCurrentSessionCrossSigningInfo()
        givenDeviceFullInfoList()
        givenRefreshDevicesOnCryptoDevicesChange()

        // When
        val viewModel = createViewModel()

        // Then
        verify {
            fakeVerificationService.addListener(viewModel)
        }
    }

    @Test
    fun `given the viewModel when clearing it then verification listener is removed`() {
        // Given
        val fakeVerificationService = givenVerificationService()
        givenCurrentSessionCrossSigningInfo()
        givenDeviceFullInfoList()
        givenRefreshDevicesOnCryptoDevicesChange()

        // When
        val viewModel = createViewModel()
        viewModel.onCleared()

        // Then
        verify {
            fakeVerificationService.removeListener(viewModel)
        }
    }

    @Test
    fun `given the viewModel when initializing it then view state is updated with current session cross signing info`() {
        // Given
        givenVerificationService()
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        givenDeviceFullInfoList()
        givenRefreshDevicesOnCryptoDevicesChange()

        // When
        val viewModelTest = createViewModel().test()

        // Then
        viewModelTest.assertLatestState { it.currentSessionCrossSigningInfo == currentSessionCrossSigningInfo }
        viewModelTest.finish()
    }

    @Test
    fun `given the viewModel when initializing it then view state is updated with current device full info list`() {
        // Given
        givenVerificationService()
        givenCurrentSessionCrossSigningInfo()
        val deviceFullInfoList = givenDeviceFullInfoList()
        givenRefreshDevicesOnCryptoDevicesChange()

        // When
        val viewModelTest = createViewModel().test()

        // Then
        viewModelTest.assertLatestState {
            it.devices is Success && it.devices.invoke() == deviceFullInfoList &&
                    it.inactiveSessionsCount == 1 && it.unverifiedSessionsCount == 1
        }
        viewModelTest.finish()
    }

    @Test
    fun `given the viewModel when initializing it then devices are refreshed on crypto devices change`() {
        // Given
        givenVerificationService()
        givenCurrentSessionCrossSigningInfo()
        givenDeviceFullInfoList()
        givenRefreshDevicesOnCryptoDevicesChange()

        // When
        createViewModel()

        // Then
        coVerify { refreshDevicesOnCryptoDevicesChangeUseCase.execute() }
    }

    private fun givenVerificationService(): FakeVerificationService {
        val fakeVerificationService = fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
        every { fakeVerificationService.addListener(any()) } just runs
        every { fakeVerificationService.removeListener(any()) } just runs
        return fakeVerificationService
    }

    private fun givenCurrentSessionCrossSigningInfo(): CurrentSessionCrossSigningInfo {
        val currentSessionCrossSigningInfo = mockk<CurrentSessionCrossSigningInfo>()
        every { getCurrentSessionCrossSigningInfoUseCase.execute() } returns flowOf(currentSessionCrossSigningInfo)
        return currentSessionCrossSigningInfo
    }

    /**
     * Generate mocked deviceFullInfo list with 1 unverified and inactive + 1 verified and active.
     */
    private fun givenDeviceFullInfoList(): List<DeviceFullInfo> {
        val verifiedCryptoDeviceInfo = mockk<CryptoDeviceInfo>()
        every { verifiedCryptoDeviceInfo.isVerified } returns true
        val unverifiedCryptoDeviceInfo = mockk<CryptoDeviceInfo>()
        every { unverifiedCryptoDeviceInfo.isVerified } returns false

        val deviceFullInfo1 = DeviceFullInfo(
                deviceInfo = mockk(),
                cryptoDeviceInfo = verifiedCryptoDeviceInfo,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                isInactive = false
        )
        val deviceFullInfo2 = DeviceFullInfo(
                deviceInfo = mockk(),
                cryptoDeviceInfo = unverifiedCryptoDeviceInfo,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
                isInactive = true
        )
        val deviceFullInfoList = listOf(deviceFullInfo1, deviceFullInfo2)
        val deviceFullInfoListFlow = flowOf(deviceFullInfoList)
        every { getDeviceFullInfoListUseCase.execute(any(), any()) } returns deviceFullInfoListFlow
        return deviceFullInfoList
    }

    private fun givenRefreshDevicesOnCryptoDevicesChange() {
        coEvery { refreshDevicesOnCryptoDevicesChangeUseCase.execute() } just runs
    }
}
