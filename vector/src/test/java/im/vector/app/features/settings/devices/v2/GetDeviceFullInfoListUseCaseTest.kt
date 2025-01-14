/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.session.clientinfo.GetMatrixClientInfoUseCase
import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.filter.FilterDevicesUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.features.settings.devices.v2.list.DeviceType
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.verification.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

private const val A_DEVICE_ID_1 = "device-id-1"
private const val A_DEVICE_ID_2 = "device-id-2"
private const val A_DEVICE_ID_3 = "device-id-3"
private const val A_TIMESTAMP_1 = 100L
private const val A_TIMESTAMP_2 = 200L
private const val A_TIMESTAMP_3 = 300L

class GetDeviceFullInfoListUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val checkIfSessionIsInactiveUseCase = mockk<CheckIfSessionIsInactiveUseCase>()
    private val getEncryptionTrustLevelForDeviceUseCase = mockk<GetEncryptionTrustLevelForDeviceUseCase>()
    private val getCurrentSessionCrossSigningInfoUseCase = mockk<GetCurrentSessionCrossSigningInfoUseCase>()
    private val filterDevicesUseCase = mockk<FilterDevicesUseCase>()
    private val parseDeviceUserAgentUseCase = mockk<ParseDeviceUserAgentUseCase>()
    private val getMatrixClientInfoUseCase = mockk<GetMatrixClientInfoUseCase>()

    private val getDeviceFullInfoListUseCase = GetDeviceFullInfoListUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            checkIfSessionIsInactiveUseCase = checkIfSessionIsInactiveUseCase,
            getEncryptionTrustLevelForDeviceUseCase = getEncryptionTrustLevelForDeviceUseCase,
            getCurrentSessionCrossSigningInfoUseCase = getCurrentSessionCrossSigningInfoUseCase,
            filterDevicesUseCase = filterDevicesUseCase,
            parseDeviceUserAgentUseCase = parseDeviceUserAgentUseCase,
            getMatrixClientInfoUseCase = getMatrixClientInfoUseCase,
    )

    @Before
    fun setUp() {
        mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given active session when getting list of device full info then the list is correct and sorted in descending order`() = runTest(testDispatcher) {
        // Given
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        val fakeFlowSession = fakeActiveSessionHolder.fakeSession.givenFlowSession()
        val cryptoDeviceInfo1 = givenACryptoDeviceInfo(A_DEVICE_ID_1)
        val cryptoDeviceInfo2 = givenACryptoDeviceInfo(A_DEVICE_ID_2)
        val cryptoDeviceInfo3 = givenACryptoDeviceInfo(A_DEVICE_ID_3)
        val cryptoDeviceInfoList = listOf(cryptoDeviceInfo1, cryptoDeviceInfo2, cryptoDeviceInfo3)
        every { fakeFlowSession.liveUserCryptoDevices(any()) } returns flowOf(cryptoDeviceInfoList)
        val deviceInfo1 = givenADevicesInfo(
                deviceId = A_DEVICE_ID_1,
                lastSeenTs = A_TIMESTAMP_1,
                isInactive = true,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                cryptoDeviceInfo = cryptoDeviceInfo1,
        )
        val deviceInfo2 = givenADevicesInfo(
                deviceId = A_DEVICE_ID_2,
                lastSeenTs = A_TIMESTAMP_2,
                isInactive = false,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                cryptoDeviceInfo = cryptoDeviceInfo2,
        )
        val deviceInfo3 = givenADevicesInfo(
                deviceId = A_DEVICE_ID_3,
                lastSeenTs = A_TIMESTAMP_3,
                isInactive = false,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
                cryptoDeviceInfo = cryptoDeviceInfo3,
        )
        val deviceInfoList = listOf(deviceInfo1, deviceInfo2, deviceInfo3)
        every { fakeFlowSession.liveMyDevicesInfo() } returns flowOf(deviceInfoList)
        val matrixClientInfo1 = givenAMatrixClientInfo(A_DEVICE_ID_1)
        val matrixClientInfo2 = givenAMatrixClientInfo(A_DEVICE_ID_2)
        val matrixClientInfo3 = givenAMatrixClientInfo(A_DEVICE_ID_3)
        val expectedResult1 = DeviceFullInfo(
                deviceInfo = deviceInfo1,
                cryptoDeviceInfo = cryptoDeviceInfo1,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                isInactive = true,
                isCurrentDevice = true,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = matrixClientInfo1,
        )
        val expectedResult2 = DeviceFullInfo(
                deviceInfo = deviceInfo2,
                cryptoDeviceInfo = cryptoDeviceInfo2,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
                isInactive = false,
                isCurrentDevice = false,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = matrixClientInfo2,
        )
        val expectedResult3 = DeviceFullInfo(
                deviceInfo = deviceInfo3,
                cryptoDeviceInfo = cryptoDeviceInfo3,
                roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
                isInactive = false,
                isCurrentDevice = false,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = matrixClientInfo3,
        )
        val expectedResult = listOf(expectedResult3, expectedResult2, expectedResult1)
        every { filterDevicesUseCase.execute(any(), any(), any()) } returns expectedResult
        val filterType = DeviceManagerFilterType.ALL_SESSIONS

        // When
        val result = getDeviceFullInfoListUseCase.execute(filterType, excludeCurrentDevice = false)
                .test(this)

        // Then
        result.assertValues(expectedResult)
                .finish()
        verify {
            getCurrentSessionCrossSigningInfoUseCase.execute()
            fakeFlowSession.liveUserCryptoDevices(fakeActiveSessionHolder.fakeSession.myUserId)
            fakeFlowSession.liveMyDevicesInfo()
            getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo1)
            getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo2)
            getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo3)
            checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP_1)
            checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP_2)
            checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP_3)
            getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID_1)
            getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID_2)
            getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID_3)
            filterDevicesUseCase.execute(currentSessionCrossSigningInfo, expectedResult, filterType, emptyList())
        }
    }

    @Test
    fun `given no active session when getting list then the result is empty`() = runTest(testDispatcher) {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val result = getDeviceFullInfoListUseCase.execute(DeviceManagerFilterType.ALL_SESSIONS, excludeCurrentDevice = false)
                .test(this)

        // Then
        result.assertNoValues()
                .finish()
    }

    private fun givenCurrentSessionCrossSigningInfo(): CurrentSessionCrossSigningInfo {
        val currentSessionCrossSigningInfo = mockk<CurrentSessionCrossSigningInfo>()
        every { getCurrentSessionCrossSigningInfoUseCase.execute() } returns flowOf(currentSessionCrossSigningInfo)
        every { currentSessionCrossSigningInfo.deviceId } returns A_DEVICE_ID_1
        return currentSessionCrossSigningInfo
    }

    private fun givenACryptoDeviceInfo(deviceId: String): CryptoDeviceInfo {
        val cryptoDeviceInfo = mockk<CryptoDeviceInfo>()
        every { cryptoDeviceInfo.deviceId } returns deviceId
        return cryptoDeviceInfo
    }

    private fun givenADevicesInfo(
            deviceId: String,
            lastSeenTs: Long,
            isInactive: Boolean,
            roomEncryptionTrustLevel: RoomEncryptionTrustLevel,
            cryptoDeviceInfo: CryptoDeviceInfo,
    ): DeviceInfo {
        val deviceInfo = mockk<DeviceInfo>()
        every { deviceInfo.deviceId } returns deviceId
        every { deviceInfo.lastSeenTs } returns lastSeenTs
        every { deviceInfo.getBestLastSeenUserAgent() } returns ""
        every { getEncryptionTrustLevelForDeviceUseCase.execute(any(), cryptoDeviceInfo) } returns roomEncryptionTrustLevel
        every { checkIfSessionIsInactiveUseCase.execute(lastSeenTs) } returns isInactive
        every { parseDeviceUserAgentUseCase.execute(any()) } returns DeviceExtendedInfo(
                DeviceType.MOBILE,
        )

        return deviceInfo
    }

    private fun givenAMatrixClientInfo(deviceId: String): MatrixClientInfoContent {
        val matrixClientInfo = mockk<MatrixClientInfoContent>()
        every { getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, deviceId) } returns matrixClientInfo
        return matrixClientInfo
    }
}
