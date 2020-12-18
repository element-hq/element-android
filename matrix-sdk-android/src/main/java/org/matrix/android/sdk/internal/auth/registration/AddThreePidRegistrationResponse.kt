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

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AddThreePidRegistrationResponse(
        /**
         * Required. The session ID. Session IDs are opaque strings that must consist entirely of the characters [0-9a-zA-Z.=_-].
         * Their length must not exceed 255 characters and they must not be empty.
         */
        @Json(name = "sid")
        val sid: String,

        /**
         * An optional field containing a URL where the client must submit the validation token to, with identical parameters to the Identity
         * Service API's POST /validate/email/submitToken endpoint. The homeserver must send this token to the user (if applicable),
         * who should then be prompted to provide it to the client.
         *
         * If this field is not present, the client can assume that verification will happen without the client's involvement provided
         * the homeserver advertises this specification version in the /versions response (ie: r0.5.0).
         */
        @Json(name = "submit_url")
        val submitUrl: String? = null,

        /* ==========================================================================================
         * It seems that the homeserver is sending more data, we may need it
         * ========================================================================================== */

        @Json(name = "msisdn")
        val msisdn: String? = null,

        @Json(name = "intl_fmt")
        val formattedMsisdn: String? = null,

        @Json(name = "success")
        val success: Boolean? = null
)
