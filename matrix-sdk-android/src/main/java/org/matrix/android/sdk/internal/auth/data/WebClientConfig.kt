/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class WebClientConfig(
        /**
         * This is now deprecated, but still used first, rather than value from "default_server_config"
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
