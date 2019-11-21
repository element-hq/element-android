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
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.registration.RegisterThreePid
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.auth.data.ThreePidMedium
import im.vector.matrix.android.internal.auth.registration.AddThreePidRegistrationParams
import im.vector.matrix.android.internal.auth.registration.AddThreePidRegistrationResponse
import im.vector.matrix.android.internal.auth.registration.RegisterAddThreePidTask
import im.vector.matrix.android.internal.auth.signin.ResetPasswordMailConfirmed
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
import java.util.*
import javax.inject.Inject

// Container to store the data when a reset password is in the email validation step
internal data class ResetPasswordData(
        val newPassword: String,
        val addThreePidRegistrationResponse: AddThreePidRegistrationResponse
)

internal class DefaultAuthenticator @Inject constructor(@Unauthenticated
                                                        private val okHttpClient: Lazy<OkHttpClient>,
                                                        private val retrofitFactory: RetrofitFactory,
                                                        private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                        private val sessionParamsStore: SessionParamsStore,
                                                        private val sessionManager: SessionManager
) : Authenticator {
    private var clientSecret = UUID.randomUUID().toString()
    private var sendAttempt = 0

    private var resetPasswordData: ResetPasswordData? = null

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

    override fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<LoginFlowResponse>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val result = runCatching {
                getLoginFlowInternal(homeServerConnectionConfig)
            }
            result.foldToCallback(callback)
        }
        return CancelableCoroutine(job)
    }

    override fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig,
                              login: String,
                              password: String,
                              deviceName: String,
                              callback: MatrixCallback<Session>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val sessionOrFailure = runCatching {
                authenticate(homeServerConnectionConfig, login, password, deviceName)
            }
            sessionOrFailure.foldToCallback(callback)
        }
        return CancelableCoroutine(job)
    }

    private suspend fun getLoginFlowInternal(homeServerConnectionConfig: HomeServerConnectionConfig) = withContext(coroutineDispatchers.io) {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        executeRequest<LoginFlowResponse> {
            apiCall = authAPI.getLoginFlows()
        }
    }

    private suspend fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig,
                                     login: String,
                                     password: String,
                                     deviceName: String) = withContext(coroutineDispatchers.io) {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)
        val loginParams = if (Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
            PasswordLoginParams.thirdPartyIdentifier(ThreePidMedium.EMAIL, login, password, deviceName)
        } else {
            PasswordLoginParams.userIdentifier(login, password, deviceName)
        }
        val credentials = executeRequest<Credentials> {
            apiCall = authAPI.login(loginParams)
        }
        val sessionParams = SessionParams(credentials, homeServerConnectionConfig)
        sessionParamsStore.save(sessionParams)
        sessionManager.getOrCreateSession(sessionParams)
    }

    override fun createSessionFromSso(credentials: Credentials,
                                      homeServerConnectionConfig: HomeServerConnectionConfig,
                                      callback: MatrixCallback<Session>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val sessionOrFailure = runCatching {
                createSessionFromSso(credentials, homeServerConnectionConfig)
            }
            sessionOrFailure.foldToCallback(callback)
        }
        return CancelableCoroutine(job)
    }

    private suspend fun createSessionFromSso(credentials: Credentials,
                                             homeServerConnectionConfig: HomeServerConnectionConfig): Session = withContext(coroutineDispatchers.computation) {
        val sessionParams = SessionParams(credentials, homeServerConnectionConfig)
        sessionParamsStore.save(sessionParams)
        sessionManager.getOrCreateSession(sessionParams)
    }

    override fun resetPassword(homeServerConnectionConfig: HomeServerConnectionConfig, email: String, newPassword: String, callback: MatrixCallback<Unit>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val result = runCatching {
                resetPasswordInternal(homeServerConnectionConfig, email, newPassword)
            }
            result.foldToCallback(callback)
        }
        return CancelableCoroutine(job)
    }

    private suspend fun resetPasswordInternal(homeServerConnectionConfig: HomeServerConnectionConfig, email: String, newPassword: String) {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        val param = RegisterAddThreePidTask.Params(
                RegisterThreePid.Email(email),
                clientSecret,
                sendAttempt++
        )

        val result = executeRequest<AddThreePidRegistrationResponse> {
            apiCall = authAPI.resetPassword(AddThreePidRegistrationParams.from(param))
        }

        resetPasswordData = ResetPasswordData(newPassword, result)
    }

    override fun resetPasswordMailConfirmed(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<Unit>): Cancelable {
        val safeResetPasswordData = resetPasswordData ?: run {
            callback.onFailure(IllegalStateException("developer error, no reset password in progress"))
            return NoOpCancellable
        }
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val result = runCatching {
                resetPasswordMailConfirmedInternal(homeServerConnectionConfig, safeResetPasswordData)
            }
            result.foldToCallback(callback)
        }
        return CancelableCoroutine(job)
    }

    private suspend fun resetPasswordMailConfirmedInternal(homeServerConnectionConfig: HomeServerConnectionConfig, resetPasswordData: ResetPasswordData) {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        val param = ResetPasswordMailConfirmed.create(
                clientSecret,
                resetPasswordData.addThreePidRegistrationResponse.sid,
                resetPasswordData.newPassword
        )

        executeRequest<Unit> {
            apiCall = authAPI.resetPasswordMailConfirmed(param)
        }

        // Set to null?
        // resetPasswordData = null
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }
}
