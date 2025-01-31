/*
 * Copyright 2022-2025 New Vector Ltd.
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
import io.mockk.verify
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
        val intent = givenIntentForDeviceOverview(A_DEVICE_ID)
        context.givenStartActivity(intent)

        otherSessionsViewNavigator.navigateToSessionOverview(context.instance, A_DEVICE_ID)

        verify {
            context.instance.startActivity(intent)
        }
    }

    private fun givenIntentForDeviceOverview(deviceId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionOverviewActivity.newIntent(context.instance, deviceId) } returns intent
        return intent
    }
}
