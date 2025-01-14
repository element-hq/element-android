/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

private const val A_DEVICE_ID = "device-id"

class CheckIfCanToggleNotificationsViaAccountDataUseCaseTest {

    private val fakeGetNotificationSettingsAccountDataUseCase = mockk<GetNotificationSettingsAccountDataUseCase>()
    private val fakeSession = FakeSession()

    private val checkIfCanToggleNotificationsViaAccountDataUseCase =
            CheckIfCanToggleNotificationsViaAccountDataUseCase(
                    getNotificationSettingsAccountDataUseCase = fakeGetNotificationSettingsAccountDataUseCase,
            )

    @Test
    fun `given current session and an account data with a content for the device id when execute then result is true`() {
        // Given
        val content = LocalNotificationSettingsContent(isSilenced = true)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns content

        // When
        val result = checkIfCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo true
    }

    @Test
    fun `given current session and an account data with empty content for the device id when execute then result is false`() {
        // Given
        val content = LocalNotificationSettingsContent(isSilenced = null)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns content

        // When
        val result = checkIfCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo false
    }

    @Test
    fun `given current session and NO account data for the device id when execute then result is false`() {
        // Given
        val content = null
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns content

        // When
        val result = checkIfCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo false
    }
}
