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

package im.vector.app.features.settings.notifications

import im.vector.app.core.pushers.UnregisterUnifiedPushUseCase
import im.vector.app.features.settings.devices.v2.notification.CheckIfCanToggleNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.ToggleNotificationsUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakePushersManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val A_SESSION_ID = "session-id"

class DisableNotificationsForCurrentSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakePushersManager = FakePushersManager()
    private val fakeCheckIfCanToggleNotificationsViaPusherUseCase = mockk<CheckIfCanToggleNotificationsViaPusherUseCase>()
    private val fakeToggleNotificationsUseCase = mockk<ToggleNotificationsUseCase>()
    private val fakeUnregisterUnifiedPushUseCase = mockk<UnregisterUnifiedPushUseCase>()

    private val disableNotificationsForCurrentSessionUseCase = DisableNotificationsForCurrentSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            pushersManager = fakePushersManager.instance,
            checkIfCanToggleNotificationsViaPusherUseCase = fakeCheckIfCanToggleNotificationsViaPusherUseCase,
            toggleNotificationUseCase = fakeToggleNotificationsUseCase,
            unregisterUnifiedPushUseCase = fakeUnregisterUnifiedPushUseCase,
    )

    @Test
    fun `given toggle via pusher is possible when execute then disable notification via toggle of existing pusher`() = runTest {
        // Given
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(A_SESSION_ID)
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns true
        coJustRun { fakeToggleNotificationsUseCase.execute(A_SESSION_ID, any()) }

        // When
        disableNotificationsForCurrentSessionUseCase.execute()

        // Then
        coVerify { fakeToggleNotificationsUseCase.execute(A_SESSION_ID, false) }
    }

    @Test
    fun `given toggle via pusher is NOT possible when execute then disable notification by unregistering the pusher`() = runTest {
        // Given
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(A_SESSION_ID)
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns false
        coJustRun { fakeToggleNotificationsUseCase.execute(A_SESSION_ID, any()) }
        coJustRun { fakeUnregisterUnifiedPushUseCase.execute(any()) }

        // When
        disableNotificationsForCurrentSessionUseCase.execute()

        // Then
        coVerify {
            fakeToggleNotificationsUseCase.execute(A_SESSION_ID, false)
            fakeUnregisterUnifiedPushUseCase.execute(fakePushersManager.instance)
        }
    }
}
