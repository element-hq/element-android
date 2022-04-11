/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.network.ssl

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.network.ssl.CertUtil
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@JsonClass(generateAdapter = true)
data class Fingerprint(
        val bytes: ByteArray,
        val hashType: HashType
) {

    val displayableHexRepr: String by lazy {
        CertUtil.fingerprintToHexString(bytes)
    }

    @Throws(CertificateException::class)
    internal fun matchesCert(cert: X509Certificate): Boolean {
        val o: Fingerprint? = when (hashType) {
            HashType.SHA256 -> newSha256Fingerprint(cert)
            HashType.SHA1   -> newSha1Fingerprint(cert)
        }
        return equals(o)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fingerprint
        if (!bytes.contentEquals(other.bytes)) return false
        if (hashType != other.hashType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + hashType.hashCode()
        return result
    }

    internal companion object {

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

    @JsonClass(generateAdapter = false)
    enum class HashType {
        @Json(name = "sha-1") SHA1,
        @Json(name = "sha-256") SHA256
    }
}
