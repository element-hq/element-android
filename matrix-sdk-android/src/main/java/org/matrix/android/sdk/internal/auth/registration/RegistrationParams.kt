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
