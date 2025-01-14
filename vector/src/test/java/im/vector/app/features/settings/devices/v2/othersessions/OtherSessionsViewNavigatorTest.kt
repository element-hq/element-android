/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.content.Intent
import im.vector.app.features.settings.devices.v2.overview.SessionOverviewActivity
import im.vector.app.test.fakes.FakeContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_DEVICE_ID = "A_DEVICE_ID"

class OtherSessionsViewNavigatorTest {

    private val context = FakeContext()
    private val otherSessionsViewNavigator = OtherSessionsViewNavigator()

    @Before
    fun setUp() {
        mockkObject(SessionOverviewActivity)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a device id when navigating to overview then it starts the correct activity`() {
        // Given
        val intent = givenIntentForDeviceOverview(A_DEVICE_ID)
        context.givenStartActivity(intent)

        // When
        otherSessionsViewNavigator.navigateToSessionOverview(context.instance, A_DEVICE_ID)

        // Then
        context.verifyStartActivity(intent)
    }

    private fun givenIntentForDeviceOverview(deviceId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionOverviewActivity.newIntent(context.instance, deviceId) } returns intent
        return intent
    }
}
