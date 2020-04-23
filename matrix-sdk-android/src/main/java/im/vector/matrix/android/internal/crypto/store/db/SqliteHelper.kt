package im.vector.matrix.android.internal.crypto.store.db

import android.util.Base64
import im.vector.matrix.android.internal.util.CompatUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPInputStream

/**
 * Serialize any Serializable object, zip it and convert to Base64 String
 */
fun serializeForSqlite(o: Any?): String? {
    if (o == null) {
        return null
    }

    val baos = ByteArrayOutputStream()
    val gzis = CompatUtil.createGzipOutputStream(baos)
    val out = ObjectOutputStream(gzis)

    out.writeObject(o)
    out.close()

    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}

/**
 * Do the opposite of serializeForSqlite.
 */
fun <T> deserializeFromSqlite(string: String?): T? {
    if (string == null) {
        return null
    }

    val decodedB64 = Base64.decode(string.toByteArray(), Base64.DEFAULT)

    val bais = ByteArrayInputStream(decodedB64)
    val gzis = GZIPInputStream(bais)
    val ois = ObjectInputStream(gzis)

    @Suppress("UNCHECKED_CAST")
    val result = ois.readObject() as T

    ois.close()

    return result
}
