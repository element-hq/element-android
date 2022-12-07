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

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fixtures.PusherFixture
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class ToggleNotificationsUseCaseTest {

    private val activeSessionHolder = FakeActiveSessionHolder()
    private val fakeCheckIfCanToggleNotificationsViaPusherUseCase =
            mockk<CheckIfCanToggleNotificationsViaPusherUseCase>()
    private val fakeCheckIfCanToggleNotificationsViaAccountDataUseCase =
            mockk<CheckIfCanToggleNotificationsViaAccountDataUseCase>()
    private val fakeSetNotificationSettingsAccountDataUseCase =
            mockk<SetNotificationSettingsAccountDataUseCase>()

    private val toggleNotificationsUseCase =
            ToggleNotificationsUseCase(
                    activeSessionHolder = activeSessionHolder.instance,
                    checkIfCanToggleNotificationsViaPusherUseCase = fakeCheckIfCanToggleNotificationsViaPusherUseCase,
                    checkIfCanToggleNotificationsViaAccountDataUseCase = fakeCheckIfCanToggleNotificationsViaAccountDataUseCase,
                    setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
            )

    @Test
    fun `when execute, then toggle enabled for device pushers`() = runTest {
        // Given
        val sessionId = "a_session_id"
        val pushers = listOf(
                PusherFixture.aPusher(deviceId = sessionId, enabled = false),
                PusherFixture.aPusher(deviceId = "another id", enabled = false)
        )
        val fakeSession = activeSessionHolder.fakeSession
        fakeSession.pushersService().givenPushersLive(pushers)
        fakeSession.pushersService().givenGetPushers(pushers)
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns true
        every { fakeCheckIfCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, sessionId) } returns false

        // When
        toggleNotificationsUseCase.execute(sessionId, true)

        // Then
        activeSessionHolder.fakeSession.pushersService().verifyTogglePusherCalled(pushers.first(), true)
    }

    @Test
    fun `when execute, then toggle local notification settings`() = runTest {
        // Given
        val sessionId = "a_session_id"
        val fakeSession = activeSessionHolder.fakeSession
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns false
        every { fakeCheckIfCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, sessionId) } returns true
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val expectedLocalNotificationSettingsContent = LocalNotificationSettingsContent(
                isSilenced = false
        )

        // When
        toggleNotificationsUseCase.execute(sessionId, true)

        // Then
        coVerify {
            fakeSetNotificationSettingsAccountDataUseCase.execute(fakeSession, sessionId, expectedLocalNotificationSettingsContent)
        }
    }
}
