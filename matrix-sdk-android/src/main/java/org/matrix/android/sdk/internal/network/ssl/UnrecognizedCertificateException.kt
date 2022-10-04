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

package org.matrix.android.sdk.internal.network.ssl

import org.matrix.android.sdk.api.network.ssl.Fingerprint
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Thrown when we are given a certificate that does match the certificate we were told to
 * expect.
 */
internal data class UnrecognizedCertificateException(
        val certificate: X509Certificate,
        val fingerprint: Fingerprint,
        override val cause: Throwable?
) : CertificateException("Unrecognized certificate with unknown fingerprint: " + certificate.subjectDN, cause)
