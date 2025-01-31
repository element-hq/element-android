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
 * Class representing an users search response.
 */
@JsonClass(generateAdapter = true)
internal data class SearchUsersResponse(
        @Json(name = "limited") val limited: Boolean = false,
        @Json(name = "results") val users: List<SearchUser> = emptyList()
)
