/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
