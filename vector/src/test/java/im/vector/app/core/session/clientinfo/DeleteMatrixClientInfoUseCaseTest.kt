/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class DeleteMatrixClientInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeSetMatrixClientInfoUseCase = mockk<SetMatrixClientInfoUseCase>()

    private val deleteMatrixClientInfoUseCase = DeleteMatrixClientInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            setMatrixClientInfoUseCase = fakeSetMatrixClientInfoUseCase
    )

    @Test
    fun `given current session when calling use case then empty client info is set and result is success`() = runTest {
        // Given
        givenSetMatrixClientInfoSucceeds()
        val expectedClientInfoToBeSet = MatrixClientInfoContent(
                name = "",
                version = "",
                url = "",
        )

        // When
        val result = deleteMatrixClientInfoUseCase.execute()

        // Then
        result.isSuccess shouldBe true
        coVerify {
            fakeSetMatrixClientInfoUseCase.execute(
                    fakeActiveSessionHolder.fakeSession,
                    expectedClientInfoToBeSet
            )
        }
    }

    @Test
    fun `given current session and error during the process when calling use case then result is failure`() = runTest {
        // Given
        val error = Exception()
        givenSetMatrixClientInfoFails(error)
        val expectedClientInfoToBeSet = MatrixClientInfoContent(
                name = "",
                version = "",
                url = "",
        )

        // When
        val result = deleteMatrixClientInfoUseCase.execute()

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
        coVerify {
            fakeSetMatrixClientInfoUseCase.execute(
                    fakeActiveSessionHolder.fakeSession,
                    expectedClientInfoToBeSet
            )
        }
    }

    private fun givenSetMatrixClientInfoSucceeds() {
        coEvery { fakeSetMatrixClientInfoUseCase.execute(any(), any()) } returns Result.success(Unit)
    }

    private fun givenSetMatrixClientInfoFails(error: Exception) {
        coEvery { fakeSetMatrixClientInfoUseCase.execute(any(), any()) } returns Result.failure(error)
    }
}
