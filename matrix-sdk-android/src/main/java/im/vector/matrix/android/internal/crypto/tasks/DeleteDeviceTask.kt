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

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.DeleteDeviceParams
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface DeleteDeviceTask : Task<DeleteDeviceTask.Params, Unit> {
    data class Params(
            val deviceId: String
    )
}

internal class DefaultDeleteDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val eventBus: EventBus
) : DeleteDeviceTask {

    override suspend fun execute(params: DeleteDeviceTask.Params) {
        try {
            executeRequest<Unit>(eventBus) {
                apiCall = cryptoApi.deleteDevice(params.deviceId, DeleteDeviceParams())
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.OtherServerError && throwable.httpCode == 401) {
                // Parse to get a RegistrationFlowResponse
                val registrationFlowResponse = try {
                    MoshiProvider.providesMoshi()
                            .adapter(RegistrationFlowResponse::class.java)
                            .fromJson(throwable.errorBody)
                } catch (e: Exception) {
                    null
                }
                // check if the server response can be casted
                if (registrationFlowResponse != null) {
                    throw Failure.RegistrationFlowError(registrationFlowResponse)
                } else {
                    throw throwable
                }
            } else {
                // Other error
                throw throwable
            }
        }
    }
}
