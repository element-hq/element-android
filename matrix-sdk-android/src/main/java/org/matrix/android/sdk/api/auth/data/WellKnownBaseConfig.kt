/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
 * <pre>
 * {
 *     "base_url": "https://vector.im"
 * }
 * </pre>
 * .
 */
@JsonClass(generateAdapter = true)
data class WellKnownBaseConfig(
        @Json(name = "base_url")
        val baseURL: String? = null
)
