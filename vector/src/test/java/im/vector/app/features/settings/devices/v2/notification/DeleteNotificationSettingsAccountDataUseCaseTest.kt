/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeSession
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class DeleteNotificationSettingsAccountDataUseCaseTest {

    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()
    private val fakeGetNotificationSettingsAccountDataUseCase = mockk<GetNotificationSettingsAccountDataUseCase>()

    private val deleteNotificationSettingsAccountDataUseCase = DeleteNotificationSettingsAccountDataUseCase(
            setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
            getNotificationSettingsAccountDataUseCase = fakeGetNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given a device id and existing account data content when execute then empty content is set for the account data`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(any(), any()) } returns LocalNotificationSettingsContent(
                isSilenced = true,
        )
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val expectedContent = LocalNotificationSettingsContent(
                isSilenced = null
        )

        // When
        deleteNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        verify { fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId) }
        coVerify { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }

    @Test
    fun `given a device id and empty existing account data content when execute then nothing is done`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(any(), any()) } returns LocalNotificationSettingsContent(
                isSilenced = null,
        )

        // When
        deleteNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        verify { fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId) }
        coVerify(inverse = true) { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, any()) }
    }
}
