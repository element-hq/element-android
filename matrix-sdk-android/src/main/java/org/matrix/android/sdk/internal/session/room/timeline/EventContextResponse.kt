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

package org.matrix.android.sdk.internal.session.room.timeline

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

@JsonClass(generateAdapter = true)
internal data class EventContextResponse(
        /**
         * Details of the requested event.
         */
        @Json(name = "event") val event: Event,
        /**
         * A token that can be used to paginate backwards with.
         */
        @Json(name = "start") override val start: String? = null,
        /**
         * A list of room events that happened just before the requested event, in reverse-chronological order.
         */
        @Json(name = "events_before") val eventsBefore: List<Event>? = null,
        /**
         * A list of room events that happened just after the requested event, in chronological order.
         */
        @Json(name = "events_after") val eventsAfter: List<Event>? = null,
        /**
         * A token that can be used to paginate forwards with.
         */
        @Json(name = "end") override val end: String? = null,
        /**
         * The state of the room at the last event returned.
         */
        @Json(name = "state") override val stateEvents: List<Event>? = null
) : TokenChunkEvent {

    override val events: List<Event> by lazy {
        eventsAfter.orEmpty().reversed() + event + eventsBefore.orEmpty()
    }
}
