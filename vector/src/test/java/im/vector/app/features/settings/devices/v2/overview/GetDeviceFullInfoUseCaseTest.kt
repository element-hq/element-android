/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import im.vector.app.core.session.clientinfo.GetMatrixClientInfoUseCase
import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.ParseDeviceUserAgentUseCase
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.features.settings.devices.v2.list.DeviceType
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.verification.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.Optional

private const val A_DEVICE_ID = "device-id"
private const val A_TIMESTAMP = 123L

class GetDeviceFullInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val getCurrentSessionCrossSigningInfoUseCase = mockk<GetCurrentSessionCrossSigningInfoUseCase>()
    private val getEncryptionTrustLevelForDeviceUseCase = mockk<GetEncryptionTrustLevelForDeviceUseCase>()
    private val checkIfSessionIsInactiveUseCase = mockk<CheckIfSessionIsInactiveUseCase>()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()
    private val parseDeviceUserAgentUseCase = mockk<ParseDeviceUserAgentUseCase>()
    private val getMatrixClientInfoUseCase = mockk<GetMatrixClientInfoUseCase>()

    private val getDeviceFullInfoUseCase = GetDeviceFullInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            getCurrentSessionCrossSigningInfoUseCase = getCurrentSessionCrossSigningInfoUseCase,
            getEncryptionTrustLevelForDeviceUseCase = getEncryptionTrustLevelForDeviceUseCase,
            checkIfSessionIsInactiveUseCase = checkIfSessionIsInactiveUseCase,
            parseDeviceUserAgentUseCase = parseDeviceUserAgentUseCase,
            getMatrixClientInfoUseCase = getMatrixClientInfoUseCase,
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given current session and info for device when getting device info then the result is correct`() = runTest {
        // Given
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        val deviceInfo = givenADeviceInfo()
        val cryptoDeviceInfo = givenACryptoDeviceInfo()
        val trustLevel = givenTrustLevel(currentSessionCrossSigningInfo, cryptoDeviceInfo)
        val isInactive = false
        val isCurrentDevice = true
        every { checkIfSessionIsInactiveUseCase.execute(any()) } returns isInactive
        every { parseDeviceUserAgentUseCase.execute(any()) } returns DeviceExtendedInfo(DeviceType.MOBILE)
        val matrixClientInfo = givenAMatrixClientInfo()

        // When
        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        // Then
        deviceFullInfo shouldBeEqualTo DeviceFullInfo(
                deviceInfo = deviceInfo,
                cryptoDeviceInfo = cryptoDeviceInfo,
                roomEncryptionTrustLevel = trustLevel,
                isInactive = isInactive,
                isCurrentDevice = isCurrentDevice,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = matrixClientInfo,
        )
        verify {
            fakeActiveSessionHolder.instance.getSafeActiveSession()
            getCurrentSessionCrossSigningInfoUseCase.execute()
            getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow()
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow()
            checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP)
            getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID)
        }
    }

    @Test
    fun `given current session and no crypto info for device when getting device info then the result is correct`() = runTest {
        // Given
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        val deviceInfo = givenADeviceInfo()
        val cryptoDeviceInfo = null
        val trustLevel = givenTrustLevel(currentSessionCrossSigningInfo, cryptoDeviceInfo)
        val isInactive = false
        val isCurrentDevice = true
        every { checkIfSessionIsInactiveUseCase.execute(any()) } returns isInactive
        every { parseDeviceUserAgentUseCase.execute(any()) } returns DeviceExtendedInfo(DeviceType.MOBILE)
        val matrixClientInfo = givenAMatrixClientInfo()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()

        // When
        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        // Then
        deviceFullInfo shouldBeEqualTo DeviceFullInfo(
                deviceInfo = deviceInfo,
                cryptoDeviceInfo = cryptoDeviceInfo,
                roomEncryptionTrustLevel = trustLevel,
                isInactive = isInactive,
                isCurrentDevice = isCurrentDevice,
                deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
                matrixClientInfo = matrixClientInfo,
        )
        verify {
            fakeActiveSessionHolder.instance.getSafeActiveSession()
            getCurrentSessionCrossSigningInfoUseCase.execute()
            getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow()
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow()
            checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP)
            getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID)
        }
    }

    @Test
    fun `given current session and no info for device when getting device info then the result is empty`() = runTest {
        // Given
        givenCurrentSessionCrossSigningInfo()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()

        // When
        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        // Then
        deviceFullInfo.shouldBeNull()
        verify {
            fakeActiveSessionHolder.instance.getSafeActiveSession()
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow()
            fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow()
        }
    }

    @Test
    fun `given no current session when getting device info then the result is empty`() = runTest {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        // Then
        deviceFullInfo.shouldBeNull()
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
    }

    private fun givenCurrentSessionCrossSigningInfo(): CurrentSessionCrossSigningInfo {
        val currentSessionCrossSigningInfo = CurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = false
        )
        every { getCurrentSessionCrossSigningInfoUseCase.execute() } returns flowOf(currentSessionCrossSigningInfo)
        return currentSessionCrossSigningInfo
    }

    private fun givenTrustLevel(currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo, cryptoDeviceInfo: CryptoDeviceInfo?): RoomEncryptionTrustLevel {
        val trustLevel = RoomEncryptionTrustLevel.Trusted
        every { getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo) } returns trustLevel
        return trustLevel
    }

    private fun givenADeviceInfo(): DeviceInfo {
        val deviceInfo = DeviceInfo(
                deviceId = A_DEVICE_ID,
                lastSeenTs = A_TIMESTAMP,
        )
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(deviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        return deviceInfo
    }

    private fun givenACryptoDeviceInfo(): CryptoDeviceInfo {
        val cryptoDeviceInfo = CryptoDeviceInfo(deviceId = A_DEVICE_ID, userId = "")
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(cryptoDeviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()
        return cryptoDeviceInfo
    }

    private fun givenAMatrixClientInfo(): MatrixClientInfoContent {
        val matrixClientInfo = mockk<MatrixClientInfoContent>()
        every { getMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession, A_DEVICE_ID) } returns matrixClientInfo
        return matrixClientInfo
    }
}
