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

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.SessionParams

private const val A_SESSION_ID_1 = "session-id-1"
private const val A_SESSION_ID_2 = "session-id-2"

class IsCurrentSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val isCurrentSessionUseCase = IsCurrentSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Test
    fun `given the session id of the current session when checking if id is current session then result is true`() {
        // Given
        val sessionParams = givenIdForCurrentSession(A_SESSION_ID_1)

        // When
        val result = isCurrentSessionUseCase.execute(A_SESSION_ID_1)

        // Then
        result shouldBe true
        verify { sessionParams.deviceId }
    }

    @Test
    fun `given a session id different from the current session id when checking if id is current session then result is false`() {
        // Given
        val sessionParams = givenIdForCurrentSession(A_SESSION_ID_1)

        // When
        val result = isCurrentSessionUseCase.execute(A_SESSION_ID_2)

        // Then
        result shouldBe false
        verify { sessionParams.deviceId }
    }

    @Test
    fun `given no current active session when checking if id is current session then result is false`() {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val result = isCurrentSessionUseCase.execute(A_SESSION_ID_1)

        // Then
        result shouldBe false
    }

    private fun givenIdForCurrentSession(sessionId: String): SessionParams {
        return fakeActiveSessionHolder.fakeSession.givenSessionId(sessionId)
    }
}
