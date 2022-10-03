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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_DEVICE_ID = "device-id"
private const val A_CLIENT_NAME = "client-name"
private const val A_CLIENT_VERSION = "client-version"
private const val A_CLIENT_URL = "client-url"

class GetMatrixClientInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val getMatrixClientInfoUseCase = GetMatrixClientInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given a device id and existing content when getting the info then result should contain that info`() {
        // Given
        givenClientInfoContent(A_DEVICE_ID)
        val expectedClientInfo = MatrixClientInfoContent(
                name = A_CLIENT_NAME,
                version = A_CLIENT_VERSION,
                url = A_CLIENT_URL,
        )

        // When
        val result = getMatrixClientInfoUseCase.execute(A_DEVICE_ID)

        // Then
        result shouldBeEqualTo expectedClientInfo
    }

    @Test
    fun `given no active session when getting the info then result should be null`() {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val result = getMatrixClientInfoUseCase.execute(A_DEVICE_ID)

        // Then
        result shouldBe null
    }

    private fun givenClientInfoContent(deviceId: String) {
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + deviceId
        val content = mapOf(
                Pair("name", A_CLIENT_NAME),
                Pair("version", A_CLIENT_VERSION),
                Pair("url", A_CLIENT_URL),
        )
        fakeActiveSessionHolder.fakeSession
                .fakeSessionAccountDataService
                .givenGetUserAccountDataEventReturns(type, content)
    }
}
