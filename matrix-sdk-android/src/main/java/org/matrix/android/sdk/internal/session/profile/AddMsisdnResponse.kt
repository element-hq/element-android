/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AddMsisdnResponse(
        /**
         * Required. The session ID. Session IDs are opaque strings that must consist entirely of the characters [0-9a-zA-Z.=_-].
         * Their length must not exceed 255 characters and they must not be empty.
         */
        @Json(name = "sid")
        val sid: String,

        /**
         * An optional field containing a URL where the client must submit the validation token to, with identical parameters to the Identity
         * Service API's POST /validate/email/submitToken endpoint (without the requirement for an access token).
         * The homeserver must send this token to the user (if applicable), who should then be prompted to provide it to the client.
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
