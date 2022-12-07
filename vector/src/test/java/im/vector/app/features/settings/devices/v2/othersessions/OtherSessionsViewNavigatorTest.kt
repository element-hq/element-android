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
