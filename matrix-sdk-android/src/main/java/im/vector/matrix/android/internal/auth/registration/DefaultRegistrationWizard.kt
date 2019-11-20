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
import im.vector.matrix.android.api.auth.registration.RegisterThreePid
import im.vector.matrix.android.api.auth.registration.RegistrationResult
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.*

// Container to store the data when a three pid is in validation step
internal data class ThreePidData(
        val threePid: RegisterThreePid,
        val registrationParams: RegistrationParams
)

/**
 * This class execute the registration request and is responsible to keep the session of interactive authentication
 */
internal class DefaultRegistrationWizard(private val homeServerConnectionConfig: HomeServerConnectionConfig,
                                         private val okHttpClient: Lazy<OkHttpClient>,
                                         private val retrofitFactory: RetrofitFactory,
                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                         private val sessionParamsStore: SessionParamsStore,
                                         private val sessionManager: SessionManager) : RegistrationWizard {
    private var clientSecret = UUID.randomUUID().toString()
    private var sendAttempt = 0

    private var currentSession: String? = null

    private val authAPI = buildAuthAPI()
    private val registerTask = DefaultRegisterTask(authAPI)
    private val registerAddThreePidTask = DefaultRegisterAddThreePidTask(authAPI)

    private var currentThreePidData: ThreePidData? = null

    override val currentThreePid: String?
        get() {
            return when (val threePid = currentThreePidData?.threePid) {
                is RegisterThreePid.Email  -> threePid.email
                is RegisterThreePid.Msisdn -> threePid.msisdn
                null                       -> null
            }
        }

    override fun getRegistrationFlow(callback: MatrixCallback<RegistrationResult>): Cancelable {
        return performRegistrationRequest(RegistrationParams(), callback)
    }

    override fun createAccount(userName: String,
                               password: String,
                               initialDeviceDisplayName: String?,
                               callback: MatrixCallback<RegistrationResult>): Cancelable {
        return performRegistrationRequest(RegistrationParams(
                username = userName,
                password = password,
                initialDeviceDisplayName = initialDeviceDisplayName
        ), callback)
    }

    override fun performReCaptcha(response: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        return performRegistrationRequest(
                RegistrationParams(
                        auth = AuthParams.createForCaptcha(safeSession, response)
                ), callback)
    }

    override fun acceptTerms(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        return performRegistrationRequest(
                RegistrationParams(
                        auth = AuthParams(
                                type = LoginFlowTypes.TERMS,
                                session = safeSession
                        )
                ), callback)
    }

    override fun addThreePid(threePid: RegisterThreePid, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        val job = GlobalScope.launch(coroutineDispatchers.main) {
            runCatching {
                registerAddThreePidTask.execute(RegisterAddThreePidTask.Params(threePid, clientSecret, sendAttempt++))
            }
                    .fold(
                            {
                                // Store data
                                currentThreePidData = ThreePidData(
                                        threePid,
                                        RegistrationParams(
                                                auth = AuthParams.createForEmailIdentity(safeSession,
                                                        ThreePidCredentials(
                                                                clientSecret = clientSecret,
                                                                sid = it.sid
                                                        )
                                                )
                                        ))
                                        .also { threePidData ->
                                            // and send the sid a first time
                                            performRegistrationRequest(threePidData.registrationParams, callback)
                                        }
                            },
                            {
                                callback.onFailure(it)
                            }
                    )
        }
        return CancelableCoroutine(job)
    }

    override fun validateEmail(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeParam = currentThreePidData?.registrationParams ?: run {
            callback.onFailure(IllegalStateException("developer error, no pending three pid"))
            return NoOpCancellable
        }

        // Wait 10 seconds before doing the request
        return performRegistrationRequest(safeParam, callback, 10_000)
    }

    override fun confirmMsisdn(code: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        // TODO
        return performRegistrationRequest(
                RegistrationParams(
                        // TODO
                        auth = AuthParams.createForEmailIdentity(safeSession, ThreePidCredentials(code))
                ), callback)
    }

    override fun dummy(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }

        return performRegistrationRequest(
                RegistrationParams(
                        auth = AuthParams(
                                type = LoginFlowTypes.DUMMY,
                                session = safeSession
                        )
                ), callback)
    }

    private fun performRegistrationRequest(registrationParams: RegistrationParams,
                                           callback: MatrixCallback<RegistrationResult>,
                                           delayMillis: Long = 0): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            runCatching {
                if (delayMillis > 0) delay(delayMillis)
                registerTask.execute(RegisterTask.Params(registrationParams))
            }
                    .fold(
                            {
                                val sessionParams = SessionParams(it, homeServerConnectionConfig)
                                sessionParamsStore.save(sessionParams)
                                val session = sessionManager.getOrCreateSession(sessionParams)

                                callback.onSuccess(RegistrationResult.Success(session))
                            },
                            {
                                if (it is Failure.RegistrationFlowError) {
                                    currentSession = it.registrationFlowResponse.session
                                    callback.onSuccess(RegistrationResult.FlowResponse(it.registrationFlowResponse.toFlowResult()))
                                } else {
                                    callback.onFailure(it)
                                }
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
