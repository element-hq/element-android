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

// RoomsSyncResponse represents the rooms list in server sync v2 response.
@JsonClass(generateAdapter = true)
data class RoomsSyncResponse(
        /**
         * Joined rooms: keys are rooms ids.
         */
        @Json(name = "join") val join: Map<String, RoomSync> = emptyMap(),

        /**
         * Invitations. The rooms that the user has been invited to: keys are rooms ids.
         */
        @Json(name = "invite") val invite: Map<String, InvitedRoomSync> = emptyMap(),

        /**
         * Left rooms. The rooms that the user has left or been banned from: keys are rooms ids.
         */
        @Json(name = "leave") val leave: Map<String, RoomSync> = emptyMap()
)
