/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import im.vector.app.test.fakes.FakeSession
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.toContent

private const val A_DEVICE_ID = "device-id"

class SetMatrixClientInfoUseCaseTest {

    private val fakeSession = FakeSession()

    private val setMatrixClientInfoUseCase = SetMatrixClientInfoUseCase()

    @Test
    fun `given client info and no error when setting the info then account data is correctly updated`() = runTest {
        // Given
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID
        val clientInfo = givenClientInfo()
        val content = clientInfo.toContent()
        fakeSession
                .givenSessionId(A_DEVICE_ID)
        fakeSession
                .fakeSessionAccountDataService
                .givenUpdateUserAccountDataEventSucceeds()

        // When
        val result = setMatrixClientInfoUseCase.execute(fakeSession, clientInfo)

        // Then
        result.isSuccess shouldBe true
        fakeSession
                .fakeSessionAccountDataService
                .verifyUpdateUserAccountDataEventSucceeds(type, content)
    }

    @Test
    fun `given client info and error during update when setting the info then result is failure`() = runTest {
        // Given
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID
        val clientInfo = givenClientInfo()
        val content = clientInfo.toContent()
        fakeSession
                .givenSessionId(A_DEVICE_ID)
        val error = Exception()
        fakeSession
                .fakeSessionAccountDataService
                .givenUpdateUserAccountDataEventFailsWithError(error)

        // When
        val result = setMatrixClientInfoUseCase.execute(fakeSession, clientInfo)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
        fakeSession
                .fakeSessionAccountDataService
                .verifyUpdateUserAccountDataEventSucceeds(type, content)
    }

    @Test
    fun `given client info and null device id when setting the info then result is failure`() = runTest {
        // Given
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + A_DEVICE_ID
        val clientInfo = givenClientInfo()
        val content = clientInfo.toContent()
        fakeSession
                .givenSessionId(null)

        // When
        val result = setMatrixClientInfoUseCase.execute(fakeSession, clientInfo)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeInstanceOf NoDeviceIdError::class
        fakeSession
                .fakeSessionAccountDataService
                .verifyUpdateUserAccountDataEventSucceeds(type, content, inverse = true)
    }

    private fun givenClientInfo() = MatrixClientInfoContent(
            name = "name",
            version = "version",
            url = null,
    )
}
