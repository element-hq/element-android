/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.openid

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestOpenIdTokenResponse(
        /**
         * Required. An access token the consumer may use to verify the identity of the person who generated the token.
         * This is given to the federation API GET /openid/userinfo to verify the user's identity.
         */
        @Json(name = "access_token")
        val openIdToken: String,

        /**
         * Required. The string "Bearer".
         */
        @Json(name = "token_type")
        val tokenType: String,

        /**
         * Required. The homeserver domain the consumer should use when attempting to verify the user's identity.
         */
        @Json(name = "matrix_server_name")
        val matrixServerName: String,

        /**
         * Required. The number of seconds before this token expires and a new one must be generated.
         */
        @Json(name = "expires_in")
        val expiresIn: Int
)
