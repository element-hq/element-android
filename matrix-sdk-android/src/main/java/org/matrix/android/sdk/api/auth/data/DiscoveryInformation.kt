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
 * This is a light version of Wellknown model, used for login response
 * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-login
 */
@JsonClass(generateAdapter = true)
data class DiscoveryInformation(
        /**
         * Required. Used by clients to discover homeserver information.
         */
        @Json(name = "m.homeserver")
        val homeServer: WellKnownBaseConfig? = null,

        /**
         * Used by clients to discover identity server information.
         * Note: matrix.org does not send this field
         */
        @Json(name = "m.identity_server")
        val identityServer: WellKnownBaseConfig? = null
)
