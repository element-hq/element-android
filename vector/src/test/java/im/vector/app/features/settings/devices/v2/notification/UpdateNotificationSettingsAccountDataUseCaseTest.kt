/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import im.vector.app.test.fakes.FakeVectorPreferences
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class UpdateNotificationSettingsAccountDataUseCaseTest {

    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()
    private val fakeGetNotificationSettingsAccountDataUseCase = mockk<GetNotificationSettingsAccountDataUseCase>()
    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()
    private val fakeDeleteNotificationSettingsAccountDataUseCase = mockk<DeleteNotificationSettingsAccountDataUseCase>()

    private val updateNotificationSettingsAccountDataUseCase = UpdateNotificationSettingsAccountDataUseCase(
            vectorPreferences = fakeVectorPreferences.instance,
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
            getNotificationSettingsAccountDataUseCase = fakeGetNotificationSettingsAccountDataUseCase,
            setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
            deleteNotificationSettingsAccountDataUseCase = fakeDeleteNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given back sync enabled, a device id and a different local setting compared to remote when execute then content is updated`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationsEnabledForDevice(areNotificationsEnabled)
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(true)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(any(), any()) } returns
                LocalNotificationSettingsContent(
                        isSilenced = null
                )
        val expectedContent = LocalNotificationSettingsContent(
                isSilenced = !areNotificationsEnabled
        )

        // When
        updateNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        verify {
            fakeUnifiedPushHelper.instance.isBackgroundSync()
            fakeVectorPreferences.instance.areNotificationEnabledForDevice()
            fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)
        }
        coVerify(inverse = true) { fakeDeleteNotificationSettingsAccountDataUseCase.execute(aSession) }
        coVerify { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }

    @Test
    fun `given back sync enabled, a device id and a same local setting compared to remote when execute then content is not updated`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationsEnabledForDevice(areNotificationsEnabled)
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(true)
        every { fakeGetNotificationSettingsAccountDataUseCase.execute(any(), any()) } returns
                LocalNotificationSettingsContent(
                        isSilenced = false
                )
        val expectedContent = LocalNotificationSettingsContent(
                isSilenced = !areNotificationsEnabled
        )

        // When
        updateNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        verify {
            fakeUnifiedPushHelper.instance.isBackgroundSync()
            fakeVectorPreferences.instance.areNotificationEnabledForDevice()
            fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)
        }
        coVerify(inverse = true) { fakeDeleteNotificationSettingsAccountDataUseCase.execute(aSession) }
        coVerify(inverse = true) { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }

    @Test
    fun `given back sync disabled and a device id when execute then content is deleted`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeDeleteNotificationSettingsAccountDataUseCase.execute(any()) }
        fakeUnifiedPushHelper.givenIsBackgroundSyncReturns(false)

        // When
        updateNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        verify {
            fakeUnifiedPushHelper.instance.isBackgroundSync()
        }
        coVerify { fakeDeleteNotificationSettingsAccountDataUseCase.execute(aSession) }
        coVerify(inverse = true) { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, any()) }
    }
}
