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
internal data class SearchRequestEventContext(
        // How many events before the result are returned.
        @Json(name = "before_limit")
        val beforeLimit: Int? = null,
        // How many events after the result are returned.
        @Json(name = "after_limit")
        val afterLimit: Int? = null,
        // Requests that the server returns the historic profile information
        @Json(name = "include_profile")
        val includeProfile: Boolean? = null
)
