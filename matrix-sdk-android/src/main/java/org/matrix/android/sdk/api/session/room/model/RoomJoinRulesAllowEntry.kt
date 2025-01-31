/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomJoinRulesAllowEntry(
        /**
         * The room ID to check the membership of.
         */
        @Json(name = "room_id") val roomId: String?,
        /**
         * "m.room_membership" to describe that we are allowing access via room membership. Future MSCs may define other types.
         */
        @Json(name = "type") val type: String?
) {
    companion object {
        fun restrictedToRoom(roomId: String) = RoomJoinRulesAllowEntry(roomId, "m.room_membership")
    }
}
