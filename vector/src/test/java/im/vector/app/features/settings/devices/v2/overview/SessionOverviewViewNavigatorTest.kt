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

package im.vector.app.features.settings.devices.v2.overview

import android.content.Intent
import im.vector.app.features.settings.devices.v2.details.SessionDetailsActivity
import im.vector.app.features.settings.devices.v2.rename.RenameSessionActivity
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_SESSION_ID = "session_id"

class SessionOverviewViewNavigatorTest {

    private val context = FakeContext()
    private val sessionOverviewViewNavigator = SessionOverviewViewNavigator()

    @Before
    fun setUp() {
        mockkObject(SessionDetailsActivity)
        mockkObject(RenameSessionActivity)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a session id when navigating to details then it starts the correct activity`() {
        // Given
        val intent = givenIntentForSessionDetails(A_SESSION_ID)
        context.givenStartActivity(intent)

        // When
        sessionOverviewViewNavigator.goToSessionDetails(context.instance, A_SESSION_ID)

        // Then
        verify {
            context.instance.startActivity(intent)
        }
    }

    @Test
    fun `given a session id when navigating to rename screen then it starts the correct activity`() {
        // Given
        val intent = givenIntentForRenameSession(A_SESSION_ID)
        context.givenStartActivity(intent)

        // When
        sessionOverviewViewNavigator.goToRenameSession(context.instance, A_SESSION_ID)

        // Then
        verify {
            context.instance.startActivity(intent)
        }
    }

    private fun givenIntentForSessionDetails(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionDetailsActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }

    private fun givenIntentForRenameSession(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { RenameSessionActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }
}
