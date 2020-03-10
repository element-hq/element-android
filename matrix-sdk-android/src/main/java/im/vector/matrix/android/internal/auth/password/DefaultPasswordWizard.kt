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

package im.vector.matrix.android.internal.auth.password

import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.password.PasswordWizard
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.auth.data.UpdatePasswordParams
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.launchToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import timber.log.Timber

internal class DefaultPasswordWizard(
        okHttpClient: Lazy<OkHttpClient>,
        retrofitFactory: RetrofitFactory,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val homeServerConnectionConfig: HomeServerConnectionConfig
) : PasswordWizard {

    private val authAPI = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
            .create(AuthAPI::class.java)

    override fun updatePassword(sessionId: String, userId: String, oldPassword: String, newPassword: String, callback: MatrixCallback<Unit>): Cancelable {
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            updatePasswordInternal(sessionId, userId, oldPassword, newPassword)
        }
    }

    private suspend fun updatePasswordInternal(sessionId: String, userId: String, oldPassword: String, newPassword: String) {
        val params = UpdatePasswordParams.create(sessionId, userId, oldPassword, newPassword)
        try {
            executeRequest<Unit>(null) {
                apiCall = authAPI.updatePassword(params)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.OtherServerError
                    && throwable.httpCode == 401
                    /* Avoid infinite loop */
                    && params.auth?.session == null) {
                try {
                    MoshiProvider.providesMoshi()
                            .adapter(RegistrationFlowResponse::class.java)
                            .fromJson(throwable.errorBody)
                } catch (e: Exception) {
                    null
                }?.let {
                    // Retry with authentication
                    try {
                        executeRequest<Unit>(null) {
                            apiCall = authAPI.updatePassword(
                                    params.copy(auth = params.auth?.copy(session = it.session))
                            )
                        }
                        return
                    } catch (failure: Throwable) {
                        throw failure
                    }
                }
            }
            throw throwable
        }
    }
}
