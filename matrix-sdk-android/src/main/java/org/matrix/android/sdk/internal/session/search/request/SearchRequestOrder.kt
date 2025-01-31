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

/**
 * Represents the order in which to search for results.
 */
@JsonClass(generateAdapter = false)
internal enum class SearchRequestOrder {
    @Json(name = "rank") RANK,
    @Json(name = "recent") RECENT
}
