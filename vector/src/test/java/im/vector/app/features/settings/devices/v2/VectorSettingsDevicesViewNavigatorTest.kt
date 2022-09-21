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

package im.vector.app.features.settings.devices.v2

import android.content.Intent
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
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
private const val A_TITLE_RESOURCE_ID = 1234
private val A_DEFAULT_FILTER = DeviceManagerFilterType.INACTIVE

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
        val intent = givenIntentForOtherSessions(A_TITLE_RESOURCE_ID, A_DEFAULT_FILTER, true)
        context.givenStartActivity(intent)

        vectorSettingsDevicesViewNavigator.navigateToOtherSessions(context.instance, A_TITLE_RESOURCE_ID, A_DEFAULT_FILTER, true)

        verify {
            context.instance.startActivity(intent)
        }
    }

    private fun givenIntentForSessionOverview(sessionId: String): Intent {
        val intent = mockk<Intent>()
        every { SessionOverviewActivity.newIntent(context.instance, sessionId) } returns intent
        return intent
    }

    private fun givenIntentForOtherSessions(titleResourceId: Int, defaultFilter: DeviceManagerFilterType, excludeCurrentDevice: Boolean): Intent {
        val intent = mockk<Intent>()
        every { OtherSessionsActivity.newIntent(context.instance, titleResourceId, defaultFilter, excludeCurrentDevice) } returns intent
        return intent
    }
}
