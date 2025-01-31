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
