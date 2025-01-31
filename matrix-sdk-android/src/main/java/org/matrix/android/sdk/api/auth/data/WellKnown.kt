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
import org.matrix.android.sdk.api.util.JsonDict

/**
 * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
 * <pre>
 * {
 *     "m.homeserver": {
 *         "base_url": "https://matrix.org"
 *     },
 *     "m.identity_server": {
 *         "base_url": "https://vector.im"
 *     }
 *     "m.integrations": {
 *          "managers": [
 *              {
 *                  "api_url": "https://integrations.example.org",
 *                  "ui_url": "https://integrations.example.org/ui"
 *              },
 *              {
 *                  "api_url": "https://bots.example.org"
 *              }
 *          ]
 *    }
 * }
 * </pre>
 * .
 */
@JsonClass(generateAdapter = true)
data class WellKnown(
        @Json(name = "m.homeserver")
        val homeServer: WellKnownBaseConfig? = null,

        @Json(name = "m.identity_server")
        val identityServer: WellKnownBaseConfig? = null,

        @Json(name = "m.integrations")
        val integrations: JsonDict? = null
)
