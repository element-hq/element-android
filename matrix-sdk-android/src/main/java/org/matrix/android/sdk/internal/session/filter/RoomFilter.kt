/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents "RoomFilter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class RoomFilter(
        /**
         * A list of room IDs to exclude. If this list is absent then no rooms are excluded.
         * A matching room will be excluded even if it is listed in the 'rooms' filter.
         * This filter is applied before the filters in ephemeral, state, timeline or account_data
         */
        @Json(name = "not_rooms") val notRooms: List<String>? = null,
        /**
         * A list of room IDs to include. If this list is absent then all rooms are included.
         * This filter is applied before the filters in ephemeral, state, timeline or account_data
         */
        @Json(name = "rooms") val rooms: List<String>? = null,
        /**
         * The events that aren't recorded in the room history, e.g. typing and receipts, to include for rooms.
         */
        @Json(name = "ephemeral") val ephemeral: RoomEventFilter? = null,
        /**
         * Include rooms that the user has left in the sync, default false
         */
        @Json(name = "include_leave") val includeLeave: Boolean? = null,
        /**
         * The state events to include for rooms.
         * Developer remark: StateFilter is exactly the same than RoomEventFilter
         */
        @Json(name = "state") val state: RoomEventFilter? = null,
        /**
         * The message and state update events to include for rooms.
         */
        @Json(name = "timeline") val timeline: RoomEventFilter? = null,
        /**
         * The per user account data to include for rooms.
         */
        @Json(name = "account_data") val accountData: RoomEventFilter? = null
) {

    fun hasData(): Boolean {
        return (notRooms != null ||
                rooms != null ||
                ephemeral != null ||
                includeLeave != null ||
                state != null ||
                timeline != null ||
                accountData != null)
    }
}
