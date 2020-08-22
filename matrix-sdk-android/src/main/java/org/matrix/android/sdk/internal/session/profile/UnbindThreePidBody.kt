/*
 * Copyright (c) 2020 New Vector Ltd
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
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UnbindThreePidBody(
        /**
         * The identity server to unbind from. If not provided, the homeserver MUST use the id_server the identifier was added through.
         * If the homeserver does not know the original id_server, it MUST return a id_server_unbind_result of no-support.
         */
        @Json(name = "id_server")
        val identityServerUrlWithoutProtocol: String?,

        /**
         * Required. The medium of the third party identifier being removed. One of: ["email", "msisdn"]
         */
        @Json(name = "medium")
        val medium: String,

        /**
         * Required. The third party address being removed.
         */
        @Json(name = "address")
        val address: String
)
