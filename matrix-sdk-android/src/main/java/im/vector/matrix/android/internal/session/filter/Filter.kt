/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package im.vector.matrix.android.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents "Filter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
data class Filter(
        @Json(name = "limit") var limit: Int? = null,
        @Json(name = "senders") var senders: MutableList<String>? = null,
        @Json(name = "not_senders") var notSenders: MutableList<String>? = null,
        @Json(name = "types") var types: MutableList<String>? = null,
        @Json(name = "not_types") var notTypes: MutableList<String>? = null,
        @Json(name = "rooms") var rooms: MutableList<String>? = null,
        @Json(name = "not_rooms") var notRooms: MutableList<String>? = null
) {
    fun hasData(): Boolean {
        return (limit != null
                || senders != null
                || notSenders != null
                || types != null
                || notTypes != null
                || rooms != null
                || notRooms != null)
    }
}
