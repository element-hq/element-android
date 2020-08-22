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
internal data class ThirdPartyIdentifier(
        /**
         * Required. The medium of the third party identifier. One of: ["email", "msisdn"]
         */
        @Json(name = "medium")
        val medium: String? = null,

        /**
         * Required. The third party identifier address.
         */
        @Json(name = "address")
        val address: String? = null,

        /**
         * Required. The timestamp in milliseconds when this 3PID has been validated.
         * Define as Object because it should be Long and it is a Double.
         * So, it might change.
         */
        @Json(name = "validated_at")
        val validatedAt: Any? = null,

        /**
         * Required. The timestamp in milliseconds when this 3PID has been added to the user account.
         * Define as Object because it should be Long and it is a Double.
         * So, it might change.
         */
        @Json(name = "added_at")
        val addedAt: Any? = null
) {
    companion object {
        const val MEDIUM_EMAIL = "email"
        const val MEDIUM_MSISDN = "msisdn"

        val SUPPORTED_MEDIUM = listOf(MEDIUM_EMAIL, MEDIUM_MSISDN)
    }
}
