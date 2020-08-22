/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth.login

import android.util.Patterns
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.auth.PendingSessionStore
import org.matrix.android.sdk.internal.auth.SessionCreator
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.auth.data.ThreePidMedium
import org.matrix.android.sdk.internal.auth.data.TokenLoginParams
import org.matrix.android.sdk.internal.auth.db.PendingSessionData
import org.matrix.android.sdk.internal.auth.registration.AddThreePidRegistrationParams
import org.matrix.android.sdk.internal.auth.registration.AddThreePidRegistrationResponse
import org.matrix.android.sdk.internal.auth.registration.RegisterAddThreePidTask
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

internal class DefaultLoginWizard(
        okHttpClient: OkHttpClient,
        retrofitFactory: RetrofitFactory,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore,
        private val coroutineScope: CoroutineScope
) : LoginWizard {

    private var pendingSessionData: PendingSessionData = pendingSessionStore.getPendingSessionData() ?: error("Pending session data should exist here")

    private val authAPI = retrofitFactory.create(okHttpClient, pendingSessionData.homeServerConnectionConfig.homeServerUri.toString())
            .create(AuthAPI::class.java)

    override fun login(login: String,
                       password: String,
                       deviceName: String,
                       callback: MatrixCallback<Session>): Cancelable {
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            loginInternal(login, password, deviceName)
        }
    }

    /**
     * Ref: https://matrix.org/docs/spec/client_server/latest#handling-the-authentication-endpoint
     */
    override fun loginWithToken(loginToken: String, callback: MatrixCallback<Session>): Cancelable {
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            val loginParams = TokenLoginParams(
                    token = loginToken
            )
            val credentials = executeRequest<Credentials>(null) {
                apiCall = authAPI.login(loginParams)
            }

            sessionCreator.createSession(credentials, pendingSessionData.homeServerConnectionConfig)
        }
    }

    private suspend fun loginInternal(login: String,
                                      password: String,
                                      deviceName: String) = withContext(coroutineDispatchers.computation) {
        val loginParams = if (Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
            PasswordLoginParams.thirdPartyIdentifier(ThreePidMedium.EMAIL, login, password, deviceName)
        } else {
            PasswordLoginParams.userIdentifier(login, password, deviceName)
        }
        val credentials = executeRequest<Credentials>(null) {
            apiCall = authAPI.login(loginParams)
        }

        sessionCreator.createSession(credentials, pendingSessionData.homeServerConnectionConfig)
    }

    override fun resetPassword(email: String, newPassword: String, callback: MatrixCallback<Unit>): Cancelable {
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            resetPasswordInternal(email, newPassword)
        }
    }

    private suspend fun resetPasswordInternal(email: String, newPassword: String) {
        val param = RegisterAddThreePidTask.Params(
                RegisterThreePid.Email(email),
                pendingSessionData.clientSecret,
                pendingSessionData.sendAttempt
        )

        pendingSessionData = pendingSessionData.copy(sendAttempt = pendingSessionData.sendAttempt + 1)
                .also { pendingSessionStore.savePendingSessionData(it) }

        val result = executeRequest<AddThreePidRegistrationResponse>(null) {
            apiCall = authAPI.resetPassword(AddThreePidRegistrationParams.from(param))
        }

        pendingSessionData = pendingSessionData.copy(resetPasswordData = ResetPasswordData(newPassword, result))
                .also { pendingSessionStore.savePendingSessionData(it) }
    }

    override fun resetPasswordMailConfirmed(callback: MatrixCallback<Unit>): Cancelable {
        val safeResetPasswordData = pendingSessionData.resetPasswordData ?: run {
            callback.onFailure(IllegalStateException("developer error, no reset password in progress"))
            return NoOpCancellable
        }
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            resetPasswordMailConfirmedInternal(safeResetPasswordData)
        }
    }

    private suspend fun resetPasswordMailConfirmedInternal(resetPasswordData: ResetPasswordData) {
        val param = ResetPasswordMailConfirmed.create(
                pendingSessionData.clientSecret,
                resetPasswordData.addThreePidRegistrationResponse.sid,
                resetPasswordData.newPassword
        )

        executeRequest<Unit>(null) {
            apiCall = authAPI.resetPasswordMailConfirmed(param)
        }

        // Set to null?
        // resetPasswordData = null
    }
}
