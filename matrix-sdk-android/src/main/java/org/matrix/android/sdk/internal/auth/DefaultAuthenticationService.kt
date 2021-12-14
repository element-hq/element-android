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

package org.matrix.android.sdk.internal.auth

import android.net.Uri
import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixIdFailure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.appendParamToUrl
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.auth.data.WebClientConfig
import org.matrix.android.sdk.internal.auth.db.PendingSessionData
import org.matrix.android.sdk.internal.auth.login.DefaultLoginWizard
import org.matrix.android.sdk.internal.auth.login.DirectLoginTask
import org.matrix.android.sdk.internal.auth.registration.DefaultRegistrationWizard
import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import org.matrix.android.sdk.internal.auth.version.isSupportedBySdk
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.httpclient.addSocketFactory
import org.matrix.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.matrix.android.sdk.internal.wellknown.GetWellknownTask
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal class DefaultAuthenticationService @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore,
        private val getWellknownTask: GetWellknownTask,
        private val directLoginTask: DirectLoginTask
) : AuthenticationService {

    private var pendingSessionData: PendingSessionData? = pendingSessionStore.getPendingSessionData()

    private var currentLoginWizard: LoginWizard? = null
    private var currentRegistrationWizard: RegistrationWizard? = null

    override fun hasAuthenticatedSessions(): Boolean {
        return sessionParamsStore.getLast() != null
    }

    override fun getLastAuthenticatedSession(): Session? {
        val sessionParams = sessionParamsStore.getLast()
        return sessionParams?.let {
            sessionManager.getOrCreateSession(it)
        }
    }

    override suspend fun getLoginFlowOfSession(sessionId: String): LoginFlowResult {
        val homeServerConnectionConfig = sessionParamsStore.get(sessionId)?.homeServerConnectionConfig
                ?: throw IllegalStateException("Session not found")

        return getLoginFlow(homeServerConnectionConfig)
    }

    override fun getSsoUrl(redirectUrl: String, deviceId: String?, providerId: String?): String? {
        val homeServerUrlBase = getHomeServerUrlBase() ?: return null

        return buildString {
            append(homeServerUrlBase)
            append(SSO_REDIRECT_PATH)
            if (providerId != null) {
                append("/$providerId")
            }
            // Set the redirect url
            appendParamToUrl(SSO_REDIRECT_URL_PARAM, redirectUrl)
            deviceId?.takeIf { it.isNotBlank() }?.let {
                // But https://github.com/matrix-org/synapse/issues/5755
                appendParamToUrl("device_id", it)
            }
        }
    }

    override fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String? {
        val homeServerUrlBase = getHomeServerUrlBase() ?: return null

        return buildString {
            append(homeServerUrlBase)
            if (forSignIn) {
                append(LOGIN_FALLBACK_PATH)
                deviceId?.takeIf { it.isNotBlank() }?.let {
                    // But https://github.com/matrix-org/synapse/issues/5755
                    appendParamToUrl("device_id", it)
                }
            } else {
                // For sign up
                append(REGISTER_FALLBACK_PATH)
            }
        }
    }

    private fun getHomeServerUrlBase(): String? {
        return pendingSessionData
                ?.homeServerConnectionConfig
                ?.homeServerUriBase
                ?.toString()
                ?.trim { it == '/' }
    }

    /**
     * This is the entry point of the authentication service.
     * homeServerConnectionConfig contains a homeserver URL probably entered by the user, which can be a
     * valid homeserver API url, the url of Element Web, or anything else.
     */
    override suspend fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        pendingSessionData = null

        pendingSessionStore.delete()

        val result = runCatching {
            getLoginFlowInternal(homeServerConnectionConfig)
        }
        return result.fold(
                {
                    // The homeserver exists and up to date, keep the config
                    // Homeserver url may have been changed, if it was a Web client url
                    val alteredHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                            homeServerUriBase = Uri.parse(it.homeServerUrl)
                    )

                    pendingSessionData = PendingSessionData(alteredHomeServerConnectionConfig)
                            .also { data -> pendingSessionStore.savePendingSessionData(data) }
                    it
                },
                {
                    if (it is UnrecognizedCertificateException) {
                        throw Failure.UnrecognizedCertificateFailure(homeServerConnectionConfig.homeServerUriBase.toString(), it.fingerprint)
                    } else {
                        throw it
                    }
                }
        )
    }

    private suspend fun getLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        // First check if there is a well-known file
        return try {
            getWellknownLoginFlowInternal(homeServerConnectionConfig)
        } catch (failure: Throwable) {
            if (failure is Failure.OtherServerError &&
                    failure.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // 404, no well-known data, try direct access to the API
                // First check the homeserver version
                return runCatching {
                    executeRequest(null) {
                        authAPI.versions()
                    }
                }
                        .map { versions ->
                            // Ok, it seems that the homeserver url is valid
                            getLoginFlowResult(authAPI, versions, homeServerConnectionConfig.homeServerUriBase.toString())
                        }
                        .fold(
                                {
                                    it
                                },
                                {
                                    if (it is Failure.OtherServerError &&
                                            it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                        // It's maybe a Web client url?
                                        getWebClientDomainLoginFlowInternal(homeServerConnectionConfig)
                                    } else {
                                        throw it
                                    }
                                }
                        )
            } else {
                throw failure
            }
        }
    }

    private suspend fun getWebClientDomainLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        val domain = homeServerConnectionConfig.homeServerUri.host
                ?: return getWebClientLoginFlowInternal(homeServerConnectionConfig)

        // Ok, try to get the config.domain.json file of a Web client
        return runCatching {
            executeRequest(null) {
                authAPI.getWebClientConfigDomain(domain)
            }
        }
                .map { webClientConfig ->
                    onWebClientConfigRetrieved(homeServerConnectionConfig, webClientConfig)
                }
                .fold(
                        {
                            it
                        },
                        {
                            if (it is Failure.OtherServerError &&
                                    it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                // Try with config.json
                                getWebClientLoginFlowInternal(homeServerConnectionConfig)
                            } else {
                                throw it
                            }
                        }
                )
    }

    private suspend fun getWebClientLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        // Ok, try to get the config.json file of a Web client
        return executeRequest(null) {
            authAPI.getWebClientConfig()
        }
                .let { webClientConfig ->
                    onWebClientConfigRetrieved(homeServerConnectionConfig, webClientConfig)
                }
    }

    private suspend fun onWebClientConfigRetrieved(homeServerConnectionConfig: HomeServerConnectionConfig, webClientConfig: WebClientConfig): LoginFlowResult {
        val defaultHomeServerUrl = webClientConfig.getPreferredHomeServerUrl()
        if (defaultHomeServerUrl?.isNotEmpty() == true) {
            // Ok, good sign, we got a default hs url
            val newHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                    homeServerUriBase = Uri.parse(defaultHomeServerUrl)
            )

            val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

            val versions = executeRequest(null) {
                newAuthAPI.versions()
            }

            return getLoginFlowResult(newAuthAPI, versions, defaultHomeServerUrl)
        } else {
            // Config exists, but there is no default homeserver url (ex: https://riot.im/app)
            throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)
        }
    }

    private suspend fun getWellknownLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val domain = homeServerConnectionConfig.homeServerUri.host
                ?: throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)

        val wellknownResult = getWellknownTask.execute(GetWellknownTask.Params(domain, homeServerConnectionConfig))

        return when (wellknownResult) {
            is WellknownResult.Prompt -> {
                val newHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                        homeServerUriBase = Uri.parse(wellknownResult.homeServerUrl),
                        identityServerUri = wellknownResult.identityServerUrl?.let { Uri.parse(it) } ?: homeServerConnectionConfig.identityServerUri
                )

                val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

                val versions = executeRequest(null) {
                    newAuthAPI.versions()
                }

                getLoginFlowResult(newAuthAPI, versions, wellknownResult.homeServerUrl)
            }
            else                      -> throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)
        }
    }

    private suspend fun getLoginFlowResult(authAPI: AuthAPI, versions: Versions, homeServerUrl: String): LoginFlowResult {
        // Get the login flow
        val loginFlowResponse = executeRequest(null) {
            authAPI.getLoginFlows()
        }
        return LoginFlowResult(
                supportedLoginTypes = loginFlowResponse.flows.orEmpty().mapNotNull { it.type },
                ssoIdentityProviders = loginFlowResponse.flows.orEmpty().firstOrNull { it.type == LoginFlowTypes.SSO }?.ssoIdentityProvider,
                isLoginAndRegistrationSupported = versions.isLoginAndRegistrationSupportedBySdk(),
                homeServerUrl = homeServerUrl,
                isOutdatedHomeserver = !versions.isSupportedBySdk()
        )
    }

    override fun getRegistrationWizard(): RegistrationWizard {
        return currentRegistrationWizard
                ?: let {
                    pendingSessionData?.homeServerConnectionConfig?.let {
                        DefaultRegistrationWizard(
                                buildAuthAPI(it),
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentRegistrationWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override val isRegistrationStarted: Boolean
        get() = currentRegistrationWizard?.isRegistrationStarted == true

    override fun getLoginWizard(): LoginWizard {
        return currentLoginWizard
                ?: let {
                    pendingSessionData?.homeServerConnectionConfig?.let {
                        DefaultLoginWizard(
                                buildAuthAPI(it),
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentLoginWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override suspend fun cancelPendingLoginOrRegistration() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        // Keep only the home sever config
        // Update the local pendingSessionData synchronously
        pendingSessionData = pendingSessionData?.homeServerConnectionConfig
                ?.let { PendingSessionData(it) }
                .also {
                    if (it == null) {
                        // Should not happen
                        pendingSessionStore.delete()
                    } else {
                        pendingSessionStore.savePendingSessionData(it)
                    }
                }
    }

    override suspend fun reset() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        pendingSessionData = null

        pendingSessionStore.delete()
    }

    override suspend fun createSessionFromSso(homeServerConnectionConfig: HomeServerConnectionConfig,
                                              credentials: Credentials): Session {
        return sessionCreator.createSession(credentials, homeServerConnectionConfig)
    }

    override suspend fun getWellKnownData(matrixId: String,
                                          homeServerConnectionConfig: HomeServerConnectionConfig?): WellknownResult {
        if (!MatrixPatterns.isUserId(matrixId)) {
            throw MatrixIdFailure.InvalidMatrixId
        }

        return getWellknownTask.execute(GetWellknownTask.Params(
                domain = matrixId.getDomain(),
                homeServerConnectionConfig = homeServerConnectionConfig)
        )
    }

    override suspend fun directAuthentication(homeServerConnectionConfig: HomeServerConnectionConfig,
                                              matrixId: String,
                                              password: String,
                                              initialDeviceName: String,
                                              deviceId: String?): Session {
        return directLoginTask.execute(DirectLoginTask.Params(
                homeServerConnectionConfig = homeServerConnectionConfig,
                userId = matrixId,
                password = password,
                deviceName = initialDeviceName,
                deviceId = deviceId
        ))
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(buildClient(homeServerConnectionConfig), homeServerConnectionConfig.homeServerUriBase.toString())
        return retrofit.create(AuthAPI::class.java)
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(homeServerConnectionConfig)
                .build()
    }
}
