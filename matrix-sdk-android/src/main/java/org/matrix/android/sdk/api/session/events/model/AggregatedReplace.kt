/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Note that there can be multiple events with an m.replace relationship to a given event (for example, if an event is edited multiple times).
 * These should be aggregated by the homeserver.
 * https://spec.matrix.org/v1.4/client-server-api/#server-side-aggregation-of-mreplace-relationships
 *
 */
@JsonClass(generateAdapter = true)
data class AggregatedReplace(
        @Json(name = "event_id") val eventId: String? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
        @Json(name = "sender") val senderId: String? = null,
)
