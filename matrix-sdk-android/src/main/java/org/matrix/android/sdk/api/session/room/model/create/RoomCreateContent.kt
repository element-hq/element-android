/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Content of a m.room.create type event.
 */
@JsonClass(generateAdapter = true)
data class RoomCreateContent(
        @Json(name = "creator") val creator: String? = null,
        @Json(name = "room_version") val roomVersion: String? = null,
        @Json(name = "predecessor") val predecessor: Predecessor? = null,
        // Defines the room type, see #RoomType (user extensible)
        @Json(name = "type") val type: String? = null
)
