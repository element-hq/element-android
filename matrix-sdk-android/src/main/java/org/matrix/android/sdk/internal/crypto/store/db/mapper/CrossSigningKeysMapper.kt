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

    private val signaturesAdapter = moshi.adapter<Map<String, Map<String, String>>>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))

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
