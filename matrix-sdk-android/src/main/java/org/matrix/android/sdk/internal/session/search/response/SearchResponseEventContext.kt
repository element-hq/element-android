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

package org.matrix.android.sdk.internal.session.search.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.JsonDict

@JsonClass(generateAdapter = true)
internal data class SearchResponseEventContext(
        // Events just before the result.
        @Json(name = "events_before")
        val eventsBefore: List<Event>,
        // Events just after the result.
        @Json(name = "events_after")
        val eventsAfter: List<Event>,
        // Pagination token for the start of the chunk
        @Json(name = "start")
        val start: String? = null,
        // Pagination token for the end of the chunk
        @Json(name = "end")
        val end: String? = null,
        // The historic profile information of the users that sent the events returned. The string key is the user ID for which the profile belongs to.
        @Json(name = "profile_info")
        val profileInfo: Map<String, JsonDict>? = null
)
