/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import im.vector.app.test.fakes.FakeSession
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_DEVICE_ID = "device-id"
private const val A_CLIENT_NAME = "client-name"
private const val A_CLIENT_VERSION = "client-version"
private const val A_CLIENT_URL = "client-url"

class GetMatrixClientInfoUseCaseTest {

    private val fakeSession = FakeSession()

    private val getMatrixClientInfoUseCase = GetMatrixClientInfoUseCase()

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
        val result = getMatrixClientInfoUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo expectedClientInfo
    }

    private fun givenClientInfoContent(deviceId: String) {
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + deviceId
        val content = mapOf(
                Pair("name", A_CLIENT_NAME),
                Pair("version", A_CLIENT_VERSION),
                Pair("url", A_CLIENT_URL),
        )
        fakeSession
                .fakeSessionAccountDataService
                .givenGetUserAccountDataEventReturns(type, content)
    }
}
