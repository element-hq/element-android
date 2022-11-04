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

import im.vector.app.features.settings.devices.v2.notification.CheckIfCanTogglePushNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.TogglePushNotificationUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakePushersManager
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val A_SESSION_ID = "session-id"

class DisableNotificationsForCurrentSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()
    private val fakePushersManager = FakePushersManager()
    private val fakeCheckIfCanTogglePushNotificationsViaPusherUseCase = mockk<CheckIfCanTogglePushNotificationsViaPusherUseCase>()
    private val fakeTogglePushNotificationUseCase = mockk<TogglePushNotificationUseCase>()

    private val disableNotificationsForCurrentSessionUseCase = DisableNotificationsForCurrentSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
            pushersManager = fakePushersManager.instance,
            checkIfCanTogglePushNotificationsViaPusherUseCase = fakeCheckIfCanTogglePushNotificationsViaPusherUseCase,
            togglePushNotificationUseCase = fakeTogglePushNotificationUseCase,
    )

    @Test
    fun `given toggle via pusher is possible when execute then disable notification via toggle of existing pusher`() = runTest {
        // Given
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(A_SESSION_ID)
        every { fakeCheckIfCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession) } returns true
        coJustRun { fakeTogglePushNotificationUseCase.execute(A_SESSION_ID, any()) }

        // When
        disableNotificationsForCurrentSessionUseCase.execute()

        // Then
        coVerify { fakeTogglePushNotificationUseCase.execute(A_SESSION_ID, false) }
    }

    @Test
    fun `given toggle via pusher is NOT possible when execute then disable notification by unregistering the pusher`() = runTest {
        // Given
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(A_SESSION_ID)
        every { fakeCheckIfCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession) } returns false
        fakeUnifiedPushHelper.givenUnregister(fakePushersManager.instance)

        // When
        disableNotificationsForCurrentSessionUseCase.execute()

        // Then
        fakeUnifiedPushHelper.verifyUnregister(fakePushersManager.instance)
    }
}
