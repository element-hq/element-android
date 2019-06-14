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

package im.vector.matrix.android.internal.crypto.tasks

import arrow.core.Try
import arrow.core.failure
import arrow.core.recoverWith
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.DeleteDeviceAuth
import im.vector.matrix.android.internal.crypto.model.rest.DeleteDeviceParams
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import timber.log.Timber

internal interface DeleteDeviceTask : Task<DeleteDeviceTask.Params, Unit> {
    data class Params(
            val deviceId: String,
            val accountPassword: String
    )
}

internal class DefaultDeleteDeviceTask(private val cryptoApi: CryptoApi,
                                       private val credentials: Credentials)
    : DeleteDeviceTask {

    override suspend fun execute(params: DeleteDeviceTask.Params): Try<Unit> {
        return executeRequest<Unit> {
            apiCall = cryptoApi.deleteDevice(params.deviceId, DeleteDeviceParams())
        }.recoverWith { throwable ->
            if (throwable is Failure.OtherServerError && throwable.httpCode == 401) {
                // Replay the request with passing the credentials

                // Parse to get a RegistrationFlowResponse
                val registrationFlowResponseAdapter = MoshiProvider.providesMoshi().adapter(RegistrationFlowResponse::class.java)
                val registrationFlowResponse = try {
                    registrationFlowResponseAdapter.fromJson(throwable.errorBody)
                } catch (e: Exception) {
                    null
                }

                // check if the server response can be casted
                if (registrationFlowResponse?.flows?.isNotEmpty() == true) {
                    val stages = ArrayList<String>()

                    // Get all stages
                    registrationFlowResponse.flows?.forEach {
                        stages.addAll(it.stages ?: emptyList())
                    }

                    Timber.v("## deleteDevice() : supported stages $stages")

                    deleteDeviceRecursive(registrationFlowResponse.session, params, stages)
                } else {
                    throwable.failure()
                }

            } else {
                // Other error
                throwable.failure()
            }
        }
    }

    private fun deleteDeviceRecursive(authSession: String?,
                                      params: DeleteDeviceTask.Params,
                                      remainingStages: MutableList<String>): Try<Unit> {
        // Pick the first stage
        val stage = remainingStages.first()

        val newParams = DeleteDeviceParams()
                .apply {
                    deleteDeviceAuth = DeleteDeviceAuth()
                            .apply {
                                type = stage
                                session = authSession
                                user = credentials.userId
                                password = params.accountPassword
                            }
                }

        return executeRequest<Unit> {
            apiCall = cryptoApi.deleteDevice(params.deviceId, newParams)
        }.recoverWith { throwable ->
            if (throwable is Failure.ServerError
                    && throwable.httpCode == 401
                    && (throwable.error.code == MatrixError.FORBIDDEN || throwable.error.code == MatrixError.UNKNOWN)) {
                if (remainingStages.size > 1) {
                    // Try next stage
                    val otherStages = remainingStages.subList(1, remainingStages.size)

                    deleteDeviceRecursive(authSession, params, otherStages)
                } else {
                    // No more stage remaining
                    throwable.failure()
                }
            } else {
                // Other error
                throwable.failure()
            }
        }
    }
}
