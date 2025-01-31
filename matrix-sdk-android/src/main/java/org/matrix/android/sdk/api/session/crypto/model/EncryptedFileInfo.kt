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

/**
 * In Matrix specs: EncryptedFile.
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
     * Check what the spec tells us.
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
