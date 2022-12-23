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

package im.vector.app.core.notification

import im.vector.app.features.session.coroutineScope
import im.vector.app.test.fakes.FakeSession
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationsSettingUpdaterTest {

    private val fakeUpdateEnableNotificationsSettingOnChangeUseCase = mockk<UpdateEnableNotificationsSettingOnChangeUseCase>()

    private val notificationsSettingUpdater = NotificationsSettingUpdater(
            updateEnableNotificationsSettingOnChangeUseCase = fakeUpdateEnableNotificationsSettingOnChangeUseCase,
    )

    @Before
    fun setup() {
        mockkStatic("im.vector.app.features.session.SessionCoroutineScopesKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a session when calling onSessionStarted then update enable notification on change`() = runTest {
        // Given
        val aSession = FakeSession()
        every { aSession.coroutineScope } returns this
        coJustRun { fakeUpdateEnableNotificationsSettingOnChangeUseCase.execute(any()) }

        // When
        notificationsSettingUpdater.onSessionStarted(aSession)
        advanceUntilIdle()

        // Then
        coVerify { fakeUpdateEnableNotificationsSettingOnChangeUseCase.execute(aSession) }
    }
}
