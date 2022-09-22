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
