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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class CreateNotificationSettingsAccountDataUseCaseTest {

    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()

    private val createNotificationSettingsAccountDataUseCase = CreateNotificationSettingsAccountDataUseCase(
        vectorPreferences = fakeVectorPreferences.instance,
        setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given a device id when execute then content with the current notification preference is set for the account data`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val session = FakeSession()
        session.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationEnabled(areNotificationsEnabled)
        val expectedContent = LocalNotificationSettingsContent(
                isSilenced = !areNotificationsEnabled
        )

        // When
        createNotificationSettingsAccountDataUseCase.execute(session)

        // Then
        verify { fakeVectorPreferences.instance.areNotificationEnabledForDevice() }
        coVerify { fakeSetNotificationSettingsAccountDataUseCase.execute(aDeviceId, expectedContent) }
    }
}
