/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

private const val AN_IP_ADDRESS = "ip-address"

class CheckIfSectionDeviceIsVisibleUseCaseTest {

    private val checkIfSectionDeviceIsVisibleUseCase = CheckIfSectionDeviceIsVisibleUseCase()

    @Test
    fun `given device info with Ip address when checking is device section is visible then it returns true`() = runTest {
        // Given
        val deviceInfo = givenADeviceInfo(AN_IP_ADDRESS)

        // When
        val result = checkIfSectionDeviceIsVisibleUseCase.execute(deviceInfo)

        // Then
        result shouldBeEqualTo true
    }

    @Test
    fun `given device info with empty or null Ip address when checking is device section is visible then it returns false`() = runTest {
        // Given
        val deviceInfo1 = givenADeviceInfo("")
        val deviceInfo2 = givenADeviceInfo(null)

        // When
        val result1 = checkIfSectionDeviceIsVisibleUseCase.execute(deviceInfo1)
        val result2 = checkIfSectionDeviceIsVisibleUseCase.execute(deviceInfo2)

        // Then
        result1 shouldBeEqualTo false
        result2 shouldBeEqualTo false
    }

    private fun givenADeviceInfo(ipAddress: String?): DeviceInfo {
        val info = mockk<DeviceInfo>()
        every { info.lastSeenIp } returns ipAddress
        return info
    }
}
