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
 * A link to an old room in case of room versioning.
 */
@JsonClass(generateAdapter = true)
data class Predecessor(
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "event_id") val eventId: String? = null
)
