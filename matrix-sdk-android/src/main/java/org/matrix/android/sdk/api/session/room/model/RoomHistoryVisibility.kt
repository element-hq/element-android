/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#room-history-visibility
 */
@JsonClass(generateAdapter = false)
enum class RoomHistoryVisibility(val value: String) {
    /**
     * All events while this is the m.room.history_visibility value may be shared by any
     * participating homeserver with anyone, regardless of whether they have ever joined the room.
     */
    @Json(name = "world_readable") WORLD_READABLE("world_readable"),

    /**
     * Previous events are always accessible to newly joined members. All events in the
     * room are accessible, even those sent when the member was not a part of the room.
     */
    @Json(name = "shared") SHARED("shared"),

    /**
     * Events are accessible to newly joined members from the point they were invited onwards.
     * Events stop being accessible when the member's state changes to something other than invite or join.
     */
    @Json(name = "invited") INVITED("invited"),

    /**
     * Events are accessible to newly joined members from the point they joined the room onwards.
     * Events stop being accessible when the member's state changes to something other than join.
     */
    @Json(name = "joined") JOINED("joined")
}

/**
 * Room history should be shared only if room visibility is world_readable or shared.
 */
internal fun RoomHistoryVisibility.shouldShareHistory() =
        this == RoomHistoryVisibility.WORLD_READABLE || this == RoomHistoryVisibility.SHARED
