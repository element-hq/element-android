package im.vector.matrix.android.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.crypto.model.MXKeysObject

/**
 * "self_signing_key": {
 *       "user_id": "@alice:example.com",
 *       "usage": ["self_signing"],
 *       "keys": {
 *               "ed25519:base64+self+signing+public+key": "base64+self+signing+public+key"
 *       },
 *       "signatures": {
 *               "@alice:example.com": {
 *                       "ed25519:base64+master+public+key": "base64+signature"
 *               }
 *       }
 *  }
 */
@JsonClass(generateAdapter = true)
data class CrossSigningKeyInfo(
        /**
         * The user who owns the key
         */
        @Json(name = "user_id")
        override var userId: String,
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
        override var keys: Map<String, String>?,

        /**
         *  Signatures of the key.
         *  A self-signing or user-signing key must be signed by the master key.
         *  A master key may be signed by a device.
         */
        @Json(name = "signatures")
        override var signatures: Map<String, Map<String, String>>? = null
) : MXKeysObject {
    // Shortcut to get key as "keys" is an object that must have one entry
    val unpaddedBase64PublicKey: String? = keys?.values?.firstOrNull()

    val isMasterKey = usages?.contains(KeyUsage.MASTER.value) ?: false
    val isSelfSigningKey = usages?.contains(KeyUsage.SELF_SIGNING.value) ?: false
    val isUserKey = usages?.contains(KeyUsage.USER_SIGNING.value) ?: false

    fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()
        userId.let { map["user_id"] = it }
        usages?.let { map["usage"] = it }
        keys?.let { map["keys"] = it }

        return map
    }

    fun addSignature(userId: String, signedWithNoPrefix: String, signature: String) = apply {
        val updated = (signatures?.toMutableMap() ?: HashMap())
        val userMap = updated[userId]?.toMutableMap()
                ?: HashMap<String, String>().also { updated[userId] = it }
        userMap["ed25519:${signedWithNoPrefix}"] = signature
        signatures = updated
    }

//    fun toXSigningKeys(): XSigningKeys {
//        return XSigningKeys(
//                userId = userId,
//                usage = usages ?: emptyList(),
//                keys = keys ?: emptyMap(),
//                signatures = signatures
//
//        )
//    }

    data class Builder(
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

        fun build(): CrossSigningKeyInfo {
            val b64key = base64Pkey ?: throw IllegalArgumentException("")

            val signMap = HashMap<String, HashMap<String, String>>()
            signatures.forEach { info ->
                val uMap = signMap[info.first]
                        ?: HashMap<String, String>().also { signMap[info.first] = it }
                uMap["ed25519:${info.second}"] = info.third
            }

            return CrossSigningKeyInfo(
                    userId = userId,
                    usages = listOf(usage.value),
                    keys = mapOf("ed25519:$b64key" to b64key),
                    signatures = signMap
            )
        }
    }

    enum class KeyUsage(val value: String) {
        MASTER("master"),
        SELF_SIGNING("self_signing"),
        USER_SIGNING("user_signing")
    }

}
