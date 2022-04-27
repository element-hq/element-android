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
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.internal.crypto.model.CryptoInfoMapper

@JsonClass(generateAdapter = true)
internal data class RestKeyInfo(
        /**
         * The user who owns the key
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * Allowed uses for the key.
         * Must contain "master" for master keys, "self_signing" for self-signing keys, and "user_signing" for user-signing keys.
         * See CrossSigningKeyInfo#KEY_USAGE_* constants
         */
        @Json(name = "usage")
        val usages: List<String>?,

        /**
         * An object that must have one entry,
         * whose name is "ed25519:" followed by the unpadded base64 encoding of the public key,
         * and whose value is the unpadded base64 encoding of the public key.
         */
        @Json(name = "keys")
        val keys: Map<String, String>?,

        /**
         *  Signatures of the key.
         *  A self-signing or user-signing key must be signed by the master key.
         *  A master key may be signed by a device.
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>? = null
) {
    fun toCryptoModel(): CryptoCrossSigningKey {
        return CryptoInfoMapper.map(this)
    }
}
