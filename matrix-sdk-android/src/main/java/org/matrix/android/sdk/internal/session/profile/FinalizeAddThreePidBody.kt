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
internal data class FinalizeAddThreePidBody(
        /**
         * Required. The client secret used in the session with the homeserver.
         */
        @Json(name = "client_secret")
        val clientSecret: String,

        /**
         * Required. The session identifier given by the homeserver.
         */
        @Json(name = "sid")
        val sid: String,

        /**
         * Additional authentication information for the user-interactive authentication API.
         */
        @Json(name = "auth")
        val auth: Map<String, *>? = null
)
