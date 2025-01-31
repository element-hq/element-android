/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class WebClientConfig(
        /**
         * This is now deprecated, but still used first, rather than value from "default_server_config".
         */
        @Json(name = "default_hs_url")
        val defaultHomeServerUrl: String?,

        @Json(name = "default_server_config")
        val defaultServerConfig: WebClientConfigDefaultServerConfig?
) {
    fun getPreferredHomeServerUrl(): String? {
        return defaultHomeServerUrl
                ?.takeIf { it.isNotEmpty() }
                ?: defaultServerConfig?.homeServer?.baseURL
    }
}

@JsonClass(generateAdapter = true)
internal data class WebClientConfigDefaultServerConfig(
        @Json(name = "m.homeserver")
        val homeServer: WebClientConfigBaseConfig? = null
)

@JsonClass(generateAdapter = true)
internal data class WebClientConfigBaseConfig(
        @Json(name = "base_url")
        val baseURL: String? = null
)
