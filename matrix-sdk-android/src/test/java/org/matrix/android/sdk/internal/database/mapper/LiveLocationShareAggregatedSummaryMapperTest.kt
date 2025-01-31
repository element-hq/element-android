/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import com.squareup.moshi.Moshi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity

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
                userId = ANY_USER_ID,
                isActive = ANY_ACTIVE_STATE,
                endOfLiveTimestampMillis = ANY_TIMEOUT,
                lastLocationDataContent = MessageBeaconLocationDataContent(locationInfo = A_LOCATION_INFO)
        )
    }

    private fun anEntity(content: MessageBeaconLocationDataContent) = LiveLocationShareAggregatedSummaryEntity(
            userId = ANY_USER_ID,
            isActive = ANY_ACTIVE_STATE,
            endOfLiveTimestampMillis = ANY_TIMEOUT,
            lastLocationContent = Moshi.Builder().build().adapter(MessageBeaconLocationDataContent::class.java).toJson(content)
    )
}
