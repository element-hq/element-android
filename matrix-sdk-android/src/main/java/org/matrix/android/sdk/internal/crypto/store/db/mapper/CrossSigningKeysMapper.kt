/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.mapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.realm.RealmList
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntity
import timber.log.Timber
import javax.inject.Inject

internal class CrossSigningKeysMapper @Inject constructor(moshi: Moshi) {

    private val signaturesAdapter = moshi.adapter<Map<String, Map<String, String>>>(
            Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
            )
    )

    fun update(keyInfo: KeyInfoEntity, cryptoCrossSigningKey: CryptoCrossSigningKey) {
        // update signatures?
        keyInfo.signatures = serializeSignatures(cryptoCrossSigningKey.signatures)
        keyInfo.usages = cryptoCrossSigningKey.usages?.toTypedArray()?.let { RealmList(*it) }
                ?: RealmList()
    }

    fun map(userId: String?, keyInfo: KeyInfoEntity?): CryptoCrossSigningKey? {
        val pubKey = keyInfo?.publicKeyBase64 ?: return null
        return CryptoCrossSigningKey(
                userId = userId ?: "",
                keys = mapOf("ed25519:$pubKey" to pubKey),
                usages = keyInfo.usages.toList(),
                signatures = deserializeSignatures(keyInfo.signatures),
                trustLevel = keyInfo.trustLevelEntity?.let {
                    DeviceTrustLevel(
                            crossSigningVerified = it.crossSignedVerified ?: false,
                            locallyVerified = it.locallyVerified ?: false
                    )
                }
        )
    }

    fun map(keyInfo: CryptoCrossSigningKey): KeyInfoEntity {
        return KeyInfoEntity().apply {
            publicKeyBase64 = keyInfo.unpaddedBase64PublicKey
            usages = keyInfo.usages?.let { RealmList(*it.toTypedArray()) } ?: RealmList()
            signatures = serializeSignatures(keyInfo.signatures)
            // TODO how to handle better, check if same keys?
            // reset trust
            trustLevelEntity = null
        }
    }

    fun serializeSignatures(signatures: Map<String, Map<String, String>>?): String {
        return signaturesAdapter.toJson(signatures)
    }

    fun deserializeSignatures(signatures: String?): Map<String, Map<String, String>>? {
        if (signatures == null) {
            return null
        }
        return try {
            signaturesAdapter.fromJson(signatures)
        } catch (failure: Throwable) {
            Timber.e(failure)
            null
        }
    }
}
