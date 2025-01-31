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

// RoomSyncEphemeral represents the ephemeral events in the room that aren't recorded in the timeline or state of the room (e.g. typing).
@JsonClass(generateAdapter = true)
data class RoomSyncEphemeral(
        /**
         * List of ephemeral events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null
)
