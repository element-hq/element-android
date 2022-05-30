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
import im.vector.app.test.fakes.FakeLocationPinProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent

private const val A_USER_ID = "aUserId"
private const val A_IS_ACTIVE = true
private const val A_END_OF_LIVE_TIMESTAMP = 123L
private const val A_LATITUDE = 40.05
private const val A_LONGITUDE = 29.24
private const val A_UNCERTAINTY = 30.0
private const val A_GEO_URI = "geo:$A_LATITUDE,$A_LONGITUDE;$A_UNCERTAINTY"

class UserLiveLocationViewStateMapperTest {

    private val locationPinProvider = FakeLocationPinProvider()

    private val userLiveLocationViewStateMapper = UserLiveLocationViewStateMapper(locationPinProvider.instance)

    @Test
    fun `given a summary with invalid data then result is null`() = runTest {
        val summary1 = LiveLocationShareAggregatedSummary(
                userId = null,
                isActive = true,
                endOfLiveTimestampMillis = null,
                lastLocationDataContent = null,
        )
        val summary2 = summary1.copy(userId = "")
        val summaryWithoutLocation = summary1.copy(userId = A_USER_ID)

        val viewState1 = userLiveLocationViewStateMapper.map(summary1)
        val viewState2 = userLiveLocationViewStateMapper.map(summary2)
        val viewState3 = userLiveLocationViewStateMapper.map(summaryWithoutLocation)

        viewState1 shouldBeEqualTo null
        viewState2 shouldBeEqualTo null
        viewState3 shouldBeEqualTo null
    }

    @Test
    fun `given a summary with valid data then result is correctly mapped`() = runTest {
        val pinDrawable = mockk<Drawable>()

        val locationDataContent = MessageBeaconLocationDataContent(
                locationInfo = LocationInfo(geoUri = A_GEO_URI)
        )
        val summary = LiveLocationShareAggregatedSummary(
                userId = A_USER_ID,
                isActive = A_IS_ACTIVE,
                endOfLiveTimestampMillis = A_END_OF_LIVE_TIMESTAMP,
                lastLocationDataContent = locationDataContent,
        )
        locationPinProvider.givenCreateForUserId(A_USER_ID, pinDrawable)

        val viewState = userLiveLocationViewStateMapper.map(summary)

        val expectedViewState = UserLiveLocationViewState(
                userId = A_USER_ID,
                pinDrawable = pinDrawable,
                locationData = LocationData(
                        latitude = A_LATITUDE,
                        longitude = A_LONGITUDE,
                        uncertainty = A_UNCERTAINTY
                ),
                endOfLiveTimestampMillis = A_END_OF_LIVE_TIMESTAMP
        )
        viewState shouldBeEqualTo expectedViewState
    }
}
