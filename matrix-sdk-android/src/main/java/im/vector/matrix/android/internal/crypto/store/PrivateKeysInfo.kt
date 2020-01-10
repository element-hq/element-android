package im.vector.matrix.android.internal.crypto.store

data class PrivateKeysInfo(
        val master: String? = null,
        val selfSigned: String? = null,
        val user: String? = null
)
