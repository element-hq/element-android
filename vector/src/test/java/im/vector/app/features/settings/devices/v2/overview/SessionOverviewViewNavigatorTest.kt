/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import im.vector.app.features.settings.devices.v2.details.SessionDetailsActivity
import im.vector.app.features.settings.devices.v2.rename.RenameSessionActivity
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
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
        context.verifyStartActivity(intent)
    }

    @Test
    fun `given a session id when navigating to rename screen then it starts the correct activity`() {
        // Given
        val intent = givenIntentForRenameSession(A_SESSION_ID)
        context.givenStartActivity(intent)

        // When
        sessionOverviewViewNavigator.goToRenameSession(context.instance, A_SESSION_ID)

        // Then
        context.verifyStartActivity(intent)
    }

    @Test
    fun `given an activity when going back then the activity is finished`() {
        // Given
        val fragmentActivity = mockk<FragmentActivity>()
        every { fragmentActivity.finish() } just runs

        // When
        sessionOverviewViewNavigator.goBack(fragmentActivity)

        // Then
        verify {
            fragmentActivity.finish()
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
