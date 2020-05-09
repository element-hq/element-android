/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.identity.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Just to consider common parameters
private interface IdentityRequestTokenBody {
    val clientSecret: String
    val sendAttempt: Int
}

@JsonClass(generateAdapter = true)
internal data class IdentityRequestTokenForEmailBody(
        @Json(name = "client_secret")
        override val clientSecret: String,

        @Json(name = "send_attempt")
        override val sendAttempt: Int,

        @Json(name = "email")
        val email: String
) : IdentityRequestTokenBody

@JsonClass(generateAdapter = true)
internal data class IdentityRequestTokenForMsisdnBody(
        @Json(name = "client_secret")
        override val clientSecret: String,

        @Json(name = "send_attempt")
        override val sendAttempt: Int,

        @Json(name = "phone_number")
        val phoneNumber: String,

        @Json(name = "country")
        val countryCode: String?
) : IdentityRequestTokenBody
