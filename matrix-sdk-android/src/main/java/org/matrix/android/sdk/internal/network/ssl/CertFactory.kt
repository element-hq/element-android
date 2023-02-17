/*
 * Copyright (c) 2023 New Vector Ltd
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

@file:Suppress("unused")

package org.matrix.android.sdk.internal.network.ssl

import org.matrix.android.sdk.api.network.ssl.Fingerprint
import timber.log.Timber
import java.nio.charset.StandardCharsets.UTF_8
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

// TODO: delete this file used for debugging
object CertFactory {
    const val USE_FAKE_CERT = true
    // matrix.org
    private const val REAL_CERT = """-----BEGIN CERTIFICATE-----
MIIFMjCCBNmgAwIBAgIQBBeWeU5gfMfdXerawPjLJzAKBggqhkjOPQQDAjBKMQsw
CQYDVQQGEwJVUzEZMBcGA1UEChMQQ2xvdWRmbGFyZSwgSW5jLjEgMB4GA1UEAxMX
Q2xvdWRmbGFyZSBJbmMgRUNDIENBLTMwHhcNMjIwNjAzMDAwMDAwWhcNMjMwNjAy
MjM1OTU5WjB1MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQG
A1UEBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQQ2xvdWRmbGFyZSwgSW5jLjEe
MBwGA1UEAxMVc25pLmNsb3VkZmxhcmVzc2wuY29tMFkwEwYHKoZIzj0CAQYIKoZI
zj0DAQcDQgAE8dtavEXS2K4uRm+1Es+J2y6DawGgf7o7Pq3eeWXVmKaMH6mkANzB
CWRQPwIHwiY4EIHxJ+rj6cHOx3ZMWLhOVaOCA3QwggNwMB8GA1UdIwQYMBaAFKXO
N+rrsHUOlGeItEX62SQQh5YfMB0GA1UdDgQWBBQHH1R1J09Rny+Uow1mStelGJ5G
QDA6BgNVHREEMzAxggptYXRyaXgub3JnggwqLm1hdHJpeC5vcmeCFXNuaS5jbG91
ZGZsYXJlc3NsLmNvbTAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUH
AwEGCCsGAQUFBwMCMHsGA1UdHwR0MHIwN6A1oDOGMWh0dHA6Ly9jcmwzLmRpZ2lj
ZXJ0LmNvbS9DbG91ZGZsYXJlSW5jRUNDQ0EtMy5jcmwwN6A1oDOGMWh0dHA6Ly9j
cmw0LmRpZ2ljZXJ0LmNvbS9DbG91ZGZsYXJlSW5jRUNDQ0EtMy5jcmwwPgYDVR0g
BDcwNTAzBgZngQwBAgIwKTAnBggrBgEFBQcCARYbaHR0cDovL3d3dy5kaWdpY2Vy
dC5jb20vQ1BTMHYGCCsGAQUFBwEBBGowaDAkBggrBgEFBQcwAYYYaHR0cDovL29j
c3AuZGlnaWNlcnQuY29tMEAGCCsGAQUFBzAChjRodHRwOi8vY2FjZXJ0cy5kaWdp
Y2VydC5jb20vQ2xvdWRmbGFyZUluY0VDQ0NBLTMuY3J0MAwGA1UdEwEB/wQCMAAw
ggF+BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB2AOg+0No+9QY1MudXKLyJa8kD08vR
EWvs62nhd31tBr1uAAABgSj8RDsAAAQDAEcwRQIgeKrerQniRck5d4Z6znAukXfy
J9UkueqAgMFbRIwtUpsCIQD80xXpZ0fVpRizbsaqLtaVoauUfMjVHaY8pJn6iq/R
LQB2ADXPGRu/sWxXvw+tTG1Cy7u2JyAmUeo/4SrvqAPDO9ZMAAABgSj8REIAAAQD
AEcwRQIgBjnCX2/hZeblE8/7oyY3DMqQKAXL2GViwjqKtdpd6HMCIQDLqK5sPNX3
aEq0a+U1j9THuE8TRcaNDhsa/J1dPIN3xgB2ALNzdwfhhFD4Y4bWBancEQlKeS2x
ZwwLh9zwAw55NqWaAAABgSj8RH8AAAQDAEcwRQIhANNxszQajjCzVJFmrt9csXx/
JMHlPuLDCe0OOQSmNwgGAiAaBdLaFHDCZcwu0XWygrZa2PVuA6V1Rx8TRh1OIkI8
0TAKBggqhkjOPQQDAgNHADBEAiAYvhguHG93pulpDLIx8m/nQjrlJ3XIE3EJvCc/
7eqYIQIgCTFkBsfnOrMNDMmpKThKmZLeN+rCzRimNJzVrA9FoUA=
-----END CERTIFICATE-----"""

    // matrix.org self signed
    private const val FAKE_CERT = """-----BEGIN CERTIFICATE-----
MIIDCzCCAfOgAwIBAgIUXrfdDbZtpn9HealS9lniLu21fPwwDQYJKoZIhvcNAQEL
BQAwFTETMBEGA1UEAwwKbWF0cml4Lm9yZzAeFw0yMzAyMTYxNTA4MDJaFw0yNDAy
MTYxNTA4MDJaMBUxEzARBgNVBAMMCm1hdHJpeC5vcmcwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQCUSHOv+arN7NL1IA0c1v9mlmNTaVIzIQHoWwAv5DAI
3EutVjjWKAukGRU8ZlGMzA3lN9Ho21Rzn0+G/8tuMEr+msO153n/AUU8fGLJa5zX
LNrOn7bg0KtGBuCtZOowmT7zWS73OBLJqC5F3cTtkQc9nKdOf50xfI/gVBcwwEZ5
PXyCxSOYvXyryOkStHL70sNfMAAwrznObtT2+W6VGn31A86QWiMKC7flbHL0n+1D
mXSAANsMYH3fYMD8wbaoBDhF4GRt1okT7MV6AFvYNk6FCF6N9cVuRipKc7USYEcq
JydBiYH1a6bHhisRNTb9ZEnwO6BDeVE87Q7nJjYUUperAgMBAAGjUzBRMB0GA1Ud
DgQWBBTyJ6kHBTo/nyeC8jQQmW/bO8afXzAfBgNVHSMEGDAWgBTyJ6kHBTo/nyeC
8jQQmW/bO8afXzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCA
FkPAiDPM9vt5qw46Vu4obLJMN3CjvOnnWx3Ydj2JmN7uLh6rKYO//r7oQLm6Mxhx
6RxuwdTkUdmG2q4Vb7UkmGHFDyN5uYu+QMHI8Iqj4lBcGZ6GxTb8bb3tptFhywmr
JF5yRBS81VcqPxdNrXgcUcwXM5ai1dhW7IJC9xQ4I/hLsr84gW5qAF509uOQcX0C
vYpowVMQTzhvaeOnOZIIAQbEC8mVQ2CvGQxJWK1md6Q2kh5vEZTrtSHTdCDogxco
GcpkZPllI0egxNBjgFq6TBlnUlc0Vusv6aQP4qU0M1qWzNNtwGyTthQ2aG5ELrlk
S23krUKHHamt3FH6dZiC
-----END CERTIFICATE-----"""

    fun createStaticFingerprint() = if (USE_FAKE_CERT) {
        createFakeFingerprint()
    } else {
        createRealFingerprint()
    }


    fun createRealFingerprint() =
            createFingerprint(createCert(REAL_CERT))

    fun createFakeFingerprint() =
            createFingerprint(createCert(FAKE_CERT))

    private fun createFingerprint(cert: X509Certificate): Fingerprint {
        val fingerprint = Fingerprint.newSha256Fingerprint(cert)

        Timber.d("## CERT Fingerprint: ${fingerprint.displayableHexRepr}")
        return fingerprint
    }

    private fun createCert(certString: String) = CertificateFactory
            .getInstance("X.509")
            .generateCertificate(certString.byteInputStream(UTF_8)) as X509Certificate
}
