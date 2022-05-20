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

package org.matrix.android.sdk.internal.legacy.riot;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import okhttp3.CipherSuite;
import okhttp3.TlsVersion;
import timber.log.Timber;

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 *
 * Represents how to connect to a specific Homeserver, may include credentials to use.
 */
public class HomeServerConnectionConfig {

    // the homeserver URI
    private Uri mHomeServerUri;
    // the jitsi server URI. Can be null
    @Nullable
    private Uri mJitsiServerUri;
    // the identity server URI. Can be null
    @Nullable
    private Uri mIdentityServerUri;
    // the anti-virus server URI
    private Uri mAntiVirusServerUri;
    // allowed fingerprints
    private List<Fingerprint> mAllowedFingerprints = new ArrayList<>();
    // the credentials
    private Credentials mCredentials;
    // tell whether we should reject X509 certs that were issued by trusts CAs and only trustcerts with matching fingerprints.
    private boolean mPin;
    // the accepted TLS versions
    private List<TlsVersion> mTlsVersions;
    // the accepted TLS cipher suites
    private List<CipherSuite> mTlsCipherSuites;
    // should accept TLS extensions
    private boolean mShouldAcceptTlsExtensions = true;
    // Force usage of TLS versions
    private boolean mForceUsageTlsVersions;
    // the proxy hostname
    private String mProxyHostname;
    // the proxy port
    private int mProxyPort = -1;


    /**
     * Private constructor. Please use the Builder
     */
    private HomeServerConnectionConfig() {
        // Private constructor
    }

    /**
     * Update the homeserver URI.
     *
     * @param uri the new HS uri
     */
    public void setHomeserverUri(Uri uri) {
        mHomeServerUri = uri;
    }

    /**
     * @return the homeserver uri
     */
    public Uri getHomeserverUri() {
        return mHomeServerUri;
    }

    /**
     * @return the jitsi server uri
     */
    public Uri getJitsiServerUri() {
        return mJitsiServerUri;
    }

    /**
     * @return the identity server uri, or null if not defined
     */
    @Nullable
    public Uri getIdentityServerUri() {
        return mIdentityServerUri;
    }

    /**
     * @return the anti-virus server uri
     */
    public Uri getAntiVirusServerUri() {
        if (null != mAntiVirusServerUri) {
            return mAntiVirusServerUri;
        }
        // Else consider the HS uri by default.
        return mHomeServerUri;
    }

    /**
     * @return the allowed fingerprints.
     */
    public List<Fingerprint> getAllowedFingerprints() {
        return mAllowedFingerprints;
    }

    /**
     * @return the credentials
     */
    public Credentials getCredentials() {
        return mCredentials;
    }

    /**
     * Update the credentials.
     *
     * @param credentials the new credentials
     */
    public void setCredentials(Credentials credentials) {
        mCredentials = credentials;

        // Override homeserver url and/or identity server url if provided
        if (credentials.wellKnown != null) {
            if (credentials.wellKnown.homeServer != null) {
                String homeServerUrl = credentials.wellKnown.homeServer.baseURL;

                if (!TextUtils.isEmpty(homeServerUrl)) {
                    // remove trailing "/"
                    if (homeServerUrl.endsWith("/")) {
                        homeServerUrl = homeServerUrl.substring(0, homeServerUrl.length() - 1);
                    }

                    Timber.d("Overriding homeserver url to " + homeServerUrl);
                    mHomeServerUri = Uri.parse(homeServerUrl);
                }
            }

            if (credentials.wellKnown.identityServer != null) {
                String identityServerUrl = credentials.wellKnown.identityServer.baseURL;

                if (!TextUtils.isEmpty(identityServerUrl)) {
                    // remove trailing "/"
                    if (identityServerUrl.endsWith("/")) {
                        identityServerUrl = identityServerUrl.substring(0, identityServerUrl.length() - 1);
                    }

                    Timber.d("Overriding identity server url to " + identityServerUrl);
                    mIdentityServerUri = Uri.parse(identityServerUrl);
                }
            }

            if (credentials.wellKnown.jitsiServer != null) {
                String jitsiServerUrl = credentials.wellKnown.jitsiServer.preferredDomain;

                if (!TextUtils.isEmpty(jitsiServerUrl)) {
                    // add trailing "/"
                    if (!jitsiServerUrl.endsWith("/")) {
                        jitsiServerUrl = jitsiServerUrl + "/";
                    }

                    Timber.d("Overriding jitsi server url to " + jitsiServerUrl);
                    mJitsiServerUri = Uri.parse(jitsiServerUrl);
                }
            }
        }
    }

