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

package im.vector.app.features.settings.devices.v2.details.extended

import im.vector.app.core.resources.BuildMeta
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAppNameProvider
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

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeAppNameProvider = FakeAppNameProvider()
    private val fakeBuildMeta = mockk<BuildMeta>()
    private val getMatrixClientInfoUseCase = mockk<GetMatrixClientInfoUseCase>()
    private val setMatrixClientInfoUseCase = mockk<SetMatrixClientInfoUseCase>()

    private val updateMatrixClientInfoUseCase = UpdateMatrixClientInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            appNameProvider = fakeAppNameProvider,
            buildMeta = fakeBuildMeta,
            getMatrixClientInfoUseCase = getMatrixClientInfoUseCase,
            setMatrixClientInfoUseCase = setMatrixClientInfoUseCase,
    )

    @Test
    fun `given current client info is different than the stored one when trying to update then new client info is set and result is success`() = runTest {
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
        val result = updateMatrixClientInfoUseCase.execute()

        // Then
        result.isSuccess shouldBe true
        coVerify { setMatrixClientInfoUseCase.execute(match { it == expectedClientInfoToSet }) }
    }

    @Test
    fun `given current client info is equal to the stored one when trying to update then nothing is done and result is success`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        givenStoredClientInfo(AN_APP_NAME_1, A_VERSION_NAME_1)

        // When
        val result = updateMatrixClientInfoUseCase.execute()

        // Then
        result.isSuccess shouldBe true
        coVerify(inverse = true) { setMatrixClientInfoUseCase.execute(any()) }
    }

    @Test
    fun `given error during setting new client info when trying to update then result is failure`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        givenStoredClientInfo(AN_APP_NAME_2, A_VERSION_NAME_2)
        val error = Exception()
        givenSetClientInfoFailsWithError(error)
        val expectedClientInfoToSet = MatrixClientInfoContent(
                name = AN_APP_NAME_1,
                version = A_VERSION_NAME_1,
        )

        // When
        val result = updateMatrixClientInfoUseCase.execute()

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
        coVerify { setMatrixClientInfoUseCase.execute(match { it == expectedClientInfoToSet }) }
    }

    @Test
    fun `given no session id for current session when trying to update then result is failure`() = runTest {
        // Given
        givenCurrentAppName(AN_APP_NAME_1)
        givenCurrentVersionName(A_VERSION_NAME_1)
        fakeActiveSessionHolder.fakeSession.givenSessionId(null)

        // When
        val result = updateMatrixClientInfoUseCase.execute()

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeInstanceOf NoDeviceIdError::class
    }

    private fun givenCurrentAppName(appName: String) {
        fakeAppNameProvider.givenAppName(appName)
    }

    private fun givenCurrentVersionName(versionName: String) {
        every { fakeBuildMeta.versionName } returns versionName
    }

    private fun givenStoredClientInfo(appName: String, versionName: String) {
        fakeActiveSessionHolder.fakeSession.givenSessionId(A_SESSION_ID)
        every { getMatrixClientInfoUseCase.execute(A_SESSION_ID) } returns MatrixClientInfoContent(
                name = appName,
                version = versionName,
        )
    }

    private fun givenSetClientInfoSucceeds() {
        coEvery { setMatrixClientInfoUseCase.execute(any()) } returns Result.success(Unit)
    }

    private fun givenSetClientInfoFailsWithError(error: Exception) {
        coEvery { setMatrixClientInfoUseCase.execute(any()) } throws error
    }
}
