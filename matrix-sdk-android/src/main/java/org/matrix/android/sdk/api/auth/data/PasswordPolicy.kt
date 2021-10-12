/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PasswordPolicy(
        /**
         * Minimum accepted length for a password.
         */
        @Json(name = "m.minimum_length")
        val minLength: Int? = null,

        /**
         * Wether the password must contain at least one symbol.
         */
        @Json(name = "m.require_symbol")
        val requireSymbol: Boolean? = false,

        /**
         * Wether the password must contain at least one digit.
         */
        @Json(name = "m.require_digit")
        val requireDigit: Boolean? = false,

        /**
         * Wether the password must contain at least one lowercase letter.
         */
        @Json(name = "m.require_lowercase")
        val requireLowercase: Boolean? = false,

        /**
         * Wether the password must contain at least one uppercase letter.
         */
        @Json(name = "m.require_uppercase")
        val requireUppercase: Boolean? = false
)
