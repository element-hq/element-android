/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UploadSigningKeysBody(
        @Json(name = "master_key")
        val masterKey: RestKeyInfo? = null,

        @Json(name = "self_signing_key")
        val selfSigningKey: RestKeyInfo? = null,

        @Json(name = "user_signing_key")
        val userSigningKey: RestKeyInfo? = null,

        @Json(name = "auth")
        val auth: Map<String, *>? = null
)
