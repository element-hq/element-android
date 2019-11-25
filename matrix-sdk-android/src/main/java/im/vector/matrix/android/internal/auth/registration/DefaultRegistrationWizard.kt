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
import im.vector.matrix.android.api.auth.registration.RegisterThreePid
import im.vector.matrix.android.api.auth.registration.RegistrationResult
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.Failure.RegistrationFlowError
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.auth.SessionCreator
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.task.launchToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.*

// Container to store the data when a three pid is in validation step
internal data class ThreePidData(
        val threePid: RegisterThreePid,
        val addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
        val registrationParams: RegistrationParams
)

/**
 * This class execute the registration request and is responsible to keep the session of interactive authentication
 */
internal class DefaultRegistrationWizard(private val homeServerConnectionConfig: HomeServerConnectionConfig,
                                         private val okHttpClient: Lazy<OkHttpClient>,
                                         private val retrofitFactory: RetrofitFactory,
                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                         private val sessionCreator: SessionCreator) : RegistrationWizard {
    private var clientSecret = UUID.randomUUID().toString()
    private var sendAttempt = 0

    private var currentSession: String? = null

    private val authAPI = buildAuthAPI()
    private val registerTask = DefaultRegisterTask(authAPI)
    private val registerAddThreePidTask = DefaultRegisterAddThreePidTask(authAPI)
    private val validateCodeTask = DefaultValidateCodeTask(authAPI)

    private var currentThreePidData: ThreePidData? = null

    override val currentThreePid: String?
        get() {
            return when (val threePid = currentThreePidData?.threePid) {
                is RegisterThreePid.Email  -> threePid.email
                is RegisterThreePid.Msisdn -> {
                    // Take formatted msisdn if provided by the server
                    currentThreePidData?.addThreePidRegistrationResponse?.formattedMsisdn?.takeIf { it.isNotBlank() } ?: threePid.msisdn
                }
                null                       -> null
            }
        }

    override fun getRegistrationFlow(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val params = RegistrationParams()
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
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
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun performReCaptcha(response: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        val params = RegistrationParams(auth = AuthParams.createForCaptcha(safeSession, response))
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun acceptTerms(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        val params = RegistrationParams(auth = AuthParams(type = LoginFlowTypes.TERMS, session = safeSession))
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(params)
        }
    }

    override fun addThreePid(threePid: RegisterThreePid, callback: MatrixCallback<RegistrationResult>): Cancelable {
        currentThreePidData = null
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            sendThreePid(threePid)
        }
    }

    override fun sendAgainThreePid(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeCurrentThreePid = currentThreePidData?.threePid ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            sendThreePid(safeCurrentThreePid)
        }
    }

    private suspend fun sendThreePid(threePid: RegisterThreePid): RegistrationResult {
        val safeSession = currentSession ?: throw IllegalStateException("developer error, call createAccount() method first")
        val response = registerAddThreePidTask.execute(RegisterAddThreePidTask.Params(threePid, clientSecret, sendAttempt++))
        val params = RegistrationParams(
                auth = if (threePid is RegisterThreePid.Email) {
                    AuthParams.createForEmailIdentity(safeSession,
                            ThreePidCredentials(
                                    clientSecret = clientSecret,
                                    sid = response.sid
                            )
                    )
                } else {
                    AuthParams.createForMsisdnIdentity(safeSession,
                            ThreePidCredentials(
                                    clientSecret = clientSecret,
                                    sid = response.sid
                            )
                    )
                }
        )
        // Store data
        currentThreePidData = ThreePidData(threePid, response, params)
        // and send the sid a first time
        return performRegistrationRequest(params)
    }

    override fun checkIfEmailHasBeenValidated(delayMillis: Long, callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeParam = currentThreePidData?.registrationParams ?: run {
            callback.onFailure(IllegalStateException("developer error, no pending three pid"))
            return NoOpCancellable
        }
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            performRegistrationRequest(safeParam, delayMillis)
        }
    }

    override fun handleValidateThreePid(code: String, callback: MatrixCallback<RegistrationResult>): Cancelable {
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
            validateThreePid(code)
        }
    }

    private suspend fun validateThreePid(code: String): RegistrationResult {
        val registrationParams = currentThreePidData?.registrationParams ?: throw IllegalStateException("developer error, no pending three pid")
        val safeCurrentData = currentThreePidData ?: throw IllegalStateException("developer error, call createAccount() method first")
        val url = safeCurrentData.addThreePidRegistrationResponse.submitUrl ?: throw IllegalStateException("Missing url the send the code")
        val validationBody = ValidationCodeBody(
                clientSecret = clientSecret,
                sid = safeCurrentData.addThreePidRegistrationResponse.sid,
                code = code
        )
        val validationResponse = validateCodeTask.execute(ValidateCodeTask.Params(url, validationBody))
        if (validationResponse.success == true) {
            // The entered code is correct
            // Same than validate email
            return performRegistrationRequest(registrationParams, 3_000)
        } else {
            // The code is not correct
            throw Failure.SuccessError
        }
    }

    override fun dummy(callback: MatrixCallback<RegistrationResult>): Cancelable {
        val safeSession = currentSession ?: run {
            callback.onFailure(IllegalStateException("developer error, call createAccount() method first"))
            return NoOpCancellable
        }
        return GlobalScope.launchToCallback(coroutineDispatchers.main, callback) {
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
                currentSession = exception.registrationFlowResponse.session
                return RegistrationResult.FlowResponse(exception.registrationFlowResponse.toFlowResult())
            } else {
                throw exception
            }
        }

        val session = sessionCreator.createSession(credentials, homeServerConnectionConfig)
        return RegistrationResult.Success(session)
    }

    private fun buildAuthAPI(): AuthAPI {
        val retrofit = retrofitFactory.create(okHttpClient, homeServerConnectionConfig.homeServerUri.toString())
        return retrofit.create(AuthAPI::class.java)
    }
}
