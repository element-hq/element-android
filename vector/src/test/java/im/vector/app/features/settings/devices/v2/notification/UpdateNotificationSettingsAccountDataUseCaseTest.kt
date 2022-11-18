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

import im.vector.app.test.fakes.FakeSession
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
    private val fakeGetNotificationSettingsAccountDataUseCase = mockk<GetNotificationSettingsAccountDataUseCase>()
    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()

    private val updateNotificationSettingsAccountDataUseCase = UpdateNotificationSettingsAccountDataUseCase(
            vectorPreferences = fakeVectorPreferences.instance,
            getNotificationSettingsAccountDataUseCase = fakeGetNotificationSettingsAccountDataUseCase,
            setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given a device id and a different local setting compared to remote when execute then content is updated`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationEnabled(areNotificationsEnabled)
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
            fakeVectorPreferences.instance.areNotificationEnabledForDevice()
            fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)
        }
        coVerify { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }

    @Test
    fun `given a device id and a same local setting compared to remote when execute then content is not updated`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationEnabled(areNotificationsEnabled)
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
            fakeVectorPreferences.instance.areNotificationEnabledForDevice()
            fakeGetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)
        }
        coVerify(inverse = true) { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }
}