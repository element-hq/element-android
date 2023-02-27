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

package org.matrix.android.sdk.internal.network.ssl

import android.os.Build
import androidx.annotation.RequiresApi
import org.matrix.android.sdk.api.auth.certs.TrustedCertificateRepository
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * Implements a TrustManager that checks Certificates against an explicit list of known
 * fingerprints.
 *
 * @property staticFingerprints An array of SHA256 cert fingerprints
 * @property defaultTrustManager Optional truHst manager to fall back on if cert does not match
 * any of the fingerprints. Can be null.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class PinnedTrustManagerApi24(
        private val staticFingerprints: List<Fingerprint>,
        private val defaultTrustManager: X509ExtendedTrustManager?,
        private val trustedCertificateRepository: TrustedCertificateRepository,
) : X509ExtendedTrustManager() {

    private val fingerprints: List<Fingerprint>
        get() = staticFingerprints +
                listOfNotNull(trustedCertificateRepository.getCurTrustedCert())

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) {
        check(chain) {
            checkClientTrusted(chain, authType, engine)
        }
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) {
        check(chain) {
            checkClientTrusted(chain, authType, socket)
        }
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        check(chain) {
            checkClientTrusted(chain, authType)
        }
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) {
        check(chain) {
            checkServerTrusted(chain, authType, socket)
        }
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) {
        check(chain) {
            checkServerTrusted(chain, authType, engine)
        }
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, s: String) =
            check(chain) {
                checkServerTrusted(chain, s)
            }

    private fun check(chain: Array<X509Certificate>, defaultCheck: X509ExtendedTrustManager.() -> Unit) {
        if (defaultTrustManager != null) {
            try {
                defaultTrustManager.defaultCheck()
            } catch (e: CertificateException) {
                checkPins(chain)
            }
            checkCaTrusted(chain)
        } else {
            checkPins(chain)
        }
    }

    @Throws(CertificateException::class)
    private fun checkCaTrusted(chain: Array<X509Certificate>) {
        // Get the certificate closest to the root.
        // This may or may not be the root certificate.
        val cert = chain[chain.size - 1]

        val root = acceptedIssuers.firstOrNull { issuer ->
            if (issuer.subjectDN != cert.subjectDN &&
                    issuer.subjectDN != cert.issuerDN)
                return@firstOrNull false

            try {
                cert.verify(issuer.publicKey)
            } catch (e: Exception) {
                return@firstOrNull false
            }

            return@firstOrNull true
        } ?: throw UnrecognizedCertificateException(cert, Fingerprint.newSha256Fingerprint(cert), isCaCert = false, null)

        if (!fingerprints.any { it.matchesCert(root) }) {
            throw UnrecognizedCertificateException(root, Fingerprint.newSha256Fingerprint(root), isCaCert = true, null)
        }
    }

    @Throws(CertificateException::class)
    private fun checkPins(chain: Array<X509Certificate>) {
        val cert = chain[0]

        if (!staticFingerprints.any { it.matchesCert(cert) }) {
            throw UnrecognizedCertificateException(cert, Fingerprint.newSha256Fingerprint(cert), null)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager?.acceptedIssuers ?: emptyArray()
    }
}
