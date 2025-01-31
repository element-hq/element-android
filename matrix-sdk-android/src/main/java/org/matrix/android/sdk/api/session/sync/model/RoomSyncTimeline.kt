/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

// RoomSyncTimeline represents the timeline of messages and state changes for a room during server sync v2.
@JsonClass(generateAdapter = true)
data class RoomSyncTimeline(

        /**
         * List of events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null,

        /**
         * Boolean which tells whether there are more events on the server.
         */
        @Json(name = "limited") val limited: Boolean = false,

        /**
         * If the batch was limited then this is a token that can be supplied to the server to retrieve more events.
         */
        @Json(name = "prev_batch") val prevToken: String? = null
)
