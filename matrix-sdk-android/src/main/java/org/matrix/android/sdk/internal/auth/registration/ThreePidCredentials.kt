/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ThreePidCredentials(
        @Json(name = "client_secret")
        val clientSecret: String? = null,

        @Json(name = "id_server")
        val idServer: String? = null,

        @Json(name = "sid")
        val sid: String? = null
)
