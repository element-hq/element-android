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

package im.vector.matrix.android.internal.auth.wellknown

import android.util.MalformedJsonException
import dagger.Lazy
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.auth.data.WellKnown
import im.vector.matrix.android.api.auth.wellknown.WellknownResult
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.identity.IdentityPingApi
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.homeserver.CapabilitiesAPI
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.isValidUrl
import okhttp3.OkHttpClient
import java.io.EOFException
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface GetWellknownTask : Task<GetWellknownTask.Params, WellknownResult> {
    data class Params(
            val matrixId: String
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
        if (!MatrixPatterns.isUserId(params.matrixId)) {
            return WellknownResult.InvalidMatrixId
        }

        val homeServerDomain = params.matrixId.substringAfter(":")

        return findClientConfig(homeServerDomain)
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
    private suspend fun findClientConfig(domain: String): WellknownResult {
        val wellKnownAPI = retrofitFactory.create(okHttpClient, "https://dummy.org")
                .create(WellKnownAPI::class.java)

        return try {
            val wellKnown = executeRequest<WellKnown>(null) {
                apiCall = wellKnownAPI.getWellKnown(domain)
            }

            // Success
            val homeServerBaseUrl = wellKnown.homeServer?.baseURL
            if (homeServerBaseUrl.isNullOrBlank()) {
                WellknownResult.FailPrompt
            } else {
                if (homeServerBaseUrl.isValidUrl()) {
                    // Check that HS is a real one
                    validateHomeServer(homeServerBaseUrl, wellKnown)
                } else {
                    WellknownResult.FailError
                }
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is Failure.NetworkConnection               -> {
                    WellknownResult.Ignore
                }
                is Failure.OtherServerError                -> {
                    when (throwable.httpCode) {
                        HttpsURLConnection.HTTP_NOT_FOUND -> WellknownResult.Ignore
                        else                              -> WellknownResult.FailPrompt
                    }
                }
                is MalformedJsonException, is EOFException -> {
                    WellknownResult.FailPrompt
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
    private suspend fun validateHomeServer(homeServerBaseUrl: String, wellKnown: WellKnown): WellknownResult {
        val capabilitiesAPI = retrofitFactory.create(okHttpClient, homeServerBaseUrl)
                .create(CapabilitiesAPI::class.java)

        try {
            executeRequest<Unit>(null) {
                apiCall = capabilitiesAPI.getVersions()
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
                    if (validateIdentityServer(identityServerBaseUrl)) {
                        // All is ok
                        WellknownResult.Prompt(homeServerBaseUrl, identityServerBaseUrl, wellKnown)
                    } else {
                        WellknownResult.FailError
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
    private suspend fun validateIdentityServer(identityServerBaseUrl: String): Boolean {
        val identityPingApi = retrofitFactory.create(okHttpClient, identityServerBaseUrl)
                .create(IdentityPingApi::class.java)

        return try {
            executeRequest<Unit>(null) {
                apiCall = identityPingApi.ping()
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
