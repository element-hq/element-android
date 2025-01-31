/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model.tombstone

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class to contains Tombstone information.
 */
@JsonClass(generateAdapter = true)
data class RoomTombstoneContent(
        /**
         * Required. A server-defined message.
         */
        @Json(name = "body") val body: String? = null,

        /**
         * Required. The new room the client should be visiting.
         */
        @Json(name = "replacement_room") val replacementRoomId: String?
)
