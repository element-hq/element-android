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

import androidx.fragment.app.FragmentActivity
import im.vector.app.features.settings.devices.v2.notification.CheckIfCanTogglePushNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.TogglePushNotificationUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFcmHelper
import im.vector.app.test.fakes.FakePushersManager
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val A_SESSION_ID = "session-id"

class EnableNotificationsForCurrentSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()
    private val fakePushersManager = FakePushersManager()
    private val fakeFcmHelper = FakeFcmHelper()
    private val fakeCheckIfCanTogglePushNotificationsViaPusherUseCase = mockk<CheckIfCanTogglePushNotificationsViaPusherUseCase>()
    private val fakeTogglePushNotificationUseCase = mockk<TogglePushNotificationUseCase>()

    private val enableNotificationsForCurrentSessionUseCase = EnableNotificationsForCurrentSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
            pushersManager = fakePushersManager.instance,
            fcmHelper = fakeFcmHelper.instance,
            checkIfCanTogglePushNotificationsViaPusherUseCase = fakeCheckIfCanTogglePushNotificationsViaPusherUseCase,
            togglePushNotificationUseCase = fakeTogglePushNotificationUseCase,
    )

    @Test
    fun `given no existing pusher for current session when execute then a new pusher is registered`() = runTest {
        // Given
        val fragmentActivity = mockk<FragmentActivity>()
        fakePushersManager.givenGetPusherForCurrentSessionReturns(null)
        fakeUnifiedPushHelper.givenRegister(fragmentActivity)
        fakeUnifiedPushHelper.givenIsEmbeddedDistributorReturns(true)
        fakeFcmHelper.givenEnsureFcmTokenIsRetrieved(fragmentActivity, fakePushersManager.instance)
        every { fakeCheckIfCanTogglePushNotificationsViaPusherUseCase.execute(fakeActiveSessionHolder.fakeSession) } returns false

        // When
        enableNotificationsForCurrentSessionUseCase.execute(fragmentActivity)

        // Then
        fakeUnifiedPushHelper.verifyRegister(fragmentActivity)
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(fragmentActivity, fakePushersManager.instance, registerPusher = true)
    }

    @Test
    fun `given toggle via Pusher is possible when execute then current pusher is toggled to true`() = runTest {
        // Given
        val fragmentActivity = mockk<FragmentActivity>()
        fakePushersManager.givenGetPusherForCurrentSessionReturns(mockk())
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(A_SESSION_ID)
        every { fakeCheckIfCanTogglePushNotificationsViaPusherUseCase.execute(fakeActiveSessionHolder.fakeSession) } returns true
        coJustRun { fakeTogglePushNotificationUseCase.execute(A_SESSION_ID, any()) }

        // When
        enableNotificationsForCurrentSessionUseCase.execute(fragmentActivity)

        // Then
        coVerify { fakeTogglePushNotificationUseCase.execute(A_SESSION_ID, true) }
    }
}
