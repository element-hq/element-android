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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent

class CanToggleNotificationsViaAccountDataUseCaseTest {

    private val fakeGetNotificationSettingsAccountDataAsFlowUseCase = mockk<GetNotificationSettingsAccountDataAsFlowUseCase>()

    private val canToggleNotificationsViaAccountDataUseCase = CanToggleNotificationsViaAccountDataUseCase(
            getNotificationSettingsAccountDataAsFlowUseCase = fakeGetNotificationSettingsAccountDataAsFlowUseCase,
    )

    @Test
    fun `given current session and content for account data when execute then true is returned`() = runTest {
        // Given
        val aSession = FakeSession()
        val aDeviceId = "aDeviceId"
        val localNotificationSettingsContent = LocalNotificationSettingsContent(
                isSilenced = true,
        )
        every { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(any(), any()) } returns flowOf(localNotificationSettingsContent)

        // When
        val result = canToggleNotificationsViaAccountDataUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBe true
        verify { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(aSession, aDeviceId) }
    }

    @Test
    fun `given current session and empty content for account data when execute then false is returned`() = runTest {
        // Given
        val aSession = FakeSession()
        val aDeviceId = "aDeviceId"
        val localNotificationSettingsContent = LocalNotificationSettingsContent(
                isSilenced = null,
        )
        every { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(any(), any()) } returns flowOf(localNotificationSettingsContent)

        // When
        val result = canToggleNotificationsViaAccountDataUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBe false
        verify { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(aSession, aDeviceId) }
    }

    @Test
    fun `given current session and no related account data when execute then false is returned`() = runTest {
        // Given
        val aSession = FakeSession()
        val aDeviceId = "aDeviceId"
        every { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(any(), any()) } returns flowOf(null)

        // When
        val result = canToggleNotificationsViaAccountDataUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBe false
        verify { fakeGetNotificationSettingsAccountDataAsFlowUseCase.execute(aSession, aDeviceId) }
    }
}
