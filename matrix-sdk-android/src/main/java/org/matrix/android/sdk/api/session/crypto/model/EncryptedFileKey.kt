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
     * Check what the spec tells us
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
