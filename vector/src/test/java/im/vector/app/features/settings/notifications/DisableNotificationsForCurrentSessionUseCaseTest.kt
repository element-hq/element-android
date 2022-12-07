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

package im.vector.app.features.settings.notifications

import im.vector.app.core.pushers.UnregisterUnifiedPushUseCase
import im.vector.app.test.fakes.FakePushersManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DisableNotificationsForCurrentSessionUseCaseTest {

    private val fakePushersManager = FakePushersManager()
    private val fakeToggleNotificationsForCurrentSessionUseCase = mockk<ToggleNotificationsForCurrentSessionUseCase>()
    private val fakeUnregisterUnifiedPushUseCase = mockk<UnregisterUnifiedPushUseCase>()

    private val disableNotificationsForCurrentSessionUseCase = DisableNotificationsForCurrentSessionUseCase(
            pushersManager = fakePushersManager.instance,
            toggleNotificationsForCurrentSessionUseCase = fakeToggleNotificationsForCurrentSessionUseCase,
            unregisterUnifiedPushUseCase = fakeUnregisterUnifiedPushUseCase,
    )

    @Test
    fun `when execute then disable notifications and unregister the pusher`() = runTest {
        // Given
        coJustRun { fakeToggleNotificationsForCurrentSessionUseCase.execute(any()) }
        coJustRun { fakeUnregisterUnifiedPushUseCase.execute(any()) }

        // When
        disableNotificationsForCurrentSessionUseCase.execute()

        // Then
        coVerify {
            fakeToggleNotificationsForCurrentSessionUseCase.execute(false)
            fakeUnregisterUnifiedPushUseCase.execute(fakePushersManager.instance)
        }
    }
}
