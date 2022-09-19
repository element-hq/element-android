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
