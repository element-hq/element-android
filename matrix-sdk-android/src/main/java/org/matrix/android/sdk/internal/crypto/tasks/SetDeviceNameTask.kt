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

import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.UpdateDeviceInfoBody
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SetDeviceNameTask : Task<SetDeviceNameTask.Params, Unit> {
    data class Params(
            // the device id
            val deviceId: String,
            // the device name
            val deviceName: String
    )
}

internal class DefaultSetDeviceNameTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SetDeviceNameTask {

    override suspend fun execute(params: SetDeviceNameTask.Params) {
        val body = UpdateDeviceInfoBody(
                displayName = params.deviceName
        )
        return executeRequest(globalErrorReceiver) {
            cryptoApi.updateDeviceInfo(params.deviceId, body)
        }
    }
}
