/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
