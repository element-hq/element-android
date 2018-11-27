package im.vector.matrix.android.internal.util

import java.security.MessageDigest

fun String.md5() = try {
    val digest = MessageDigest.getInstance("md5")
    digest.update(toByteArray())
    val bytes = digest.digest()
    val sb = StringBuilder()
    for (i in bytes.indices) {
        sb.append(String.format("%02X", bytes[i]))
    }
    sb.toString().toLowerCase()
} catch (exc: Exception) {
    // Should not happen, but just in case
    hashCode().toString()
}
