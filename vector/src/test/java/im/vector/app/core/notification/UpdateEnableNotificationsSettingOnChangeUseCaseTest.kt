/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
