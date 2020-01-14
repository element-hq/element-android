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

import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.DeleteDeviceAuth
import im.vector.matrix.android.internal.crypto.model.rest.DeleteDeviceParams
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface DeleteDeviceWithUserPasswordTask : Task<DeleteDeviceWithUserPasswordTask.Params, Unit> {
    data class Params(
            val deviceId: String,
            val authSession: String?,
            val password: String
    )
}

internal class DefaultDeleteDeviceWithUserPasswordTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        @UserId private val userId: String,
        private val eventBus: EventBus
) : DeleteDeviceWithUserPasswordTask {

    override suspend fun execute(params: DeleteDeviceWithUserPasswordTask.Params) {
        return executeRequest(eventBus) {
            apiCall = cryptoApi.deleteDevice(params.deviceId, DeleteDeviceParams()
                    .apply {
                        deleteDeviceAuth = DeleteDeviceAuth()
                                .apply {
                                    type = LoginFlowTypes.PASSWORD
                                    session = params.authSession
                                    user = userId
                                    password = params.password
                                }
                    })
        }
    }
}
