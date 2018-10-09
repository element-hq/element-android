/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.ssl;

import android.util.Pair;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;

/**
 * Various utility classes for dealing with X509Certificates
 */
public class CertUtil {
    private static final String LOG_TAG = CertUtil.class.getSimpleName();

    /**
     * Generates the SHA-256 fingerprint of the given certificate
     *
     * @param cert the certificate.
     * @return the finger print
     * @throws CertificateException the certificate exception
     */
    public static byte[] generateSha256Fingerprint(X509Certificate cert) throws CertificateException {
        return generateFingerprint(cert, "SHA-256");
    }

    /**
     * Generates the SHA-1 fingerprint of the given certificate
     *
     * @param cert the certificated
     * @return the SHA1 fingerprint
     * @throws CertificateException the certificate exception
     */
    public static byte[] generateSha1Fingerprint(X509Certificate cert) throws CertificateException {
        return generateFingerprint(cert, "SHA-1");
    }

    /**
     * Generate the fingerprint for a dedicated type.
     *
     * @param cert the certificate
     * @param type the type
     * @return the fingerprint
     * @throws CertificateException certificate exception
     */
    private static byte[] generateFingerprint(X509Certificate cert, String type) throws CertificateException {
        final byte[] fingerprint;
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(type);
        } catch (Exception e) {
            // This really *really* shouldn't throw, as java should always have a SHA-256 and SHA-1 impl.
            throw new CertificateException(e);
        }

        fingerprint = md.digest(cert.getEncoded());

        return fingerprint;
    }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Convert the fingerprint to an hexa string.
     *
     * @param fingerprint the fingerprint
     * @return the hexa string.
     */
    public static String fingerprintToHexString(byte[] fingerprint) {
        return fingerprintToHexString(fingerprint, ' ');
    }

    public static String fingerprintToHexString(byte[] fingerprint, char sep) {
        char[] hexChars = new char[fingerprint.length * 3];
        for (int j = 0; j < fingerprint.length; j++) {
            int v = fingerprint[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = sep;
        }
        return new String(hexChars, 0, hexChars.length - 1);
    }

    /**
     * Recursively checks the exception to see if it was caused by an
     * UnrecognizedCertificateException
     *
     * @param e the throwable.
     * @return The UnrecognizedCertificateException if exists, else null.
     */
    public static UnrecognizedCertificateException getCertificateException(Throwable e) {
        int i = 0; // Just in case there is a getCause loop
        while (e != null && i < 10) {
            if (e instanceof UnrecognizedCertificateException) {
                return (UnrecognizedCertificateException) e;
            }
            e = e.getCause();
            i++;
        }

        return null;
    }

    /**
     * Create a SSLSocket factory for a HS config.
     *
     * @param hsConfig the HS config.
     * @return SSLSocket factory
     */
    public static Pair<SSLSocketFactory, X509TrustManager> newPinnedSSLSocketFactory(HomeServerConnectionConfig hsConfig) {
        try {
            X509TrustManager defaultTrustManager = null;

            // If we haven't specified that we wanted to pin the certs, fallback to standard
            // X509 checks if fingerprints don't match.
            if (!hsConfig.shouldPin()) {
                TrustManagerFactory tf = null;

                // get the PKIX instance
                try {
                    tf = TrustManagerFactory.getInstance("PKIX");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## newPinnedSSLSocketFactory() : TrustManagerFactory.getInstance failed " + e.getMessage(), e);
                }

                // it doesn't exist, use the default one.
                if (null == tf) {
                    try {
                        tf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## addRule : onBingRuleUpdateFailure failed " + e.getMessage(), e);
                    }
                }

                tf.init((KeyStore) null);
                TrustManager[] trustManagers = tf.getTrustManagers();

                for (int i = 0; i < trustManagers.length; i++) {
                    if (trustManagers[i] instanceof X509TrustManager) {
                        defaultTrustManager = (X509TrustManager) trustManagers[i];
                        break;
                    }
                }
            }

            TrustManager[] trustPinned = new TrustManager[]{
                    new PinnedTrustManager(hsConfig.getAllowedFingerprints(), defaultTrustManager)
            };

            SSLSocketFactory sslSocketFactory;

            if (hsConfig.forceUsageOfTlsVersions() && hsConfig.getAcceptedTlsVersions() != null) {
                // Force usage of accepted Tls Versions for Android < 20
                sslSocketFactory = new TLSSocketFactory(trustPinned, hsConfig.getAcceptedTlsVersions());
            } else {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustPinned, new java.security.SecureRandom());
                sslSocketFactory = sslContext.getSocketFactory();
            }

            return new Pair<>(sslSocketFactory, defaultTrustManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a Host name verifier for a hs config.
     *
     * @param hsConfig the hs config.
     * @return a new HostnameVerifier.
     */
    public static HostnameVerifier newHostnameVerifier(HomeServerConnectionConfig hsConfig) {
        final HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        final List<Fingerprint> trusted_fingerprints = hsConfig.getAllowedFingerprints();

        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (defaultVerifier.verify(hostname, session)) return true;
                if (trusted_fingerprints == null || trusted_fingerprints.size() == 0) return false;

                // If remote cert matches an allowed fingerprint, just accept it.
                try {
                    for (Certificate cert : session.getPeerCertificates()) {
                        for (Fingerprint allowedFingerprint : trusted_fingerprints) {
                            if (allowedFingerprint != null && cert instanceof X509Certificate && allowedFingerprint.matchesCert((X509Certificate) cert)) {
                                return true;
                            }
                        }
                    }
                } catch (SSLPeerUnverifiedException e) {
                    return false;
                } catch (CertificateException e) {
                    return false;
                }

                return false;
            }
        };
    }

    /**
     * Create a list of accepted TLS specifications for a hs config.
     *
     * @param hsConfig the hs config.
     * @return a list of accepted TLS specifications.
     */
    public static List<ConnectionSpec> newConnectionSpecs(HomeServerConnectionConfig hsConfig) {
        final ConnectionSpec.Builder builder = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS);

        final List<TlsVersion> tlsVersions = hsConfig.getAcceptedTlsVersions();
        if (null != tlsVersions) {
            builder.tlsVersions(tlsVersions.toArray(new TlsVersion[0]));
        }

        final List<CipherSuite> tlsCipherSuites = hsConfig.getAcceptedTlsCipherSuites();
        if (null != tlsCipherSuites) {
            builder.cipherSuites(tlsCipherSuites.toArray(new CipherSuite[0]));
        }

        builder.supportsTlsExtensions(hsConfig.shouldAcceptTlsExtensions());

        List<ConnectionSpec> list = new ArrayList<>();

        list.add(builder.build());

        if (hsConfig.isHttpConnectionAllowed()) {
            list.add(ConnectionSpec.CLEARTEXT);
        }

        return list;
    }
}
