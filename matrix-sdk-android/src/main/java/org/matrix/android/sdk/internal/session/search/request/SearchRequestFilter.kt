/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SearchRequestFilter(
        // The maximum number of events to return.
        @Json(name = "limit")
        val limit: Int? = null,
        // A list of room IDs to include. If this list is absent then all rooms are included.
        @Json(name = "rooms")
        val rooms: List<String>? = null
)
