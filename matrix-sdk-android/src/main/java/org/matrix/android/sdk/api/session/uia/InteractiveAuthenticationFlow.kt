/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.uia

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * An interactive authentication flow.
 */
@JsonClass(generateAdapter = true)
data class InteractiveAuthenticationFlow(

        @Json(name = "type")
        val type: String? = null,

        @Json(name = "stages")
        val stages: List<String>? = null
)
