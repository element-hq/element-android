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
