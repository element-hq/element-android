/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This object is used to send a code received by SMS to validate Msisdn ownership.
 */
@JsonClass(generateAdapter = true)
internal data class ValidationCodeBody(
        @Json(name = "client_secret")
        val clientSecret: String,

        @Json(name = "sid")
        val sid: String,

        @Json(name = "token")
        val code: String
)
