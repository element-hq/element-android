/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.auth.registration

import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

internal class DefaultRegistrationWizard(private val homeServerConnectionConfig: HomeServerConnectionConfig,
                                         private val okHttpClient: Lazy<OkHttpClient>,
                                         private val retrofitFactory: RetrofitFactory,
                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                         private val sessionParamsStore: SessionParamsStore,
                                         private val sessionManager: SessionManager) : RegistrationWizard {

    private var currentSession: String? = null

    private val authAPI = buildAuthAPI()
    private val registerTask = DefaultRegisterTask(authAPI)

    override fun createAccount(userName: String,
                               password: String,
                               initialDeviceDisplayName: String?,
                               callback: MatrixCallback<Session>): Cancelable {
        return performRegistrationRequest(RegistrationParams(
                username = userName,
                password = password,
                initialDeviceDisplayName = initialDeviceDisplayName
        ), callback)
    }

    override fun performReCaptcha(response: String, callback: MatrixCallback<Session>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        return performRegistrationRequest(
                RegistrationParams(
                        auth = AuthParamsCaptcha(
                                session = safeSession,
                                response = response)
                ), callback)
    }

    private fun performRegistrationRequest(registrationParams: RegistrationParams, callback: MatrixCallback<Session>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val result = runCatching {
                registerTask.execute(RegisterTask.Params(registrationParams))
            }
            result.fold(
                    {
                        val sessionParams = SessionParams(it, homeServerConnectionConfig)
                        sessionParamsStore.save(sessionParams)
                        val session = sessionManager.getOrCreateSession(sessionParams)

                        callback.onSuccess(session)
                    },
                    {
                        if (it is Failure.RegistrationFlowError) {
                            currentSession = it.registrationFlowResponse.session
                        }
                        callback.onFailure(it)
                    }
            )
        }
        return CancelableCoroutine(job)
    }

    private fun buildAuthAPI(): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }
}
