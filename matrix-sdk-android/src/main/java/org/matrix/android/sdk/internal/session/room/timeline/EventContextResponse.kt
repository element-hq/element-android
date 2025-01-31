/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
