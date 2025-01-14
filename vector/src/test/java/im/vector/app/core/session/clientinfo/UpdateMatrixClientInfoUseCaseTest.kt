/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import im.vector.app.core.resources.BuildMeta
import im.vector.app.test.fakes.FakeAppNameProvider
import im.vector.app.test.fakes.FakeSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test

private const val AN_APP_NAME_1 = "app_name_1"
private const val AN_APP_NAME_2 = "app_name_2"
private const val A_VERSION_NAME_1 = "version_name_1"
private const val A_VERSION_NAME_2 = "version_name_2"
private const val A_SESSION_ID = "session-id"

class UpdateMatrixClientInfoUseCaseTest {

    private val fakeSession = FakeSession()
    private val fakeAppNameProvider = FakeAppNameProvider()
    private val fakeBuildMeta = mockk<BuildMeta>()
    private val getMatrixClientInfoUseCase = mockk<GetMatrixClientInfoUseCase>()
    private val setMatrixClientInfoUseCase = mockk<SetMatrixClientInfoUseCase>()

    private val updateMatrixClientInfoUseCase = UpdateMatrixClientInfoUseCase(
            appNameProvider = fakeAppNameProvider,
            buildMeta = fakeBuildMeta,
            getMatrixClientInfoUseCase = getMatrixClientInfoUseCase,
            setMatrixClientInfoUseCase = setMatrixClientInfoUseCase,
    )

    @Test
    fun `given current client info is different than the stored one when trying to update then new client info is set`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        givenStoredClientInfo(AN_APP_NAME_2, A_VERSION_NAME_2)
        givenSetClientInfoSucceeds()
        val expectedClientInfoToSet = MatrixClientInfoContent(
                name = AN_APP_NAME_1,
                version = A_VERSION_NAME_1,
        )

        // When
        val result = updateMatrixClientInfoUseCase.execute(fakeSession)

        // Then
        result.isSuccess shouldBe true
        coVerify { setMatrixClientInfoUseCase.execute(fakeSession, match { it == expectedClientInfoToSet }) }
    }

    @Test
    fun `given error during set of new client info when trying to update then result is failure`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        givenStoredClientInfo(AN_APP_NAME_2, A_VERSION_NAME_2)
        val error = Exception()
        givenSetClientInfoFails(error)
        val expectedClientInfoToSet = MatrixClientInfoContent(
                name = AN_APP_NAME_1,
                version = A_VERSION_NAME_1,
        )

        // When
        val result = updateMatrixClientInfoUseCase.execute(fakeSession)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
        coVerify { setMatrixClientInfoUseCase.execute(fakeSession, match { it == expectedClientInfoToSet }) }
    }

    @Test
    fun `given current client info is equal to the stored one when trying to update then nothing is done`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        givenStoredClientInfo(AN_APP_NAME_1, A_VERSION_NAME_1)

        // When
        val result = updateMatrixClientInfoUseCase.execute(fakeSession)

        // Then
        result.isSuccess shouldBe true
        coVerify(inverse = true) { setMatrixClientInfoUseCase.execute(fakeSession, any()) }
    }

    @Test
    fun `given no session id for current session when trying to update then nothing is done`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        fakeSession.givenSessionId(null)

        // When
        val result = updateMatrixClientInfoUseCase.execute(fakeSession)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeInstanceOf NoDeviceIdError::class
        coVerify(inverse = true) { setMatrixClientInfoUseCase.execute(fakeSession, any()) }
    }

    private fun givenCurrentAppName(appName: String) {
        fakeAppNameProvider.givenAppName(appName)
    }

    private fun givenCurrentVersionName(versionName: String) {
        every { fakeBuildMeta.versionName } returns versionName
    }

    private fun givenStoredClientInfo(appName: String, versionName: String) {
        fakeSession.givenSessionId(A_SESSION_ID)
        every { getMatrixClientInfoUseCase.execute(fakeSession, A_SESSION_ID) } returns MatrixClientInfoContent(
                name = appName,
                version = versionName,
        )
    }

    private fun givenSetClientInfoSucceeds() {
        coEvery { setMatrixClientInfoUseCase.execute(any(), any()) } returns Result.success(Unit)
    }

    private fun givenSetClientInfoFails(error: Throwable) {
        coEvery { setMatrixClientInfoUseCase.execute(any(), any()) } returns Result.failure(error)
    }
}
