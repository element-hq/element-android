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

package org.matrix.android.sdk.api.session.crypto.crosssigning

import org.matrix.android.sdk.internal.crypto.model.CryptoInfo
import org.matrix.android.sdk.internal.crypto.model.CryptoInfoMapper
import org.matrix.android.sdk.internal.crypto.model.rest.RestKeyInfo

data class CryptoCrossSigningKey(
        override val userId: String,

        val usages: List<String>?,

        override val keys: Map<String, String>,

        override val signatures: Map<String, Map<String, String>>?,

        var trustLevel: DeviceTrustLevel? = null
) : CryptoInfo {

    override fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()
        userId.let { map["user_id"] = it }
        usages?.let { map["usage"] = it }
        keys.let { map["keys"] = it }

        return map
    }

    val unpaddedBase64PublicKey: String? = keys.values.firstOrNull()

    val isMasterKey = usages?.contains(KeyUsage.MASTER.value) ?: false
    val isSelfSigningKey = usages?.contains(KeyUsage.SELF_SIGNING.value) ?: false
    val isUserKey = usages?.contains(KeyUsage.USER_SIGNING.value) ?: false

    fun addSignatureAndCopy(userId: String, signedWithNoPrefix: String, signature: String): CryptoCrossSigningKey {
        val updated = (signatures?.toMutableMap() ?: HashMap())
        val userMap = updated[userId]?.toMutableMap()
                ?: HashMap<String, String>().also { updated[userId] = it }
        userMap["ed25519:$signedWithNoPrefix"] = signature

        return this.copy(
                signatures = updated
        )
    }

    fun copyForSignature(userId: String, signedWithNoPrefix: String, signature: String): CryptoCrossSigningKey {
        return this.copy(
                signatures = mapOf(userId to mapOf("ed25519:$signedWithNoPrefix" to signature))
        )
    }

    internal data class Builder(
            val userId: String,
            val usage: KeyUsage,
            private var base64Pkey: String? = null,
            private val signatures: ArrayList<Triple<String, String, String>> = ArrayList()
    ) {

        fun key(publicKeyBase64: String) = apply {
            base64Pkey = publicKeyBase64
        }

        fun signature(userId: String, keySignedBase64: String, base64Signature: String) = apply {
            signatures.add(Triple(userId, keySignedBase64, base64Signature))
        }

        fun build(): CryptoCrossSigningKey {
            val b64key = base64Pkey ?: throw IllegalArgumentException("")

            val signMap = HashMap<String, HashMap<String, String>>()
            signatures.forEach { info ->
                val uMap = signMap[info.first]
                        ?: HashMap<String, String>().also { signMap[info.first] = it }
                uMap["ed25519:${info.second}"] = info.third
            }

            return CryptoCrossSigningKey(
                    userId = userId,
                    usages = listOf(usage.value),
                    keys = mapOf("ed25519:$b64key" to b64key),
                    signatures = signMap)
        }
    }
}

internal enum class KeyUsage(val value: String) {
    MASTER("master"),
    SELF_SIGNING("self_signing"),
    USER_SIGNING("user_signing")
}

internal fun CryptoCrossSigningKey.toRest(): RestKeyInfo {
    return CryptoInfoMapper.map(this)
}
