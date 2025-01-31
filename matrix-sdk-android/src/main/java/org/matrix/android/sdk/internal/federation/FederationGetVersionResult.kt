/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.federation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://matrix.org/docs/spec/server_server/latest#get-matrix-federation-v1-version
 */
@JsonClass(generateAdapter = true)
internal data class FederationGetVersionResult(
        @Json(name = "server")
        val server: FederationGetVersionServer?
)

@JsonClass(generateAdapter = true)
internal data class FederationGetVersionServer(
        /**
         * Arbitrary name that identify this implementation.
         */
        @Json(name = "name")
        val name: String?,
        /**
         * Version of this implementation. The version format depends on the implementation.
         */
        @Json(name = "version")
        val version: String?
)