    /**
     * @return whether we should reject X509 certs that were issued by trusts CAs and only trust
     * certs with matching fingerprints.
     */
    public boolean shouldPin() {
        return mPin;
    }

    /**
     * TLS versions accepted for TLS connections with the homeserver.
     */
    @Nullable
    public List<TlsVersion> getAcceptedTlsVersions() {
        return mTlsVersions;
    }

    /**
     * TLS cipher suites accepted for TLS connections with the homeserver.
     */
    @Nullable
    public List<CipherSuite> getAcceptedTlsCipherSuites() {
        return mTlsCipherSuites;
    }

    /**
     * @return whether we should accept TLS extensions.
     */
    public boolean shouldAcceptTlsExtensions() {
        return mShouldAcceptTlsExtensions;
    }

    /**
     * @return true if the usage of TlsVersions has to be forced
     */
    public boolean forceUsageOfTlsVersions() {
        return mForceUsageTlsVersions;
    }


    /**
     * @return proxy config if available
     */
    @Nullable
    public Proxy getProxyConfig() {
        if (mProxyHostname == null || mProxyHostname.length() == 0 || mProxyPort == -1) {
            return null;
        }

        return new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(mProxyHostname, mProxyPort));
    }


    @Override
    public String toString() {
        return "HomeserverConnectionConfig{" +
                "mHomeServerUri=" + mHomeServerUri +
                ", mJitsiServerUri=" + mJitsiServerUri +
                ", mIdentityServerUri=" + mIdentityServerUri +
                ", mAntiVirusServerUri=" + mAntiVirusServerUri +
                ", mAllowedFingerprints size=" + mAllowedFingerprints.size() +
                ", mCredentials=" + mCredentials +
                ", mPin=" + mPin +
                ", mShouldAcceptTlsExtensions=" + mShouldAcceptTlsExtensions +
                ", mProxyHostname=" + (null == mProxyHostname ? "" : mProxyHostname) +
                ", mProxyPort=" + (-1 == mProxyPort ? "" : mProxyPort) +
                ", mTlsVersions=" + (null == mTlsVersions ? "" : mTlsVersions.size()) +
                ", mTlsCipherSuites=" + (null == mTlsCipherSuites ? "" : mTlsCipherSuites.size()) +
                '}';
    }

    /**
     * Convert the object instance into a JSon object
     *
     * @return the JSon representation
     * @throws JSONException the JSON conversion failure reason
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("home_server_url", mHomeServerUri.toString());
        Uri jitsiServerUri = getJitsiServerUri();
        if (jitsiServerUri != null) {
            json.put("jitsi_server_url", jitsiServerUri.toString());
        }
        Uri identityServerUri = getIdentityServerUri();
        if (identityServerUri != null) {
            json.put("identity_server_url", identityServerUri.toString());
        }

        if (mAntiVirusServerUri != null) {
            json.put("antivirus_server_url", mAntiVirusServerUri.toString());
        }

        json.put("pin", mPin);

        if (mCredentials != null) json.put("credentials", mCredentials.toJson());
        if (mAllowedFingerprints != null) {
            List<JSONObject> fingerprints = new ArrayList<>(mAllowedFingerprints.size());

            for (Fingerprint fingerprint : mAllowedFingerprints) {
                fingerprints.add(fingerprint.toJson());
            }

            json.put("fingerprints", new JSONArray(fingerprints));
        }

        json.put("tls_extensions", mShouldAcceptTlsExtensions);

        if (mTlsVersions != null) {
            List<String> tlsVersions = new ArrayList<>(mTlsVersions.size());

            for (TlsVersion tlsVersion : mTlsVersions) {
                tlsVersions.add(tlsVersion.javaName());
            }

            json.put("tls_versions", new JSONArray(tlsVersions));
        }

        json.put("force_usage_of_tls_versions", mForceUsageTlsVersions);

        if (mTlsCipherSuites != null) {
            List<String> tlsCipherSuites = new ArrayList<>(mTlsCipherSuites.size());

            for (CipherSuite tlsCipherSuite : mTlsCipherSuites) {
                tlsCipherSuites.add(tlsCipherSuite.javaName());
            }

            json.put("tls_cipher_suites", new JSONArray(tlsCipherSuites));
        }

        if (mProxyPort != -1) {
            json.put("proxy_port", mProxyPort);
        }

        if (mProxyHostname != null && mProxyHostname.length() > 0) {
            json.put("proxy_hostname", mProxyHostname);
        }

        return json;
    }

    /**
     * Create an object instance from the json object.
     *
     * @param jsonObject the json object
     * @return a HomeServerConnectionConfig instance
     * @throws JSONException the conversion failure reason
     */
    public static HomeServerConnectionConfig fromJson(JSONObject jsonObject) throws JSONException {
        JSONObject credentialsObj = jsonObject.optJSONObject("credentials");
        Credentials creds = credentialsObj != null ? Credentials.fromJson(credentialsObj) : null;

        Builder builder = new Builder()
                .withHomeServerUri(Uri.parse(jsonObject.getString("home_server_url")))
                .withJitsiServerUri(jsonObject.has("jitsi_server_url") ? Uri.parse(jsonObject.getString("jitsi_server_url")) : null)
                .withIdentityServerUri(jsonObject.has("identity_server_url") ? Uri.parse(jsonObject.getString("identity_server_url")) : null)
                .withCredentials(creds)
                .withPin(jsonObject.optBoolean("pin", false));

        JSONArray fingerprintArray = jsonObject.optJSONArray("fingerprints");
        if (fingerprintArray != null) {
            for (int i = 0; i < fingerprintArray.length(); i++) {
                builder.addAllowedFingerPrint(Fingerprint.fromJson(fingerprintArray.getJSONObject(i)));
            }
        }

        // Set the anti-virus server uri if any
        if (jsonObject.has("antivirus_server_url")) {
            builder.withAntiVirusServerUri(Uri.parse(jsonObject.getString("antivirus_server_url")));
        }

        builder.withShouldAcceptTlsExtensions(jsonObject.optBoolean("tls_extensions", true));

        // Set the TLS versions if any
        if (jsonObject.has("tls_versions")) {
            JSONArray tlsVersionsArray = jsonObject.optJSONArray("tls_versions");
            if (tlsVersionsArray != null) {
                for (int i = 0; i < tlsVersionsArray.length(); i++) {
                    builder.addAcceptedTlsVersion(TlsVersion.forJavaName(tlsVersionsArray.getString(i)));
                }
            }
        }

        builder.forceUsageOfTlsVersions(jsonObject.optBoolean("force_usage_of_tls_versions", false));

        // Set the TLS cipher suites if any
        if (jsonObject.has("tls_cipher_suites")) {
            JSONArray tlsCipherSuitesArray = jsonObject.optJSONArray("tls_cipher_suites");
            if (tlsCipherSuitesArray != null) {
                for (int i = 0; i < tlsCipherSuitesArray.length(); i++) {
                    builder.addAcceptedTlsCipherSuite(CipherSuite.forJavaName(tlsCipherSuitesArray.getString(i)));
                }
            }
        }

        // Set the proxy options right if any
        if (jsonObject.has("proxy_hostname") && jsonObject.has("proxy_port")) {
            builder.withProxy(jsonObject.getString("proxy_hostname"), jsonObject.getInt("proxy_port"));
        }

        return builder.build();
    }

    /**
     * Builder
     */
    public static class Builder {
        private HomeServerConnectionConfig mHomeServerConnectionConfig;

        /**
         * Builder constructor
         */
        public Builder() {
            mHomeServerConnectionConfig = new HomeServerConnectionConfig();
        }

        /**
         * create a Builder from an existing HomeServerConnectionConfig
         */
        public Builder(HomeServerConnectionConfig from) {
            try {
                mHomeServerConnectionConfig = HomeServerConnectionConfig.fromJson(from.toJson());
            } catch (JSONException e) {
                // Should not happen
                throw new RuntimeException("Unable to create a HomeServerConnectionConfig", e);
            }
        }

        /**
         * @param homeServerUri The URI to use to connect to the homeserver. Cannot be null
         * @return this builder
         */
        public Builder withHomeServerUri(final Uri homeServerUri) {
            if (homeServerUri == null || (!"http".equals(homeServerUri.getScheme()) && !"https".equals(homeServerUri.getScheme()))) {
                throw new RuntimeException("Invalid homeserver URI: " + homeServerUri);
            }

            // remove trailing /
            if (homeServerUri.toString().endsWith("/")) {
                try {
                    String url = homeServerUri.toString();
                    mHomeServerConnectionConfig.mHomeServerUri = Uri.parse(url.substring(0, url.length() - 1));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid homeserver URI: " + homeServerUri);
                }
            } else {
                mHomeServerConnectionConfig.mHomeServerUri = homeServerUri;
            }

            return this;
        }

        /**
         * @param jitsiServerUri The URI to use to manage identity. Can be null
         * @return this builder
         */
        public Builder withJitsiServerUri(@Nullable final Uri jitsiServerUri) {
            if (jitsiServerUri != null
                    && !jitsiServerUri.toString().isEmpty()
                    && !"http".equals(jitsiServerUri.getScheme())
                    && !"https".equals(jitsiServerUri.getScheme())) {
                throw new RuntimeException("Invalid jitsi server URI: " + jitsiServerUri);
            }

            // add trailing /
            if ((null != jitsiServerUri) && !jitsiServerUri.toString().endsWith("/")) {
                try {
                    String url = jitsiServerUri.toString();
                    mHomeServerConnectionConfig.mJitsiServerUri = Uri.parse(url + "/");
                } catch (Exception e) {
                    throw new RuntimeException("Invalid jitsi server URI: " + jitsiServerUri);
                }
            } else {
                if (jitsiServerUri != null && jitsiServerUri.toString().isEmpty()) {
                    mHomeServerConnectionConfig.mJitsiServerUri = null;
                } else {
                    mHomeServerConnectionConfig.mJitsiServerUri = jitsiServerUri;
                }
            }

            return this;
        }

        /**
         * @param identityServerUri The URI to use to manage identity. Can be null
         * @return this builder
         */
        public Builder withIdentityServerUri(@Nullable final Uri identityServerUri) {
            if (identityServerUri != null
                    && !identityServerUri.toString().isEmpty()
                    && !"http".equals(identityServerUri.getScheme())
                    && !"https".equals(identityServerUri.getScheme())) {
                throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
            }

            // remove trailing /
            if ((null != identityServerUri) && identityServerUri.toString().endsWith("/")) {
                try {
                    String url = identityServerUri.toString();
                    mHomeServerConnectionConfig.mIdentityServerUri = Uri.parse(url.substring(0, url.length() - 1));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
                }
            } else {
                if (identityServerUri != null && identityServerUri.toString().isEmpty()) {
                    mHomeServerConnectionConfig.mIdentityServerUri = null;
                } else {
                    mHomeServerConnectionConfig.mIdentityServerUri = identityServerUri;
                }
            }

            return this;
        }

        /**
         * @param credentials The credentials to use, if needed. Can be null.
         * @return this builder
         */
        public Builder withCredentials(@Nullable Credentials credentials) {
            mHomeServerConnectionConfig.mCredentials = credentials;
            return this;
        }

        /**
         * @param allowedFingerprint If using SSL, allow server certs that match this fingerprint.
         * @return this builder
         */
        public Builder addAllowedFingerPrint(@Nullable Fingerprint allowedFingerprint) {
            if (allowedFingerprint != null) {
                mHomeServerConnectionConfig.mAllowedFingerprints.add(allowedFingerprint);
            }

            return this;
        }

        /**
         * @param pin If true only allow certs matching given fingerprints, otherwise fallback to
         *            standard X509 checks.
         * @return this builder
         */
        public Builder withPin(boolean pin) {
            mHomeServerConnectionConfig.mPin = pin;

            return this;
        }

        /**
         * @param shouldAcceptTlsExtension
         * @return this builder
         */
        public Builder withShouldAcceptTlsExtensions(boolean shouldAcceptTlsExtension) {
            mHomeServerConnectionConfig.mShouldAcceptTlsExtensions = shouldAcceptTlsExtension;

            return this;
        }

        /**
         * Add an accepted TLS version for TLS connections with the homeserver.
         *
         * @param tlsVersion the tls version to add to the set of TLS versions accepted.
         * @return this builder
         */
        public Builder addAcceptedTlsVersion(@NonNull TlsVersion tlsVersion) {
            if (mHomeServerConnectionConfig.mTlsVersions == null) {
                mHomeServerConnectionConfig.mTlsVersions = new ArrayList<>();
            }

            mHomeServerConnectionConfig.mTlsVersions.add(tlsVersion);

            return this;
        }

        /**
         * Force the usage of TlsVersion. This can be usefull for device on Android version < 20
         *
         * @param forceUsageOfTlsVersions set to true to force the usage of specified TlsVersions (with {@link #addAcceptedTlsVersion(TlsVersion)}
         * @return this builder
         */
        public Builder forceUsageOfTlsVersions(boolean forceUsageOfTlsVersions) {
            mHomeServerConnectionConfig.mForceUsageTlsVersions = forceUsageOfTlsVersions;

            return this;
        }

        /**
         * Add a TLS cipher suite to the list of accepted TLS connections with the homeserver.
         *
         * @param tlsCipherSuite the tls cipher suite to add.
         * @return this builder
         */
        public Builder addAcceptedTlsCipherSuite(@NonNull CipherSuite tlsCipherSuite) {
            if (mHomeServerConnectionConfig.mTlsCipherSuites == null) {
                mHomeServerConnectionConfig.mTlsCipherSuites = new ArrayList<>();
            }

            mHomeServerConnectionConfig.mTlsCipherSuites.add(tlsCipherSuite);

            return this;
        }

        /**
         * Update the anti-virus server URI.
         *
         * @param antivirusServerUri the new anti-virus uri. Can be null
         * @return this builder
         */
        public Builder withAntiVirusServerUri(@Nullable Uri antivirusServerUri) {
            if ((null != antivirusServerUri) && (!"http".equals(antivirusServerUri.getScheme()) && !"https".equals(antivirusServerUri.getScheme()))) {
                throw new RuntimeException("Invalid antivirus server URI: " + antivirusServerUri);
            }

            mHomeServerConnectionConfig.mAntiVirusServerUri = antivirusServerUri;

            return this;
        }

        /**
         * Convenient method to limit the TLS versions and cipher suites for this Builder
         * Ref:
         * - https://www.ssi.gouv.fr/uploads/2017/02/security-recommendations-for-tls_v1.1.pdf
         * - https://developer.android.com/reference/javax/net/ssl/SSLEngine
         *
         * @param tlsLimitations          true to use Tls limitations
         * @param enableCompatibilityMode set to true for Android < 20
         * @return this builder
         */
        public Builder withTlsLimitations(boolean tlsLimitations, boolean enableCompatibilityMode) {
            if (tlsLimitations) {
                withShouldAcceptTlsExtensions(false);

                // Tls versions
                addAcceptedTlsVersion(TlsVersion.TLS_1_2);
                addAcceptedTlsVersion(TlsVersion.TLS_1_3);

                forceUsageOfTlsVersions(enableCompatibilityMode);

                // Cipher suites
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256);
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256);

                if (enableCompatibilityMode) {
                    // Adopt some preceding cipher suites for Android < 20 to be able to negotiate
                    // a TLS session.
                    addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA);
                    addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);
                }
            }

            return this;
        }

        /**
         * @param proxyHostname Proxy Hostname
         * @param proxyPort     Proxy Port
         * @return this builder
         */
        public Builder withProxy(@Nullable String proxyHostname, int proxyPort) {
            mHomeServerConnectionConfig.mProxyHostname = proxyHostname;
            mHomeServerConnectionConfig.mProxyPort = proxyPort;
            return this;
        }

        /**
         * @return the {@link HomeServerConnectionConfig}
         */
        public HomeServerConnectionConfig build() {
            // Check mandatory parameters
            if (mHomeServerConnectionConfig.mHomeServerUri == null) {
                throw new RuntimeException("Homeserver URI not set");
            }

            return mHomeServerConnectionConfig;
        }

    }
}
