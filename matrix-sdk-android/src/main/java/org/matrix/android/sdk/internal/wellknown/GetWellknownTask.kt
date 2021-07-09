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

package org.matrix.android.sdk.internal.wellknown

import android.util.MalformedJsonException
import dagger.Lazy
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.WellKnown
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.httpclient.addSocketFactory
import org.matrix.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.matrix.android.sdk.internal.session.homeserver.CapabilitiesAPI
import org.matrix.android.sdk.internal.session.identity.IdentityAuthAPI
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.isValidUrl
import okhttp3.OkHttpClient
import java.io.EOFException
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface GetWellknownTask : Task<GetWellknownTask.Params, WellknownResult> {
    data class Params(
            /**
             * domain, for instance "matrix.org"
             * the URL will be https://{domain}/.well-known/matrix/client
             */
            val domain: String,
            val homeServerConnectionConfig: HomeServerConnectionConfig?
    )
}

/**
 * Inspired from AutoDiscovery class from legacy Matrix Android SDK
 */
internal class DefaultGetWellknownTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory
) : GetWellknownTask {

    override suspend fun execute(params: GetWellknownTask.Params): WellknownResult {
        val client = buildClient(params.homeServerConnectionConfig)
        return findClientConfig(params.domain, client)
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig?): OkHttpClient {
        return if (homeServerConnectionConfig != null) {
            okHttpClient.get()
                    .newBuilder()
                    .addSocketFactory(homeServerConnectionConfig)
                    .build()
        } else {
            okHttpClient.get()
        }
    }

    /**
     * Find client config
     *
     * - Do the .well-known request
     * - validate homeserver url and identity server url if provide in .well-known result
     * - return action and .well-known data
     *
     * @param domain: homeserver domain, deduced from mx userId (ex: "matrix.org" from userId "@user:matrix.org")
     */
    private suspend fun findClientConfig(domain: String, client: OkHttpClient): WellknownResult {
        val wellKnownAPI = retrofitFactory.create(client, "https://dummy.org")
                .create(WellKnownAPI::class.java)

        return try {
            val wellKnown = executeRequest(null) {
                wellKnownAPI.getWellKnown(domain)
            }

            // Success
            val homeServerBaseUrl = wellKnown.homeServer?.baseURL
            if (homeServerBaseUrl.isNullOrBlank()) {
                WellknownResult.FailPrompt(null, null)
            } else {
                if (homeServerBaseUrl.isValidUrl()) {
                    // Check that HS is a real one
                    validateHomeServer(homeServerBaseUrl, wellKnown, client)
                } else {
                    WellknownResult.FailError
                }
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is UnrecognizedCertificateException        -> {
                    throw Failure.UnrecognizedCertificateFailure(
                            "https://$domain",
                            throwable.fingerprint
                    )
                }
                is Failure.NetworkConnection               -> {
                    WellknownResult.Ignore
                }
                is Failure.OtherServerError                -> {
                    when (throwable.httpCode) {
                        HttpsURLConnection.HTTP_NOT_FOUND -> WellknownResult.Ignore
                        else                              -> WellknownResult.FailPrompt(null, null)
                    }
                }
                is MalformedJsonException, is EOFException -> {
                    WellknownResult.FailPrompt(null, null)
                }
                else                                       -> {
                    throw throwable
                }
            }
        }
    }

    /**
     * Return true if home server is valid, and (if applicable) if identity server is pingable
     */
    private suspend fun validateHomeServer(homeServerBaseUrl: String, wellKnown: WellKnown, client: OkHttpClient): WellknownResult {
        val capabilitiesAPI = retrofitFactory.create(client, homeServerBaseUrl)
                .create(CapabilitiesAPI::class.java)

        try {
            executeRequest(null) {
                capabilitiesAPI.ping()
            }
        } catch (throwable: Throwable) {
            return WellknownResult.FailError
        }

        return if (wellKnown.identityServer == null) {
            // No identity server
            WellknownResult.Prompt(homeServerBaseUrl, null, wellKnown)
        } else {
            // if m.identity_server is present it must be valid
            val identityServerBaseUrl = wellKnown.identityServer.baseURL
            if (identityServerBaseUrl.isNullOrBlank()) {
                WellknownResult.FailError
            } else {
                if (identityServerBaseUrl.isValidUrl()) {
                    if (validateIdentityServer(identityServerBaseUrl, client)) {
                        // All is ok
                        WellknownResult.Prompt(homeServerBaseUrl, identityServerBaseUrl, wellKnown)
                    } else {
                        WellknownResult.FailPrompt(homeServerBaseUrl, wellKnown)
                    }
                } else {
                    WellknownResult.FailError
                }
            }
        }
    }

    /**
     * Return true if identity server is pingable
     */
    private suspend fun validateIdentityServer(identityServerBaseUrl: String, client: OkHttpClient): Boolean {
        val identityPingApi = retrofitFactory.create(client, identityServerBaseUrl)
                .create(IdentityAuthAPI::class.java)

        return try {
            executeRequest(null) {
                identityPingApi.ping()
            }

            true
        } catch (throwable: Throwable) {
            false
        }
    }

    /**
     * Try to get an identity server URL from a home server URL, using a .wellknown request
     */
    /*
    fun getIdentityServer(homeServerUrl: String, callback: ApiCallback<String?>) {
        if (homeServerUrl.startsWith("https://")) {
            wellKnownRestClient.getWellKnown(homeServerUrl.substring("https://".length),
                    object : SimpleApiCallback<WellKnown>(callback) {
                        override fun onSuccess(info: WellKnown) {
                            callback.onSuccess(info.identityServer?.baseURL)
                        }
                    })
        } else {
            callback.onUnexpectedError(InvalidParameterException("malformed url"))
        }
    }

    fun getServerPreferredIntegrationManagers(homeServerUrl: String, callback: ApiCallback<List<WellKnownManagerConfig>>) {
        if (homeServerUrl.startsWith("https://")) {
            wellKnownRestClient.getWellKnown(homeServerUrl.substring("https://".length),
                    object : SimpleApiCallback<WellKnown>(callback) {
                        override fun onSuccess(info: WellKnown) {
                            callback.onSuccess(info.getIntegrationManagers())
                        }
                    })
        } else {
            callback.onUnexpectedError(InvalidParameterException("malformed url"))
        }
    }
     */
}
