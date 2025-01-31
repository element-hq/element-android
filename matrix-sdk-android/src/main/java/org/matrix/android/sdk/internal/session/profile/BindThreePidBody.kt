/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BindThreePidBody(
        /**
         * Required. The client secret used in the session with the identity server.
         */
        @Json(name = "client_secret")
        val clientSecret: String,

        /**
         * Required. The identity server to use. (without "https://")
         */
        @Json(name = "id_server")
        val identityServerUrlWithoutProtocol: String,

        /**
         * Required. An access token previously registered with the identity server.
         */
        @Json(name = "id_access_token")
        val identityServerAccessToken: String,

        /**
         * Required. The session identifier given by the identity server.
         */
        @Json(name = "sid")
        val sid: String
)
