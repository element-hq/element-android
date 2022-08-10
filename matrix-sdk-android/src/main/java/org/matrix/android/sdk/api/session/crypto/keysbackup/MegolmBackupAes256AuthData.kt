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
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Data model for [org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysAlgorithmAndData.authData] in case
 * of [org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_AES_256_BACKUP].
 */
@JsonClass(generateAdapter = true)
data class MegolmBackupAes256AuthData(

        /**
         * The identity vector used to encrypt the backups.
         */
        @Json(name = "iv")
        val iv: String? = null,

        /**
         * The mac used to encrypt the backups.
         */
        @Json(name = "mac")
        val mac: String? = null,

        /**
         * In case of a backup created from a password, the salt associated with the backup
         * private key.
         */
        @Json(name = "private_key_salt")
        override val privateKeySalt: String? = null,

        /**
         * In case of a backup created from a password, the number of key derivations.
         */
        @Json(name = "private_key_iterations")
        override val privateKeyIterations: Int? = null,

        /**
         * Signatures of the public key.
         * userId -> (deviceSignKeyId -> signature)
         */
        @Json(name = "signatures")
        override val signatures: Map<String, Map<String, String>>? = null

) : MegolmBackupAuthData {

    override fun isValid(): Boolean = !(iv.isNullOrEmpty() || mac.isNullOrEmpty())

    override fun copy(newSignatures: Map<String, Map<String, String>>?): MegolmBackupAuthData {
        return copy(signatures = newSignatures)
    }

    override fun toJsonDict(): JsonDict {
        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(Map::class.java)

        return moshi
                .adapter(MegolmBackupAes256AuthData::class.java)
                .toJson(this)
                .let {
                    @Suppress("UNCHECKED_CAST")
                    adapter.fromJson(it) as JsonDict
                }
    }
}
