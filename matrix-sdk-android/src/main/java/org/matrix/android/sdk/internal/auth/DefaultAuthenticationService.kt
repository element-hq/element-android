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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.auth.data.LoginFlowResponse
import org.matrix.android.sdk.internal.auth.data.RiotConfig
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
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.toCancelable
import org.matrix.android.sdk.internal.wellknown.GetWellknownTask
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal class DefaultAuthenticationService @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore,
        private val getWellknownTask: GetWellknownTask,
        private val directLoginTask: DirectLoginTask,
        private val taskExecutor: TaskExecutor
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

    override fun getLoginFlowOfSession(sessionId: String, callback: MatrixCallback<LoginFlowResult>): Cancelable {
        val homeServerConnectionConfig = sessionParamsStore.get(sessionId)?.homeServerConnectionConfig

        return if (homeServerConnectionConfig == null) {
            callback.onFailure(IllegalStateException("Session not found"))
            NoOpCancellable
        } else {
            getLoginFlow(homeServerConnectionConfig, callback)
        }
    }

    override fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<LoginFlowResult>): Cancelable {
        pendingSessionData = null

        return taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            pendingSessionStore.delete()

            val result = runCatching {
                getLoginFlowInternal(homeServerConnectionConfig)
            }
            result.fold(
                    {
                        if (it is LoginFlowResult.Success) {
                            // The homeserver exists and up to date, keep the config
                            // Homeserver url may have been changed, if it was a Riot url
                            val alteredHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                                    homeServerUri = Uri.parse(it.homeServerUrl)
                            )

                            pendingSessionData = PendingSessionData(alteredHomeServerConnectionConfig)
                                    .also { data -> pendingSessionStore.savePendingSessionData(data) }
                        }
                        callback.onSuccess(it)
                    },
                    {
                        if (it is UnrecognizedCertificateException) {
                            callback.onFailure(Failure.UnrecognizedCertificateFailure(homeServerConnectionConfig.homeServerUri.toString(), it.fingerprint))
                        } else {
                            callback.onFailure(it)
                        }
                    }
            )
        }
                .toCancelable()
    }

    private suspend fun getLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        return withContext(coroutineDispatchers.io) {
            val authAPI = buildAuthAPI(homeServerConnectionConfig)

            // First check the homeserver version
            runCatching {
                executeRequest<Versions>(null) {
                    apiCall = authAPI.versions()
                }
            }
                    .map { versions ->
                        // Ok, it seems that the homeserver url is valid
                        getLoginFlowResult(authAPI, versions, homeServerConnectionConfig.homeServerUri.toString())
                    }
                    .fold(
                            {
                                it
                            },
                            {
                                if (it is Failure.OtherServerError
                                        && it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                    // It's maybe a Riot url?
                                    getRiotDomainLoginFlowInternal(homeServerConnectionConfig)
                                } else {
                                    throw it
                                }
                            }
                    )
        }
    }

    private suspend fun getRiotDomainLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        val domain = homeServerConnectionConfig.homeServerUri.host
                ?: return getRiotLoginFlowInternal(homeServerConnectionConfig)

        // Ok, try to get the config.domain.json file of a RiotWeb client
        return runCatching {
            executeRequest<RiotConfig>(null) {
                apiCall = authAPI.getRiotConfigDomain(domain)
            }
        }
                .map { riotConfig ->
                    onRiotConfigRetrieved(homeServerConnectionConfig, riotConfig)
                }
                .fold(
                        {
                            it
                        },
                        {
                            if (it is Failure.OtherServerError
                                    && it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                // Try with config.json
                                getRiotLoginFlowInternal(homeServerConnectionConfig)
                            } else {
                                throw it
                            }
                        }
                )
    }

    private suspend fun getRiotLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        // Ok, try to get the config.json file of a RiotWeb client
        return runCatching {
            executeRequest<RiotConfig>(null) {
                apiCall = authAPI.getRiotConfig()
            }
        }
                .map { riotConfig ->
                    onRiotConfigRetrieved(homeServerConnectionConfig, riotConfig)
                }
                .fold(
                        {
                            it
                        },
                        {
                            if (it is Failure.OtherServerError
                                    && it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                // Try with wellknown
                                getWellknownLoginFlowInternal(homeServerConnectionConfig)
                            } else {
                                throw it
                            }
                        }
                )
    }

    private suspend fun onRiotConfigRetrieved(homeServerConnectionConfig: HomeServerConnectionConfig, riotConfig: RiotConfig): LoginFlowResult {
        val defaultHomeServerUrl = riotConfig.getPreferredHomeServerUrl()
        if (defaultHomeServerUrl?.isNotEmpty() == true) {
            // Ok, good sign, we got a default hs url
            val newHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                    homeServerUri = Uri.parse(defaultHomeServerUrl)
            )

            val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

            val versions = executeRequest<Versions>(null) {
                apiCall = newAuthAPI.versions()
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

        // Create a fake userId, for the getWellknown task
        val fakeUserId = "@alice:$domain"
        val wellknownResult = getWellknownTask.execute(GetWellknownTask.Params(fakeUserId, homeServerConnectionConfig))

        return when (wellknownResult) {
            is WellknownResult.Prompt -> {
                val newHomeServerConnectionConfig = homeServerConnectionConfig.copy(
                        homeServerUri = Uri.parse(wellknownResult.homeServerUrl),
                        identityServerUri = wellknownResult.identityServerUrl?.let { Uri.parse(it) }
                )

                val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

                val versions = executeRequest<Versions>(null) {
                    apiCall = newAuthAPI.versions()
                }

                getLoginFlowResult(newAuthAPI, versions, wellknownResult.homeServerUrl)
            }
            else                      -> throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)
        }
    }

    private suspend fun getLoginFlowResult(authAPI: AuthAPI, versions: Versions, homeServerUrl: String): LoginFlowResult {
        // Get the login flow
        val loginFlowResponse = executeRequest<LoginFlowResponse>(null) {
            apiCall = authAPI.getLoginFlows()
        }
        return LoginFlowResult.Success(
                loginFlowResponse.flows.orEmpty().mapNotNull { it.type },
                loginFlowResponse.flows.orEmpty().firstOrNull { it.type == LoginFlowTypes.SSO }?.ssoIdentityProvider,
                versions.isLoginAndRegistrationSupportedBySdk(),
                homeServerUrl,
                !versions.isSupportedBySdk()
        )
    }

    override fun getRegistrationWizard(): RegistrationWizard {
        return currentRegistrationWizard
                ?: let {
                    pendingSessionData?.homeServerConnectionConfig?.let {
                        DefaultRegistrationWizard(
                                buildClient(it),
                                retrofitFactory,
                                coroutineDispatchers,
                                sessionCreator,
                                pendingSessionStore,
                                taskExecutor.executorScope
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
                                buildClient(it),
                                retrofitFactory,
                                coroutineDispatchers,
                                sessionCreator,
                                pendingSessionStore,
                                taskExecutor.executorScope
                        ).also {
                            currentLoginWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override fun cancelPendingLoginOrRegistration() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        // Keep only the home sever config
        // Update the local pendingSessionData synchronously
        pendingSessionData = pendingSessionData?.homeServerConnectionConfig
                ?.let { PendingSessionData(it) }
                .also {
                    taskExecutor.executorScope.launch(coroutineDispatchers.main) {
                        if (it == null) {
                            // Should not happen
                            pendingSessionStore.delete()
                        } else {
                            pendingSessionStore.savePendingSessionData(it)
                        }
                    }
                }
    }

    override fun reset() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        pendingSessionData = null

        taskExecutor.executorScope.launch(coroutineDispatchers.main) {
            pendingSessionStore.delete()
        }
    }

    override fun createSessionFromSso(homeServerConnectionConfig: HomeServerConnectionConfig,
                                      credentials: Credentials,
                                      callback: MatrixCallback<Session>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            createSessionFromSso(credentials, homeServerConnectionConfig)
        }
    }

    override fun getWellKnownData(matrixId: String,
                                  homeServerConnectionConfig: HomeServerConnectionConfig?,
                                  callback: MatrixCallback<WellknownResult>): Cancelable {
        return getWellknownTask
                .configureWith(GetWellknownTask.Params(matrixId, homeServerConnectionConfig)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun directAuthentication(homeServerConnectionConfig: HomeServerConnectionConfig,
                                      matrixId: String,
                                      password: String,
                                      initialDeviceName: String,
                                      callback: MatrixCallback<Session>): Cancelable {
        return directLoginTask
                .configureWith(DirectLoginTask.Params(homeServerConnectionConfig, matrixId, password, initialDeviceName)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    private suspend fun createSessionFromSso(credentials: Credentials,
                                             homeServerConnectionConfig: HomeServerConnectionConfig): Session = withContext(coroutineDispatchers.computation) {
        sessionCreator.createSession(credentials, homeServerConnectionConfig)
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(buildClient(homeServerConnectionConfig), homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(homeServerConnectionConfig)
                .build()
    }
}
