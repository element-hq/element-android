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
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

private const val A_SESSION_NAME = "session-name"
private const val A_SESSION_ID = "session-id"
private const val A_LAST_SEEN_TS = 123L

class CheckIfSectionSessionIsVisibleUseCaseTest {

    private val checkIfSectionSessionIsVisibleUseCase = CheckIfSectionSessionIsVisibleUseCase()

    @Test
    fun `given device info with name, id or lastSeenTs when checking is session section is visible then it returns true`() {
        // Given
        val deviceInfoList = listOf(
                givenADeviceInfo(
                        sessionName = A_SESSION_NAME,
                        sessionId = null,
                        lastSeenTs = null,
                ),
                givenADeviceInfo(
                        sessionName = null,
                        sessionId = A_SESSION_ID,
                        lastSeenTs = null,
                ),
                givenADeviceInfo(
                        sessionName = null,
                        sessionId = null,
                        lastSeenTs = A_LAST_SEEN_TS,
                ),
                givenADeviceInfo(
                        sessionName = A_SESSION_NAME,
                        sessionId = A_SESSION_ID,
                        lastSeenTs = null,
                ),
                givenADeviceInfo(
                        sessionName = A_SESSION_NAME,
                        sessionId = null,
                        lastSeenTs = A_LAST_SEEN_TS,
                ),
                givenADeviceInfo(
                        sessionName = null,
                        sessionId = A_SESSION_ID,
                        lastSeenTs = A_LAST_SEEN_TS,
                ),
                givenADeviceInfo(
                        sessionName = A_SESSION_NAME,
                        sessionId = A_SESSION_ID,
                        lastSeenTs = A_LAST_SEEN_TS,
                ),
        )

        deviceInfoList.forEach { deviceInfo ->
            // When
            val result = checkIfSectionSessionIsVisibleUseCase.execute(deviceInfo)

            // Then
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `given device info with missing session info when checking is session section is visible then it returns false`() {
        // Given
        val deviceInfoList = listOf(
                givenADeviceInfo(
                        sessionName = null,
                        sessionId = null,
                        lastSeenTs = null,
                ),
                givenADeviceInfo(
                        sessionName = "",
                        sessionId = "",
                        lastSeenTs = null,
                ),
                givenADeviceInfo(
                        sessionName = "",
                        sessionId = "",
                        lastSeenTs = -1,
                ),
        )

        deviceInfoList.forEach { deviceInfo ->
            // When
            val result = checkIfSectionSessionIsVisibleUseCase.execute(deviceInfo)

            // Then
            result shouldBeEqualTo false
        }
    }

    private fun givenADeviceInfo(
            sessionName: String?,
            sessionId: String?,
            lastSeenTs: Long?,
    ): DeviceInfo {
        val info = mockk<DeviceInfo>()
        every { info.displayName } returns sessionName
        every { info.deviceId } returns sessionId
        every { info.lastSeenTs } returns lastSeenTs
        return info
    }
}
