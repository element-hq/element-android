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
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Represents "RoomEventFilter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
data class RoomEventFilter(
        /**
         * The maximum number of events to return.
         */
        @Json(name = "limit") val limit: Int? = null,
        /**
         * A list of sender IDs to exclude. If this list is absent then no senders are excluded. A matching sender will
         * be excluded even if it is listed in the 'senders' filter.
         */
        @Json(name = "not_senders") val notSenders: List<String>? = null,
        /**
         * A list of event types to exclude. If this list is absent then no event types are excluded. A matching type will
         * be excluded even if it is listed in the 'types' filter. A '*' can be used as a wildcard to match any sequence of characters.
         */
        @Json(name = "not_types") val notTypes: List<String>? = null,
        /**
         * A list of senders IDs to include. If this list is absent then all senders are included.
         */
        @Json(name = "senders") val senders: List<String>? = null,
        /**
         * A list of event types to include. If this list is absent then all event types are included. A '*' can be used as
         * a wildcard to match any sequence of characters.
         */
        @Json(name = "types") val types: List<String>? = null,
        /**
         * A list of relation types which must be exist pointing to the event being filtered.
         * If this list is absent then no filtering is done on relation types.
         */
        @Json(name = "related_by_rel_types") val relationTypes: List<String>? = null,
        /**
         *  A list of senders of relations which must exist pointing to the event being filtered.
         *  If this list is absent then no filtering is done on relation types.
         */
        @Json(name = "related_by_senders") val relationSenders: List<String>? = null,

        /**
         * A list of room IDs to include. If this list is absent then all rooms are included.
         */
        @Json(name = "rooms") val rooms: List<String>? = null,
        /**
         * A list of room IDs to exclude. If this list is absent then no rooms are excluded. A matching room will be excluded
         * even if it is listed in the 'rooms' filter.
         */
        @Json(name = "not_rooms") val notRooms: List<String>? = null,
        /**
         * If true, includes only events with a url key in their content. If false, excludes those events. If omitted, url
         * key is not considered for filtering.
         */
        @Json(name = "contains_url") val containsUrl: Boolean? = null,
        /**
         * If true, enables lazy-loading of membership events. See Lazy-loading room members for more information. Defaults to false.
         */
        @Json(name = "lazy_load_members") val lazyLoadMembers: Boolean? = null
) {

    fun toJSONString(): String {
        return MoshiProvider.providesMoshi().adapter(RoomEventFilter::class.java).toJson(this)
    }

    fun hasData(): Boolean {
        return (limit != null ||
                notSenders != null ||
                notTypes != null ||
                senders != null ||
                types != null ||
                rooms != null ||
                notRooms != null ||
                containsUrl != null ||
                lazyLoadMembers != null)
    }
}
