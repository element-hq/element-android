/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model.roomdirectory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the public rooms request response.
 */
@JsonClass(generateAdapter = true)
data class PublicRoomsResponse(
        /**
         * A pagination token for the response. The absence of this token means there are no more results to fetch and the client should stop paginating.
         */
        @Json(name = "next_batch")
        val nextBatch: String? = null,

        /**
         * A pagination token that allows fetching previous results. The absence of this token means there are no results before this batch,
         * i.e. this is the first batch.
         */
        @Json(name = "prev_batch")
        val prevBatch: String? = null,

        /**
         * A paginated chunk of public rooms.
         */
        @Json(name = "chunk")
        val chunk: List<PublicRoom>? = null,

        /**
         * An estimate on the total number of public rooms, if the server has an estimate.
         */
        @Json(name = "total_room_count_estimate")
        val totalRoomCountEstimate: Int? = null
)
