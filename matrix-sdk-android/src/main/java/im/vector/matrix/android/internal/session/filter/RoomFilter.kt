/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents "RoomFilter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
data class RoomFilter(
        @Json(name = "not_rooms") var notRooms: List<String>? = null,
        @Json(name = "rooms") var rooms: List<String>? = null,
        @Json(name = "ephemeral") var ephemeral: RoomEventFilter? = null,
        @Json(name = "include_leave") var includeLeave: Boolean? = null,
        @Json(name = "state") var state: RoomEventFilter? = null,
        @Json(name = "timeline") var timeline: RoomEventFilter? = null,
        @Json(name = "account_data") var accountData: RoomEventFilter? = null
) {

    fun hasData(): Boolean {
        return (notRooms != null
                || rooms != null
                || ephemeral != null
                || includeLeave != null
                || state != null
                || timeline != null
                || accountData != null)
    }

}
