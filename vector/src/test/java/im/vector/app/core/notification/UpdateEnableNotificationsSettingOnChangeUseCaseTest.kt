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

package im.vector.app.core.notification

import im.vector.app.features.settings.devices.v2.notification.NotificationsStatus
import im.vector.app.test.fakes.FakeGetNotificationsStatusUseCase
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeVectorPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val A_SESSION_ID = "session-id"

class UpdateEnableNotificationsSettingOnChangeUseCaseTest {

    private val fakeSession = FakeSession().also { it.givenSessionId(A_SESSION_ID) }
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeGetNotificationsStatusUseCase = FakeGetNotificationsStatusUseCase()

    private val updateEnableNotificationsSettingOnChangeUseCase = UpdateEnableNotificationsSettingOnChangeUseCase(
            vectorPreferences = fakeVectorPreferences.instance,
            getNotificationsStatusUseCase = fakeGetNotificationsStatusUseCase.instance,
    )

    @Test
    fun `given notifications are enabled when execute then setting is updated to true`() = runTest {
        // Given
        fakeGetNotificationsStatusUseCase.givenExecuteReturns(
                fakeSession,
                A_SESSION_ID,
                NotificationsStatus.ENABLED,
        )
        fakeVectorPreferences.givenSetNotificationEnabledForDevice()

        // When
        updateEnableNotificationsSettingOnChangeUseCase.execute(fakeSession)

        // Then
        fakeVectorPreferences.verifySetNotificationEnabledForDevice(true)
    }

    @Test
    fun `given notifications are disabled when execute then setting is updated to false`() = runTest {
        // Given
        fakeGetNotificationsStatusUseCase.givenExecuteReturns(
                fakeSession,
                A_SESSION_ID,
                NotificationsStatus.DISABLED,
        )
        fakeVectorPreferences.givenSetNotificationEnabledForDevice()

        // When
        updateEnableNotificationsSettingOnChangeUseCase.execute(fakeSession)

        // Then
        fakeVectorPreferences.verifySetNotificationEnabledForDevice(false)
    }

    @Test
    fun `given notifications toggle is not supported when execute then nothing is done`() = runTest {
        // Given
        fakeGetNotificationsStatusUseCase.givenExecuteReturns(
                fakeSession,
                A_SESSION_ID,
                NotificationsStatus.NOT_SUPPORTED,
        )
        fakeVectorPreferences.givenSetNotificationEnabledForDevice()

        // When
        updateEnableNotificationsSettingOnChangeUseCase.execute(fakeSession)

        // Then
        fakeVectorPreferences.verifySetNotificationEnabledForDevice(true, inverse = true)
        fakeVectorPreferences.verifySetNotificationEnabledForDevice(false, inverse = true)
    }
}
