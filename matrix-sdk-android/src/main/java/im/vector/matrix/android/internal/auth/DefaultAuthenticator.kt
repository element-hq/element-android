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

import android.util.Patterns
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.auth.data.ThreePidMedium
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class DefaultAuthenticator @Inject constructor(@Unauthenticated
                                                        private val okHttpClient: OkHttpClient,
                                                        private val retrofitFactory: RetrofitFactory,
                                                        private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                        private val sessionParamsStore: SessionParamsStore,
                                                        private val sessionManager: SessionManager
) : Authenticator {

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

    override fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig,
                              login: String,
                              password: String,
                              callback: MatrixCallback<Session>): Cancelable {

        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val sessionOrFailure = runCatching {
                authenticate(homeServerConnectionConfig, login, password)
            }
            sessionOrFailure.foldToCallback(callback)
        }
        return CancelableCoroutine(job)

    }

    private suspend fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig,
                                     login: String,
                                     password: String) = withContext(coroutineDispatchers.io) {

        val authAPI = buildAuthAPI(homeServerConnectionConfig)
        val loginParams = if (Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
            PasswordLoginParams.thirdPartyIdentifier(ThreePidMedium.EMAIL, login, password, "Mobile")
        } else {
            PasswordLoginParams.userIdentifier(login, password, "Mobile")
        }
        val credentials = executeRequest<Credentials> {
            apiCall = authAPI.login(loginParams)
        }
        val sessionParams = SessionParams(credentials, homeServerConnectionConfig)
        sessionParamsStore.save(sessionParams)
        sessionManager.getOrCreateSession(sessionParams)
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }


}