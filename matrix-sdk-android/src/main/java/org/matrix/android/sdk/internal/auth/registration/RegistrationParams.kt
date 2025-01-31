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
 * Class to pass parameters to the different registration types for /register.
 */
@JsonClass(generateAdapter = true)
internal data class RegistrationParams(
        // authentication parameters
        @Json(name = "auth")
        val auth: AuthParams? = null,

        // the account username
        @Json(name = "username")
        val username: String? = null,

        // the account password
        @Json(name = "password")
        val password: String? = null,

        // device name
        @Json(name = "initial_device_display_name")
        val initialDeviceDisplayName: String? = null,

        // Temporary flag to notify the server that we support msisdn flow. Used to prevent old app
        // versions to end up in fallback because the HS returns the msisdn flow which they don't support
        @Json(name = "x_show_msisdn")
        val xShowMsisdn: Boolean? = null
)
