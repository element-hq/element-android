/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.network.ssl

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@JsonClass(generateAdapter = true)
data class Fingerprint(
        val mBytes: ByteArray,
        val mHashType: HashType
) {

    val displayableHexRepr: String by lazy {
        CertUtil.fingerprintToHexString(mBytes)
    }

    @Throws(CertificateException::class)
    fun matchesCert(cert: X509Certificate): Boolean {
        var o: Fingerprint? = when (mHashType) {
            Fingerprint.HashType.SHA256 -> Fingerprint.newSha256Fingerprint(cert)
            Fingerprint.HashType.SHA1 -> Fingerprint.newSha1Fingerprint(cert)
        }
        return equals(o)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fingerprint
        if (!mBytes.contentEquals(other.mBytes)) return false
        if (mHashType != other.mHashType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mBytes.contentHashCode()
        result = 31 * result + mHashType.hashCode()
        return result
    }

    companion object {

        @Throws(CertificateException::class)
        fun newSha256Fingerprint(cert: X509Certificate): Fingerprint {
            return Fingerprint(
                    CertUtil.generateSha256Fingerprint(cert),
                    HashType.SHA256
            )
        }

        @Throws(CertificateException::class)
        fun newSha1Fingerprint(cert: X509Certificate): Fingerprint {
            return Fingerprint(
                    CertUtil.generateSha1Fingerprint(cert),
                    HashType.SHA1
            )
        }
    }

    enum class HashType {
        @Json(name = "sha-1") SHA1,
        @Json(name = "sha-256")SHA256
    }

}
