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
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * In Matrix specs: EncryptedFile
 */
@JsonClass(generateAdapter = true)
data class EncryptedFileInfo(
        /**
         * Required. The URL to the file.
         */
        @Json(name = "url")
        val url: String? = null,

        /**
         * Required. A JSON Web Key object.
         */
        @Json(name = "key")
        val key: EncryptedFileKey? = null,

        /**
         * Required. The Initialisation Vector used by AES-CTR, encoded as unpadded base64.
         */
        @Json(name = "iv")
        val iv: String? = null,

        /**
         * Required. A map from an algorithm name to a hash of the ciphertext, encoded as unpadded base64.
         * Clients should support the SHA-256 hash, which uses the key "sha256".
         */
        @Json(name = "hashes")
        val hashes: Map<String, String>? = null,

        /**
         * Required. Version of the encrypted attachments protocol. Must be "v2".
         */
        @Json(name = "v")
        val v: String? = null
) {
    /**
     * Check what the spec tells us
     */
    fun isValid(): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }

        if (key?.isValid() != true) {
            return false
        }

        if (iv.isNullOrBlank()) {
            return false
        }

        if (hashes?.containsKey("sha256") != true) {
            return false
        }

        if (v != "v2") {
            return false
        }

        return true
    }
}
