/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.filter

import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.list.DeviceType
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

private val activeVerifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "ACTIVE_VERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "ACTIVE_VERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
        isInactive = false,
        isCurrentDevice = true,
        deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
        matrixClientInfo = MatrixClientInfoContent(),
)
private val inactiveVerifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "INACTIVE_VERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "INACTIVE_VERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
        isInactive = true,
        isCurrentDevice = false,
        deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
        matrixClientInfo = MatrixClientInfoContent(),
)
private val activeUnverifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "ACTIVE_UNVERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "ACTIVE_UNVERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
        isInactive = false,
        isCurrentDevice = false,
        deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
        matrixClientInfo = MatrixClientInfoContent(),
)
private val inactiveUnverifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "INACTIVE_UNVERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "INACTIVE_UNVERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
        isInactive = true,
        isCurrentDevice = false,
        deviceExtendedInfo = DeviceExtendedInfo(DeviceType.MOBILE),
        matrixClientInfo = MatrixClientInfoContent(),
)

private val deviceWithoutEncryptionSupport = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "DEVICE_WITHOUT_ENCRYPTION_SUPPORT"),
        cryptoDeviceInfo = null,
        roomEncryptionTrustLevel = null,
        isInactive = false,
        isCurrentDevice = false,
        deviceExtendedInfo = DeviceExtendedInfo(DeviceType.UNKNOWN),
        matrixClientInfo = MatrixClientInfoContent(),
)

private val devices = listOf(
        activeVerifiedDevice,
        inactiveVerifiedDevice,
        activeUnverifiedDevice,
        inactiveUnverifiedDevice,
        deviceWithoutEncryptionSupport,
)

class FilterDevicesUseCaseTest {

    private val filterDevicesUseCase = FilterDevicesUseCase()

    @Test
    fun `given a device list when filter type is ALL_SESSIONS then returns the same list`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(true)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.ALL_SESSIONS, emptyList())

        filteredDeviceList.size shouldBeEqualTo devices.size
    }

    @Test
    fun `given a device list and current session is verified when filter type is VERIFIED then returns only verified devices`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(true)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.VERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 2
        filteredDeviceList shouldContainAll listOf(activeVerifiedDevice, inactiveVerifiedDevice)
    }

    @Test
    fun `given a device list and current session is unverified when filter type is VERIFIED then returns empty list of devices`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(false)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.VERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 0
    }

    @Test
    fun `given a device list and current session is verified when filter type is UNVERIFIED then returns only unverified devices`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(true)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.UNVERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 3
        filteredDeviceList shouldContainAll listOf(activeUnverifiedDevice, inactiveUnverifiedDevice, deviceWithoutEncryptionSupport)
    }

    @Test
    fun `given a device list and current session is unverified when filter type is UNVERIFIED then returns empty list of devices`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(false)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.UNVERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 1
        filteredDeviceList shouldContain deviceWithoutEncryptionSupport
    }

    @Test
    fun `given a device list when filter type is INACTIVE then returns only inactive devices`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionVerified(true)
        val filteredDeviceList = filterDevicesUseCase.execute(currentSessionCrossSigningInfo, devices, DeviceManagerFilterType.INACTIVE, emptyList())

        filteredDeviceList.size shouldBeEqualTo 2
        filteredDeviceList shouldContainAll listOf(inactiveVerifiedDevice, inactiveUnverifiedDevice)
    }

    private fun givenCurrentSessionVerified(isVerified: Boolean): CurrentSessionCrossSigningInfo = CurrentSessionCrossSigningInfo(
            isCrossSigningVerified = isVerified,
            isCrossSigningInitialized = true,
            deviceId = ""
    )
}
