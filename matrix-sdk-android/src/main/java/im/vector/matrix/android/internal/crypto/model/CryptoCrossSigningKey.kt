package im.vector.matrix.android.internal.crypto.model

import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.rest.RestKeyInfo

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

enum class KeyUsage(val value: String) {
    MASTER("master"),
    SELF_SIGNING("self_signing"),
    USER_SIGNING("user_signing")
}

fun CryptoCrossSigningKey.toRest(): RestKeyInfo {
    return CryptoInfoMapper.map(this)
}
