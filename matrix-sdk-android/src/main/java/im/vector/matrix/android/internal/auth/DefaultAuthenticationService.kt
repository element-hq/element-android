/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.auth

import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.api.auth.data.*
import im.vector.matrix.android.api.auth.login.LoginWizard
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse
import im.vector.matrix.android.internal.auth.db.PendingSessionData
import im.vector.matrix.android.internal.auth.login.DefaultLoginWizard
import im.vector.matrix.android.internal.auth.registration.DefaultRegistrationWizard
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.launchToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.toCancelable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class DefaultAuthenticationService @Inject constructor(@Unauthenticated
                                                                private val okHttpClient: Lazy<OkHttpClient>,
                                                                private val retrofitFactory: RetrofitFactory,
                                                                private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                                private val sessionParamsStore: SessionParamsStore,
                                                                private val sessionManager: SessionManager,
                                                                private val sessionCreator: SessionCreator,
                                                                private val pendingSessionStore: PendingSessionStore
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

    override fun getSession(sessionParams: SessionParams): Session? {
        return sessionManager.getOrCreateSession(sessionParams)
    }

    override fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<LoginFlowResult>): Cancelable {
        return GlobalScope.launch(coroutineDispatchers.main) {
            pendingSessionStore.delete()

            val result = runCatching {
                getLoginFlowInternal(homeServerConnectionConfig)
            }
            result.fold(
                    {
                        if (it is LoginFlowResult.Success) {
                            // The homeserver exists and up to date, keep the config
                            pendingSessionData = PendingSessionData(homeServerConnectionConfig)
                                    .also { data -> pendingSessionStore.savePendingSessionData(data) }
                        }
                        callback.onSuccess(it)
                    },
                    {
                        callback.onFailure(it)
                    }
            )
        }
                .toCancelable()
    }

    private suspend fun getLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig) = withContext(coroutineDispatchers.io) {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        // First check the homeserver version
        val versions = executeRequest<Versions> {
            apiCall = authAPI.versions()
        }

        if (versions.isSupportedBySdk()) {
            // Get the login flow
            val loginFlowResponse = executeRequest<LoginFlowResponse> {
                apiCall = authAPI.getLoginFlows()
            }
            LoginFlowResult.Success(loginFlowResponse, versions.isLoginAndRegistrationSupportedBySdk())
        } else {
            // Not supported
            LoginFlowResult.OutdatedHomeserver
        }
    }

    override fun getRegistrationWizard(): RegistrationWizard {
        return currentRegistrationWizard
                ?: let {
                    pendingSessionData?.homeServerConnectionConfig?.let {
                        DefaultRegistrationWizard(
                                okHttpClient,
                                retrofitFactory,
                                coroutineDispatchers,
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentRegistrationWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override fun getLoginWizard(): LoginWizard {
        return currentLoginWizard
                ?: let {
                    pendingSessionData?.homeServerConnectionConfig?.let {
                        DefaultLoginWizard(
                                okHttpClient,
                                retrofitFactory,
                                coroutineDispatchers,
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentLoginWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override fun cancelPendingLoginOrRegistration() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        GlobalScope.launch(coroutineDispatchers.main) {
            // Keep only the home sever config
            pendingSessionData?.homeServerConnectionConfig
                    ?.let {
                        pendingSessionStore.savePendingSessionData(PendingSessionData(it))
                    }
                    ?: run {
                        // Should not happen
                        pendingSessionStore.delete()
                    }
        }
    }

    override fun createSessionFromSso(homeServerConnectionConfig: HomeServerConnectionConfig,
                                      credentials: Credentials,
                                      callback: MatrixCallback<Session>): Cancelable {
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            createSessionFromSso(credentials, homeServerConnectionConfig)
        }
    }

    private suspend fun createSessionFromSso(credentials: Credentials,
                                             homeServerConnectionConfig: HomeServerConnectionConfig): Session = withContext(coroutineDispatchers.computation) {
        sessionCreator.createSession(credentials, homeServerConnectionConfig)
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }
}
