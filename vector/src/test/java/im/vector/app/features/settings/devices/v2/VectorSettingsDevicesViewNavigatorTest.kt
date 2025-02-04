/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.content.Intent
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.othersessions.OtherSessionsActivity
import im.vector.app.features.settings.devices.v2.overview.SessionOverviewActivity
import im.vector.app.features.settings.devices.v2.rename.RenameSessionActivity
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_SESSION_ID = "session_id"
private val A_DEFAULT_FILTER = DeviceManagerFilterType.INACTIVE

class VectorSettingsDevicesViewNavigatorTest {

    private val context = FakeContext()
    private val vectorSettingsDevicesViewNavigator = VectorSettingsDevicesViewNavigator()

    @Before
    fun setUp() {
        mockkObject(SessionOverviewActivity.Companion)
        mockkObject(OtherSessionsActivity.Companion)
        mockkObject(RenameSessionActivity.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a session id when navigating to overview then it starts the correct activity`() {
        // Given
        val intent = givenIntentForSessionOverview(A_SESSION_ID)
        context.givenStartActivity(intent)

        // When
        vectorSettingsDevicesViewNavigator.navigateToSessionOverview(context.instance, A_SESSION_ID)

        // Then
        context.verifyStartActivity(intent)
    }

    @Test
    fun `given an intent when navigating to other sessions list then it starts the correct activity`() {
        // Given
        val intent = givenIntentForOtherSessions(A_DEFAULT_FILTER, true)
        context.givenStartActivity(intent)

        // When
        vectorSettingsDevicesViewNavigator.navigateToOtherSessions(context.instance, A_DEFAULT_FILTER, true)

        // Then
        context.verifyStartActivity(intent)
    }

    @Test
    fun `given an intent when navigating to rename session screen then it starts the correct activity`() {
        // Given
        val intent = givenIntentForRenameSession(A_SESSION_ID)
        context.givenStartActivity(intent)

        // When
        vectorSettingsDevicesViewNavigator.navigateToRenameSession(context.instance, A_SESSION_ID)

        // Then
        context.verifyStartActivity(intent)
    }

    private fun givenIntentForSessionOverview(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionOverviewActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }

    private fun givenIntentForOtherSessions(defaultFilter: DeviceManagerFilterType, excludeCurrentDevice: Boolean): Intent {
        val intent = mockk<Intent>()
        every { OtherSessionsActivity.newIntent(context.instance, defaultFilter, excludeCurrentDevice) } returns intent
        return intent
    }

    private fun givenIntentForRenameSession(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { RenameSessionActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }
}
