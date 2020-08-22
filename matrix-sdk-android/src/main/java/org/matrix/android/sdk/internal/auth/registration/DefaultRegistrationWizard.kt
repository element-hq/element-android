/*
 * Copyright 2018 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.Failure.RegistrationFlowError
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.auth.PendingSessionStore
import org.matrix.android.sdk.internal.auth.SessionCreator
import org.matrix.android.sdk.internal.auth.db.PendingSessionData
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

/**
 * This class execute the registration request and is responsible to keep the session of interactive authentication
 */
internal class DefaultRegistrationWizard(
        private val okHttpClient: OkHttpClient,
        private val retrofitFactory: RetrofitFactory,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore,
        private val coroutineScope: CoroutineScope
) : RegistrationWizard {

    private var pendingSessionData: PendingSessionData = pendingSessionStore.getPendingSessionData() ?: error("Pending session data should exist here")

    private val authAPI = buildAuthAPI()
    private val registerTask = DefaultRegisterTask(authAPI)
    private val registerAddThreePidTask = DefaultRegisterAddThreePidTask(authAPI)
    private val validateCodeTask = DefaultValidateCodeTask(authAPI)

    override val currentThreePid: String?
        get() {
            return when (val threePid = pendingSessionData.currentThreePidData?.threePid) {
                is RegisterThreePid.Email  -> threePid.email
                is RegisterThreePid.Msisdn -> {
                    // Take formatted msisdn if provided by the server
                    pendingSessionData.currentThreePidData?.addThreePidRegistrationResponse?.formattedMsisdn?.takeIf { it.isNotBlank() } ?: threePid.msisdn
                }
                null                       -> null
            }
        }

    override val isRegistrationStarted: Boolean
        get() = pendingSessionData.isRegistrationStarted

    override fun getRegistrationFlow(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val params = RegistrationParams()
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun createAccount(userName: String,
                               password: String,
                               initialDeviceDisplayName: String?,
                               callback: MatrixCallback<RegistrationResult>): Cancelable {
        val params = RegistrationParams(
                username = userName,
                password = password,
                initialDeviceDisplayName = initialDeviceDisplayName
        )
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
                    .also {
                        pendingSessionData = pendingSessionData.copy(isRegistrationStarted = true)
                                .also { pendingSessionStore.savePendingSessionData(it) }
                    }
        }
    }

    override fun performReCaptcha(response: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = pendingSessionData.currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        val params = RegistrationParams(auth = AuthParams.createForCaptcha(safeSession, response))
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun acceptTerms(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = pendingSessionData.currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        val params = RegistrationParams(auth = AuthParams(type = LoginFlowTypes.TERMS, session = safeSession))
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun addThreePid(threePid: RegisterThreePid, callback: MatrixCallback<RegistrationResult>): Cancelable {
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            pendingSessionData = pendingSessionData.copy(currentThreePidData = null)
                    .also { pendingSessionStore.savePendingSessionData(it) }

            sendThreePid(threePid)
        }
    }

    override fun sendAgainThreePid(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeCurrentThreePid = pendingSessionData.currentThreePidData?.threePid ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            sendThreePid(safeCurrentThreePid)
        }
    }

    private suspend fun sendThreePid(threePid: RegisterThreePid): RegistrationResult {
        val safeSession = pendingSessionData.currentSession ?: throw IllegalStateException("developer error, call createAccount() method first")
        val response = registerAddThreePidTask.execute(
                RegisterAddThreePidTask.Params(
                        threePid,
                        pendingSessionData.clientSecret,
                        pendingSessionData.sendAttempt))

        pendingSessionData = pendingSessionData.copy(sendAttempt = pendingSessionData.sendAttempt + 1)
                .also { pendingSessionStore.savePendingSessionData(it) }

        val params = RegistrationParams(
                auth = if (threePid is RegisterThreePid.Email) {
                    AuthParams.createForEmailIdentity(safeSession,
                            ThreePidCredentials(
                                    clientSecret = pendingSessionData.clientSecret,
                                    sid = response.sid
                            )
                    )
                } else {
                    AuthParams.createForMsisdnIdentity(safeSession,
                            ThreePidCredentials(
                                    clientSecret = pendingSessionData.clientSecret,
                                    sid = response.sid
                            )
                    )
                }
        )
        // Store data
        pendingSessionData = pendingSessionData.copy(currentThreePidData = ThreePidData.from(threePid, response, params))
                .also { pendingSessionStore.savePendingSessionData(it) }

        // and send the sid a first time
        return performRegistrationRequest(params)
    }

    override fun checkIfEmailHasBeenValidated(delayMillis: Long, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeParam = pendingSessionData.currentThreePidData?.registrationParams ?: run {
            callback.onFailure(IllegalStateException("developer error, no pending three pid"))
            return NoOpCancellable
        }
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(safeParam, delayMillis)
        }
    }

    override fun handleValidateThreePid(code: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            validateThreePid(code)
        }
    }

    private suspend fun validateThreePid(code: String): RegistrationResult {
        val registrationParams = pendingSessionData.currentThreePidData?.registrationParams
                ?: throw IllegalStateException("developer error, no pending three pid")
        val safeCurrentData = pendingSessionData.currentThreePidData ?: throw IllegalStateException("developer error, call createAccount() method first")
        val url = safeCurrentData.addThreePidRegistrationResponse.submitUrl ?: throw IllegalStateException("Missing url the send the code")
        val validationBody = ValidationCodeBody(
                clientSecret = pendingSessionData.clientSecret,
                sid = safeCurrentData.addThreePidRegistrationResponse.sid,
                code = code
        )
        val validationResponse = validateCodeTask.execute(ValidateCodeTask.Params(url, validationBody))
        if (validationResponse.isSuccess()) {
            // The entered code is correct
            // Same than validate email
            return performRegistrationRequest(registrationParams, 3_000)
        } else {
            // The code is not correct
            throw Failure.SuccessError
        }
    }

    override fun dummy(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = pendingSessionData.currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        return coroutineScope.launchToCallback(coroutineDispatchers.main, callback) {
            val params = RegistrationParams(auth = AuthParams(type = LoginFlowTypes.DUMMY, session = safeSession))
            performRegistrationRequest(params)
        }
    }

    private suspend fun performRegistrationRequest(registrationParams: RegistrationParams,
                                                   delayMillis: Long = 0): RegistrationResult {
        delay(delayMillis)
        val credentials = try {
            registerTask.execute(RegisterTask.Params(registrationParams))
        } catch (exception: Throwable) {
            if (exception is RegistrationFlowError) {
                pendingSessionData = pendingSessionData.copy(currentSession = exception.registrationFlowResponse.session)
                        .also { pendingSessionStore.savePendingSessionData(it) }
                return RegistrationResult.FlowResponse(exception.registrationFlowResponse.toFlowResult())
            } else {
                throw exception
            }
        }

        val session = sessionCreator.createSession(credentials, pendingSessionData.homeServerConnectionConfig)
        return RegistrationResult.Success(session)
    }

    private fun buildAuthAPI(): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, pendingSessionData.homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }
}
