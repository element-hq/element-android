/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import com.squareup.moshi.Moshi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity

private const val ANY_ROOM_ID = "a-room-id"
private const val ANY_USER_ID = "a-user-id"
private const val ANY_ACTIVE_STATE = true
private const val ANY_TIMEOUT = 123L
private val A_LOCATION_INFO = LocationInfo("a-geo-uri")

class LiveLocationShareAggregatedSummaryMapperTest {

    private val mapper = LiveLocationShareAggregatedSummaryMapper()

    @Test
    fun `given an entity then result should be mapped correctly`() {
        val entity = anEntity(content = MessageBeaconLocationDataContent(locationInfo = A_LOCATION_INFO))

        val summary = mapper.map(entity)

        summary shouldBeEqualTo LiveLocationShareAggregatedSummary(
                roomId = ANY_ROOM_ID,
                userId = ANY_USER_ID,
                isActive = ANY_ACTIVE_STATE,
                endOfLiveTimestampMillis = ANY_TIMEOUT,
                lastLocationDataContent = MessageBeaconLocationDataContent(locationInfo = A_LOCATION_INFO)
        )
    }

    private fun anEntity(content: MessageBeaconLocationDataContent) = LiveLocationShareAggregatedSummaryEntity(
            roomId = ANY_ROOM_ID,
            userId = ANY_USER_ID,
            isActive = ANY_ACTIVE_STATE,
            endOfLiveTimestampMillis = ANY_TIMEOUT,
            lastLocationContent = Moshi.Builder().build().adapter(MessageBeaconLocationDataContent::class.java).toJson(content)
    )
}
