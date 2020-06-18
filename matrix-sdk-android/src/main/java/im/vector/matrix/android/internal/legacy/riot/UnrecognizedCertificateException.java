/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy.riot;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * Thrown when we are given a certificate that does match the certificate we were told to
 * expect.
 */
public class UnrecognizedCertificateException extends CertificateException {
    private final X509Certificate mCert;
    private final Fingerprint mFingerprint;

    public UnrecognizedCertificateException(X509Certificate cert, Fingerprint fingerprint, Throwable cause) {
        super("Unrecognized certificate with unknown fingerprint: " + cert.getSubjectDN(), cause);
        mCert = cert;
        mFingerprint = fingerprint;
    }

    public X509Certificate getCertificate() {
        return mCert;
    }

    public Fingerprint getFingerprint() {
        return mFingerprint;
    }
}
