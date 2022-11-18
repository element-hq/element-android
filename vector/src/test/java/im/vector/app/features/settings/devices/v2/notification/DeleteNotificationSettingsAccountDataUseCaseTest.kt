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
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class DeleteNotificationSettingsAccountDataUseCaseTest {

    private val fakeSetNotificationSettingsAccountDataUseCase = mockk<SetNotificationSettingsAccountDataUseCase>()

    private val deleteNotificationSettingsAccountDataUseCase = DeleteNotificationSettingsAccountDataUseCase(
            setNotificationSettingsAccountDataUseCase = fakeSetNotificationSettingsAccountDataUseCase,
    )

    @Test
    fun `given a device id when execute then empty content is set for the account data`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession.givenSessionId(aDeviceId)
        coJustRun { fakeSetNotificationSettingsAccountDataUseCase.execute(any(), any(), any()) }
        val expectedContent = LocalNotificationSettingsContent(
                isSilenced = null
        )

        // When
        deleteNotificationSettingsAccountDataUseCase.execute(aSession)

        // Then
        coVerify { fakeSetNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId, expectedContent) }
    }
}
