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

package im.vector.app.features.location.live.map

import android.graphics.drawable.Drawable
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.toLocationData
import im.vector.app.test.fakes.FakeLocationPinProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent

class UserLiveLocationViewStateMapperTest {

    private val locationPinProvider = FakeLocationPinProvider()

    private val userLiveLocationViewStateMapper = UserLiveLocationViewStateMapper(locationPinProvider.instance)

    @Before
    fun setUp() {
        mockkStatic("im.vector.app.features.location.LocationDataKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("im.vector.app.features.location.LocationDataKt")
    }

    @Test
    fun `given a summary with invalid data then result is null`() = runTest {
        val summary1 = LiveLocationShareAggregatedSummary(
                userId = null,
                isActive = true,
                endOfLiveTimestampMillis = null,
                lastLocationDataContent = null,
        )
        val summary2 = summary1.copy(userId = "")
        val summaryWithoutLocation = summary1.copy(userId = "userId")

        val viewState1 = userLiveLocationViewStateMapper.map(summary1)
        val viewState2 = userLiveLocationViewStateMapper.map(summary2)
        val viewState3 = userLiveLocationViewStateMapper.map(summaryWithoutLocation)

        assertEquals(null, viewState1)
        assertEquals(null, viewState2)
        assertEquals(null, viewState3)
    }

    @Test
    fun `given a summary with valid data then result is correctly mapped`() = runTest {
        val geoUri = "geoUri"
        val userId = "userId"
        val pinDrawable = mockk<Drawable>()
        val endOfLiveTimestampMillis = 123L

        val locationDataContent = MessageBeaconLocationDataContent(
                locationInfo = LocationInfo(geoUri = geoUri)
        )
        val summary = LiveLocationShareAggregatedSummary(
                userId = userId,
                isActive = true,
                endOfLiveTimestampMillis = endOfLiveTimestampMillis,
                lastLocationDataContent = locationDataContent,
        )
        val locationData = LocationData(
                latitude = 1.0,
                longitude = 2.0,
                uncertainty = null
        )
        every { geoUri.toLocationData() } returns locationData
        locationPinProvider.givenCreateForUserId(userId, pinDrawable)

        val viewState = userLiveLocationViewStateMapper.map(summary)

        val expectedViewState = UserLiveLocationViewState(
                userId = userId,
                pinDrawable = pinDrawable,
                locationData = locationData,
                endOfLiveTimestampMillis = endOfLiveTimestampMillis
        )
        assertEquals(expectedViewState, viewState)
    }
}
