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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.keysbackup.model.SignalableMegolmBackupAuthData
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Data model for [org.matrix.androidsdk.rest.model.keys.KeysAlgorithmAndData.authData] in case
 * of [org.matrix.androidsdk.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP].
 */
@JsonClass(generateAdapter = true)
data class MegolmBackupAuthData(
        /**
         * The curve25519 public key used to encrypt the backups.
         */
        @Json(name = "public_key")
        val publicKey: String,

        /**
         * In case of a backup created from a password, the salt associated with the backup
         * private key.
         */
        @Json(name = "private_key_salt")
        val privateKeySalt: String? = null,

        /**
         * In case of a backup created from a password, the number of key derivations.
         */
        @Json(name = "private_key_iterations")
        val privateKeyIterations: Int? = null,

        /**
         * Signatures of the public key.
         * userId -> (deviceSignKeyId -> signature)
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>? = null
) {

    internal fun toJsonDict(): JsonDict {
        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(Map::class.java)

        return moshi
                .adapter(MegolmBackupAuthData::class.java)
                .toJson(this)
                .let {
                    @Suppress("UNCHECKED_CAST")
                    adapter.fromJson(it) as JsonDict
                }
    }

    internal fun signalableJSONDictionary(): JsonDict {
        return SignalableMegolmBackupAuthData(
                publicKey = publicKey,
                privateKeySalt = privateKeySalt,
                privateKeyIterations = privateKeyIterations
        )
                .signalableJSONDictionary()
    }
}
