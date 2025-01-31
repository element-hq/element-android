/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.content.Intent
import im.vector.app.features.settings.devices.v2.details.SessionDetailsActivity
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
        sessionOverviewViewNavigator.navigateToSessionDetails(context.instance, A_SESSION_ID)

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
}
