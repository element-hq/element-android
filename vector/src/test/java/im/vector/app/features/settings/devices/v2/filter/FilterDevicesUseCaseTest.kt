/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.filter

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import org.amshove.kluent.shouldBeEqualTo
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
        isInactive = false
)
private val inactiveVerifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "INACTIVE_VERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "INACTIVE_VERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
        isInactive = true
)
private val activeUnverifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "ACTIVE_UNVERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "ACTIVE_UNVERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
        isInactive = false
)
private val inactiveUnverifiedDevice = DeviceFullInfo(
        deviceInfo = DeviceInfo(deviceId = "INACTIVE_UNVERIFIED_DEVICE"),
        cryptoDeviceInfo = CryptoDeviceInfo(
                userId = "USER_ID_1",
                deviceId = "INACTIVE_UNVERIFIED_DEVICE",
                trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)
        ),
        roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Warning,
        isInactive = true
)

private val devices = listOf(
        activeVerifiedDevice,
        inactiveVerifiedDevice,
        activeUnverifiedDevice,
        inactiveUnverifiedDevice,
)

class FilterDevicesUseCaseTest {

    private val filterDevicesUseCase = FilterDevicesUseCase()

    @Test
    fun `given a device list when filter type is ALL_SESSIONS then returns the same list`() {
        val filteredDeviceList = filterDevicesUseCase.execute(devices, DeviceManagerFilterType.ALL_SESSIONS, emptyList())

        filteredDeviceList.size shouldBeEqualTo devices.size
    }

    @Test
    fun `given a device list when filter type is VERIFIED then returns only verified devices`() {
        val filteredDeviceList = filterDevicesUseCase.execute(devices, DeviceManagerFilterType.VERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 2
        filteredDeviceList shouldContainAll listOf(activeVerifiedDevice, inactiveVerifiedDevice)
    }

    @Test
    fun `given a device list when filter type is UNVERIFIED then returns only unverified devices`() {
        val filteredDeviceList = filterDevicesUseCase.execute(devices, DeviceManagerFilterType.UNVERIFIED, emptyList())

        filteredDeviceList.size shouldBeEqualTo 2
        filteredDeviceList shouldContainAll listOf(activeUnverifiedDevice, inactiveUnverifiedDevice)
    }

    @Test
    fun `given a device list when filter type is INACTIVE then returns only inactive devices`() {
        val filteredDeviceList = filterDevicesUseCase.execute(devices, DeviceManagerFilterType.INACTIVE, emptyList())

        filteredDeviceList.size shouldBeEqualTo 2
        filteredDeviceList shouldContainAll listOf(inactiveVerifiedDevice, inactiveUnverifiedDevice)
    }
}
