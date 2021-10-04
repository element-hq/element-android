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
