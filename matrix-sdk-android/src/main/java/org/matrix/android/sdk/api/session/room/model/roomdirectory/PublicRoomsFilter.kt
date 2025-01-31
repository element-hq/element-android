/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model.roomdirectory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class to define a filter to retrieve public rooms.
 */
@JsonClass(generateAdapter = true)
data class PublicRoomsFilter(
        /**
         * A string to search for in the room metadata, e.g. name, topic, canonical alias etc. (Optional).
         */
        @Json(name = "generic_search_term")
        val searchTerm: String? = null
)
