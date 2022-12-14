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

package im.vector.app.core.session.clientinfo

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

private const val A_CURRENT_DEVICE_ID = "current-device-id"
private const val A_DEVICE_ID_1 = "a-device-id-1"
private const val A_DEVICE_ID_2 = "a-device-id-2"
private const val A_DEVICE_ID_3 = "a-device-id-3"
private const val A_DEVICE_ID_4 = "a-device-id-4"

private val A_DEVICE_INFO_1 = DeviceInfo(deviceId = A_DEVICE_ID_1)
private val A_DEVICE_INFO_2 = DeviceInfo(deviceId = A_DEVICE_ID_2)

class DeleteUnusedClientInformationUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val deleteUnusedClientInformationUseCase = DeleteUnusedClientInformationUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Before
    fun setup() {
        fakeActiveSessionHolder.fakeSession.givenSessionId(A_CURRENT_DEVICE_ID)
    }

    @Test
    fun `given a device list that account data has all of them and extra devices then use case deletes the unused ones`() = runTest {
        // Given
        val devices = listOf(A_DEVICE_INFO_1, A_DEVICE_INFO_2)
        val userAccountDataEventList = listOf(
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_1, mapOf("key" to "value")),
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_2, mapOf("key" to "value")),
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_3, mapOf("key" to "value")),
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_4, mapOf("key" to "value")),
        )
        fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.givenGetUserAccountDataEventsStartWith(
                type = MATRIX_CLIENT_INFO_KEY_PREFIX,
                userAccountDataEventList = userAccountDataEventList,
        )

        // When
        deleteUnusedClientInformationUseCase.execute(devices)

        // Then
        coVerify { fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.deleteUserAccountData(MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_3) }
        coVerify { fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.deleteUserAccountData(MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_4) }
    }

    @Test
    fun `given a device list that account data has exactly all of them then use case does nothing`() = runTest {
        // Given
        val devices = listOf(A_DEVICE_INFO_1, A_DEVICE_INFO_2)
        val userAccountDataEventList = listOf(
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_1, mapOf("key" to "value")),
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_2, mapOf("key" to "value")),
        )
        fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.givenGetUserAccountDataEventsStartWith(
                type = MATRIX_CLIENT_INFO_KEY_PREFIX,
                userAccountDataEventList = userAccountDataEventList,
        )

        // When
        deleteUnusedClientInformationUseCase.execute(devices)

        // Then
        coVerify(exactly = 0) {
            fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.deleteUserAccountData(any())
        }
    }

    @Test
    fun `given a device list that account data has missing some of them then use case does nothing`() = runTest {
        // Given
        val devices = listOf(A_DEVICE_INFO_1, A_DEVICE_INFO_2)
        val userAccountDataEventList = listOf(
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_1, mapOf("key" to "value")),
        )
        fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.givenGetUserAccountDataEventsStartWith(
                type = MATRIX_CLIENT_INFO_KEY_PREFIX,
                userAccountDataEventList = userAccountDataEventList,
        )

        // When
        deleteUnusedClientInformationUseCase.execute(devices)

        // Then
        coVerify(exactly = 0) {
            fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.deleteUserAccountData(any())
        }
    }

    @Test
    fun `given an empty device list that account data has some devices then use case does nothing`() = runTest {
        // Given
        val devices = emptyList<DeviceInfo>()
        val userAccountDataEventList = listOf(
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_1, mapOf("key" to "value")),
                UserAccountDataEvent(type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID_2, mapOf("key" to "value")),
        )
        fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.givenGetUserAccountDataEventsStartWith(
                type = MATRIX_CLIENT_INFO_KEY_PREFIX,
                userAccountDataEventList = userAccountDataEventList,
        )

        // When
        deleteUnusedClientInformationUseCase.execute(devices)

        // Then
        coVerify(exactly = 0) {
            fakeActiveSessionHolder.fakeSession.fakeSessionAccountDataService.deleteUserAccountData(any())
        }
    }
}
