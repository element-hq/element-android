/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.session.room.model.roomdirectory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the public rooms request response
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
