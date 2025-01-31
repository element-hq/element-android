/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.content.Intent
import im.vector.app.features.settings.devices.v2.othersessions.OtherSessionsActivity
import im.vector.app.features.settings.devices.v2.overview.SessionOverviewActivity
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

class VectorSettingsDevicesViewNavigatorTest {

    private val context = FakeContext()
    private val vectorSettingsDevicesViewNavigator = VectorSettingsDevicesViewNavigator()

    @Before
    fun setUp() {
        mockkObject(SessionOverviewActivity.Companion)
        mockkObject(OtherSessionsActivity.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a session id when navigating to overview then it starts the correct activity`() {
        val intent = givenIntentForSessionOverview(A_SESSION_ID)
        context.givenStartActivity(intent)

        vectorSettingsDevicesViewNavigator.navigateToSessionOverview(context.instance, A_SESSION_ID)

        verify {
            context.instance.startActivity(intent)
        }
    }

    @Test
    fun `given an intent when navigating to other sessions list then it starts the correct activity`() {
        val intent = givenIntentForOtherSessions()
        context.givenStartActivity(intent)

        vectorSettingsDevicesViewNavigator.navigateToOtherSessions(context.instance)

        verify {
            context.instance.startActivity(intent)
        }
    }

    private fun givenIntentForSessionOverview(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionOverviewActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }

    private fun givenIntentForOtherSessions(): Intent {
        val intent = mockk<Intent>()
        every { OtherSessionsActivity.newIntent(context.instance) } returns intent
        return intent
    }
}
