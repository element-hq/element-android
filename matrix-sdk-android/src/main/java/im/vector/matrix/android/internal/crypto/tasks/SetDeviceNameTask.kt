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

import android.text.TextUtils
import arrow.core.Try
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.UpdateDeviceInfoBody
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task

internal interface SetDeviceNameTask : Task<SetDeviceNameTask.Params, Unit> {
    data class Params(
            // the device id
            val deviceId: String,
            // the device name
            val deviceName: String
    )
}

internal class DefaultSetDeviceNameTask(private val cryptoApi: CryptoApi)
    : SetDeviceNameTask {

    override suspend fun execute(params: SetDeviceNameTask.Params): Try<Unit> {
        val body = UpdateDeviceInfoBody(
                displayName = if (TextUtils.isEmpty(params.deviceName)) "" else params.deviceName
        )

        return executeRequest {
            apiCall = cryptoApi.updateDeviceInfo(params.deviceId, body)
        }
    }
}
