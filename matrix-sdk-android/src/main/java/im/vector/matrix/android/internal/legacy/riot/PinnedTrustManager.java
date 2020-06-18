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
import java.util.List;

import javax.annotation.Nullable;
import javax.net.ssl.X509TrustManager;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * Implements a TrustManager that checks Certificates against an explicit list of known
 * fingerprints.
 */
public class PinnedTrustManager implements X509TrustManager {
    private final List<Fingerprint> mFingerprints;
    @Nullable
    private final X509TrustManager mDefaultTrustManager;

    /**
     * @param fingerprints        An array of SHA256 cert fingerprints
     * @param defaultTrustManager Optional trust manager to fall back on if cert does not match
     *                            any of the fingerprints. Can be null.
     */
    public PinnedTrustManager(List<Fingerprint> fingerprints, @Nullable X509TrustManager defaultTrustManager) {
        mFingerprints = fingerprints;
        mDefaultTrustManager = defaultTrustManager;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
        try {
            if (mDefaultTrustManager != null) {
                mDefaultTrustManager.checkClientTrusted(
                        chain, s
                );
                return;
            }
        } catch (CertificateException e) {
            // If there is an exception we fall back to checking fingerprints
            if (mFingerprints == null || mFingerprints.size() == 0) {
                throw new UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.getCause());
            }
        }
        checkTrusted("client", chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
        try {
            if (mDefaultTrustManager != null) {
                mDefaultTrustManager.checkServerTrusted(
                        chain, s
                );
                return;
            }
        } catch (CertificateException e) {
            // If there is an exception we fall back to checking fingerprints
            if (mFingerprints == null || mFingerprints.isEmpty()) {
                throw new UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.getCause());
            }
        }
        checkTrusted("server", chain);
    }

    private void checkTrusted(String type, X509Certificate[] chain) throws CertificateException {
        X509Certificate cert = chain[0];

        boolean found = false;
        if (mFingerprints != null) {
            for (Fingerprint allowedFingerprint : mFingerprints) {
                if (allowedFingerprint != null && allowedFingerprint.matchesCert(cert)) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            throw new UnrecognizedCertificateException(cert, Fingerprint.newSha256Fingerprint(cert), null);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
