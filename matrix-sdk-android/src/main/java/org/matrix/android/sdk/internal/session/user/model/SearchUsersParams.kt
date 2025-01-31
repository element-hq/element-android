/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an user search parameters.
 */
@JsonClass(generateAdapter = true)
internal data class SearchUsersParams(
        // the searched term
        @Json(name = "search_term") val searchTerm: String,
        // set a limit to the request response
        @Json(name = "limit") val limit: Int
)
