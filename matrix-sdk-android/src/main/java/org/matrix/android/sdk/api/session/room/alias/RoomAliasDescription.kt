/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.alias

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomAliasDescription(
        /**
         * The room ID for this alias.
         */
        @Json(name = "room_id") val roomId: String,

        /**
         * A list of servers that are aware of this room ID.
         */
        @Json(name = "servers") val servers: List<String> = emptyList()
)
