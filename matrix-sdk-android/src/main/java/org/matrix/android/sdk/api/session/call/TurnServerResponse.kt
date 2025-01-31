/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// TODO Should not be exposed
/**
 * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-client-r0-voip-turnserver
 */
@JsonClass(generateAdapter = true)
data class TurnServerResponse(
        /**
         * Required. The username to use.
         */
        @Json(name = "username") val username: String?,

        /**
         * Required. The password to use.
         */
        @Json(name = "password") val password: String?,

        /**
         * Required. A list of TURN URIs
         */
        @Json(name = "uris") val uris: List<String>?,

        /**
         * Required. The time-to-live in seconds
         */
        @Json(name = "ttl") val ttl: Int?
)
