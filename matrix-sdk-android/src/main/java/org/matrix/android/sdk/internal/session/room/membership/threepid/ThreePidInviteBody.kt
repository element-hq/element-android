/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.membership.threepid

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ThreePidInviteBody(
        /**
         * Required. The hostname+port of the identity server which should be used for third party identifier lookups.
         */
        @Json(name = "id_server")
        val idServer: String,
        /**
         * Required. An access token previously registered with the identity server. Servers can treat this as optional
         * to distinguish between r0.5-compatible clients and this specification version.
         */
        @Json(name = "id_access_token")
        val idAccessToken: String,
        /**
         * Required. The kind of address being passed in the address field, for example email.
         */
        @Json(name = "medium")
        val medium: String,
        /**
         * Required. The invitee's third party identifier.
         */
        @Json(name = "address")
        val address: String
)
