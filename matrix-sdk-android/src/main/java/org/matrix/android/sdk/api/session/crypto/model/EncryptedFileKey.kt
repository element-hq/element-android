/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EncryptedFileKey(
        /**
         * Required. Algorithm. Must be "A256CTR".
         */
        @Json(name = "alg")
        val alg: String? = null,

        /**
         * Required. Extractable. Must be true. This is a W3C extension.
         */
        @Json(name = "ext")
        val ext: Boolean? = null,

        /**
         * Required. Key operations. Must at least contain "encrypt" and "decrypt".
         */
        @Json(name = "key_ops")
        val keyOps: List<String>? = null,

        /**
         * Required. Key type. Must be "oct".
         */
        @Json(name = "kty")
        val kty: String? = null,

        /**
         * Required. The key, encoded as urlsafe unpadded base64.
         */
        @Json(name = "k")
        val k: String? = null
) {
    /**
     * Check what the spec tells us.
     */
    fun isValid(): Boolean {
        if (alg != "A256CTR") {
            return false
        }

        if (ext != true) {
            return false
        }

        if (keyOps?.contains("encrypt") != true || !keyOps.contains("decrypt")) {
            return false
        }

        if (kty != "oct") {
            return false
        }

        if (k.isNullOrBlank()) {
            return false
        }

        return true
    }
}
