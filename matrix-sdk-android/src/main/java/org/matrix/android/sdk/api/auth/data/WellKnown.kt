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
