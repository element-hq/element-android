/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.list.DeviceType
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

private const val AN_IP_ADDRESS = "ip-address"
private const val A_DEVICE_MODEL = "device-model"
private const val A_DEVICE_OPERATING_SYSTEM = "device-operating-system"
private const val A_CLIENT_NAME = "client-name"

class CheckIfSectionDeviceIsVisibleUseCaseTest {

    private val checkIfSectionDeviceIsVisibleUseCase = CheckIfSectionDeviceIsVisibleUseCase()

    @Test
    fun `given device of any type with Ip address when checking if device section is visible then it returns true`() {
        DeviceType.values().forEach { deviceType ->
            // Given
            val deviceExtendedInfo = givenAnExtendedDeviceInfo(deviceType)
            val deviceFullInfo = givenADeviceFullInfo(deviceExtendedInfo)
            val deviceInfo = givenADeviceInfo(ipAddress = AN_IP_ADDRESS)
            every { deviceFullInfo.deviceInfo } returns deviceInfo

            // When
            val result = checkIfSectionDeviceIsVisibleUseCase.execute(deviceFullInfo)

            // Then
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `given device of any type with empty or null Ip address and no extended info when checking if device section is visible then it returns false`() {
        DeviceType.values().forEach { deviceType ->
            // Given
            val deviceExtendedInfo = givenAnExtendedDeviceInfo(deviceType)
            val deviceFullInfo1 = givenADeviceFullInfo(deviceExtendedInfo)
            val deviceFullInfo2 = givenADeviceFullInfo(deviceExtendedInfo)
            val deviceInfo1 = givenADeviceInfo(ipAddress = "")
            val deviceInfo2 = givenADeviceInfo(ipAddress = null)
            every { deviceFullInfo1.deviceInfo } returns deviceInfo1
            every { deviceFullInfo2.deviceInfo } returns deviceInfo2

            // When
            val result1 = checkIfSectionDeviceIsVisibleUseCase.execute(deviceFullInfo1)
            val result2 = checkIfSectionDeviceIsVisibleUseCase.execute(deviceFullInfo2)

            // Then
            result1 shouldBeEqualTo false
            result2 shouldBeEqualTo false
        }
    }

    @Test
    fun `given device of any type with extended info when checking if device section is visible then it returns true`() {
        // Given
        val deviceExtendedInfoList = listOf(
                givenAnExtendedDeviceInfo(
                        DeviceType.MOBILE,
                        deviceModel = A_DEVICE_MODEL,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.MOBILE,
                        deviceOperatingSystem = A_DEVICE_OPERATING_SYSTEM,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.MOBILE,
                        deviceModel = A_DEVICE_MODEL,
                        deviceOperatingSystem = A_DEVICE_OPERATING_SYSTEM,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.DESKTOP,
                        deviceOperatingSystem = A_DEVICE_OPERATING_SYSTEM,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.WEB,
                        clientName = A_CLIENT_NAME,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.WEB,
                        deviceOperatingSystem = A_DEVICE_OPERATING_SYSTEM,
                ),
                givenAnExtendedDeviceInfo(
                        DeviceType.WEB,
                        clientName = A_CLIENT_NAME,
                        deviceOperatingSystem = A_DEVICE_OPERATING_SYSTEM,
                ),
        )

        deviceExtendedInfoList.forEach { deviceExtendedInfo ->
            val deviceFullInfo = givenADeviceFullInfo(deviceExtendedInfo)
            val deviceInfo = givenADeviceInfo(ipAddress = null)
            every { deviceFullInfo.deviceInfo } returns deviceInfo

            // When
            val result = checkIfSectionDeviceIsVisibleUseCase.execute(deviceFullInfo)

            // Then
            result shouldBeEqualTo true
        }
    }

    private fun givenADeviceFullInfo(deviceExtendedInfo: DeviceExtendedInfo): DeviceFullInfo {
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.deviceExtendedInfo } returns deviceExtendedInfo
        return deviceFullInfo
    }

    private fun givenADeviceInfo(ipAddress: String?): DeviceInfo {
        val info = mockk<DeviceInfo>()
        every { info.lastSeenIp } returns ipAddress
        return info
    }

    private fun givenAnExtendedDeviceInfo(
            deviceType: DeviceType,
            clientName: String? = null,
            clientVersion: String? = null,
            deviceOperatingSystem: String? = null,
            deviceModel: String? = null,
    ): DeviceExtendedInfo {
        return DeviceExtendedInfo(
                deviceType = deviceType,
                clientName = clientName,
                clientVersion = clientVersion,
                deviceOperatingSystem = deviceOperatingSystem,
                deviceModel = deviceModel,
        )
    }
}
