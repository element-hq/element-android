/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.features.settings.devices.v2.notification.CheckIfCanToggleNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.DeleteNotificationSettingsAccountDataUseCase
import im.vector.app.features.settings.devices.v2.notification.SetNotificationSettingsAccountDataUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import im.vector.app.test.fixtures.PusherFixture
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class ToggleNotificationsForCurrentSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()
    private val fakeCheckIfCanToggleNotificationsViaPusherUseCase = mockk<CheckIfCanToggleNotificationsViaPusherUseCase>()
    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()
    private val fakeDeleteNotificationSettingsAccountDataUseCase = mockk<DeleteNotificationSettingsAccountDataUseCase>()

    private val toggleNotificationsForCurrentSessionUseCase = ToggleNotificationsForCurrentSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
            checkIfCanToggleNotificationsViaPusherUseCase = fakeCheckIfCanToggleNotificationsViaPusherUseCase,
            setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
            deleteNotificationSettingsAccountDataUseCase = fakeDeleteNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given background sync is enabled when execute then set the related account data with correct value`() = runTest {
        // Given
        val enabled = true
        val aDeviceId = "deviceId"
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(true)
        fakeActiveSessionHolder.fakeSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val expectedNotificationContent = LocalNotificationSettingsContent(isSilenced = !enabled)

        // When
        toggleNotificationsForCurrentSessionUseCase.execute(enabled)

        // Then
        coVerify {
            fakeSetNotificationSettingsAccountDataUseCase.execute(
                    fakeActiveSessionHolder.fakeSession,
                    aDeviceId,
                    expectedNotificationContent
            )
        }
    }

    @Test
    fun `given background sync is not enabled and toggle pusher is possible when execute then delete any related account data and toggle pusher`() = runTest {
        // Given
        val enabled = true
        val aDeviceId = "deviceId"
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(false)
        fakeActiveSessionHolder.fakeSession.givenSessionId(aDeviceId)
        coJustRun { fakeDeleteNotificationSettingsAccountDataUseCase.execute(any()) }
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(any()) } returns true
        val aPusher = PusherFixture.aPusher(deviceId = aDeviceId)
        fakeActiveSessionHolder.fakeSession.fakePushersService.givenGetPushers(listOf(aPusher))

        // When
        toggleNotificationsForCurrentSessionUseCase.execute(enabled)

        // Then
        coVerify {
            fakeDeleteNotificationSettingsAccountDataUseCase.execute(fakeActiveSessionHolder.fakeSession)
            fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeActiveSessionHolder.fakeSession)
        }
        fakeActiveSessionHolder.fakeSession.fakePushersService.verifyTogglePusherCalled(aPusher, enabled)
    }

    @Test
    fun `given background sync is not enabled and toggle pusher is not possible when execute then only delete any related account data`() = runTest {
        // Given
        val enabled = true
        val aDeviceId = "deviceId"
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(false)
        fakeActiveSessionHolder.fakeSession.givenSessionId(aDeviceId)
        coJustRun { fakeDeleteNotificationSettingsAccountDataUseCase.execute(any()) }
        every { fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(any()) } returns false

        // When
        toggleNotificationsForCurrentSessionUseCase.execute(enabled)

        // Then
        coVerify {
            fakeDeleteNotificationSettingsAccountDataUseCase.execute(fakeActiveSessionHolder.fakeSession)
            fakeCheckIfCanToggleNotificationsViaPusherUseCase.execute(fakeActiveSessionHolder.fakeSession)
        }
        coVerify(inverse = true) {
            fakeActiveSessionHolder.fakeSession.fakePushersService.togglePusher(any(), any())
        }
    }
}
