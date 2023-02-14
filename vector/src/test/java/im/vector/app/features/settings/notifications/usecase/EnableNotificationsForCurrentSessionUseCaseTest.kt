/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.core.pushers.EnsureFcmTokenIsRetrievedUseCase
import im.vector.app.core.pushers.RegisterUnifiedPushUseCase
import im.vector.app.test.fakes.FakePushersManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Test

class EnableNotificationsForCurrentSessionUseCaseTest {

    private val fakePushersManager = FakePushersManager()
    private val fakeToggleNotificationsForCurrentSessionUseCase = mockk<ToggleNotificationsForCurrentSessionUseCase>()
    private val fakeRegisterUnifiedPushUseCase = mockk<RegisterUnifiedPushUseCase>()
    private val fakeEnsureFcmTokenIsRetrievedUseCase = mockk<EnsureFcmTokenIsRetrievedUseCase>()

    private val enableNotificationsForCurrentSessionUseCase = EnableNotificationsForCurrentSessionUseCase(
            pushersManager = fakePushersManager.instance,
            toggleNotificationsForCurrentSessionUseCase = fakeToggleNotificationsForCurrentSessionUseCase,
            registerUnifiedPushUseCase = fakeRegisterUnifiedPushUseCase,
            ensureFcmTokenIsRetrievedUseCase = fakeEnsureFcmTokenIsRetrievedUseCase,
    )

    @Test
    fun `given no existing pusher and a registered distributor when execute then a new pusher is registered and result is success`() = runTest {
        // Given
        val aDistributor = "distributor"
        fakePushersManager.givenGetPusherForCurrentSessionReturns(null)
        every { fakeRegisterUnifiedPushUseCase.execute(any()) } returns RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        justRun { fakeEnsureFcmTokenIsRetrievedUseCase.execute(any(), any()) }
        coJustRun { fakeToggleNotificationsForCurrentSessionUseCase.execute(any()) }

        // When
        val result = enableNotificationsForCurrentSessionUseCase.execute(aDistributor)

        // Then
        result shouldBe EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.Success
        verify {
            fakeRegisterUnifiedPushUseCase.execute(aDistributor)
            fakeEnsureFcmTokenIsRetrievedUseCase.execute(fakePushersManager.instance, registerPusher = true)
        }
        coVerify {
            fakeToggleNotificationsForCurrentSessionUseCase.execute(enabled = true)
        }
    }

    @Test
    fun `given no existing pusher and a no registered distributor when execute then result is need to ask user for distributor`() = runTest {
        // Given
        val aDistributor = "distributor"
        fakePushersManager.givenGetPusherForCurrentSessionReturns(null)
        every { fakeRegisterUnifiedPushUseCase.execute(any()) } returns RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.NeedToAskUserForDistributor

        // When
        val result = enableNotificationsForCurrentSessionUseCase.execute(aDistributor)

        // Then
        result shouldBe EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.NeedToAskUserForDistributor
        verify {
            fakeRegisterUnifiedPushUseCase.execute(aDistributor)
        }
    }
}
