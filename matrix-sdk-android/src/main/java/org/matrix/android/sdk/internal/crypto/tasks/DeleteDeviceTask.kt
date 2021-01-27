/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.toRegistrationFlowResponse
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.DeleteDeviceParams
import org.matrix.android.sdk.internal.crypto.model.rest.UIABaseAuth
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

internal interface DeleteDeviceTask : Task<DeleteDeviceTask.Params, Unit> {
    data class Params(
            val deviceId: String,
            val userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor?,
            val userAuthParam: UIABaseAuth?
    )
}

internal class DefaultDeleteDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DeleteDeviceTask {

    override suspend fun execute(params: DeleteDeviceTask.Params) {
        try {
            executeRequest<Unit>(globalErrorReceiver) {
                apiCall = cryptoApi.deleteDevice(params.deviceId, DeleteDeviceParams(params.userAuthParam?.asMap()))
            }
        } catch (throwable: Throwable) {
            if (params.userInteractiveAuthInterceptor == null || !handleUIA(throwable, params)) {
                Timber.d("## UIA: propagate failure")
                throw  throwable
            }
        }
    }

    private suspend fun handleUIA(failure: Throwable, params: DeleteDeviceTask.Params): Boolean {
        Timber.d("## UIA: check error delete device ${failure.message}")
        if (failure is Failure.OtherServerError && failure.httpCode == 401) {
            Timber.d("## UIA: error can be passed to interceptor")
            // give a chance to the reauth helper?
            val flowResponse = failure.toRegistrationFlowResponse()
                    ?: return false.also {
                        Timber.d("## UIA: failed to parse flow response")
                    }

            Timber.d("## UIA: type = ${flowResponse.flows}")
            Timber.d("## UIA: has interceptor = ${params.userInteractiveAuthInterceptor != null}")

            Timber.d("## UIA: delegate to interceptor...")
            val authUpdate = try {
                suspendCoroutine<UIABaseAuth> { continuation ->
                    params.userInteractiveAuthInterceptor!!.performStage(flowResponse, continuation)
                }
            } catch (failure: Throwable) {
                Timber.w(failure, "## UIA: failed to participate")
                return false
            }

            Timber.d("## UIA: delete device updated auth $authUpdate")
            return try {
                execute(params.copy(userAuthParam = authUpdate))
                true
            } catch (failure: Throwable) {
                handleUIA(failure, params)
            }
        } else {
            Timber.d("## UIA: not a UIA error")
            return false
        }
    }
}
