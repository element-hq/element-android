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

import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.api.CryptoApi
import im.vector.matrix.android.internal.crypto.model.rest.DeviceKeys
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadBody
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadResponse
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.convertToUTF8
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface UploadKeysTask : Task<UploadKeysTask.Params, KeysUploadResponse> {
    data class Params(
            // the device keys to send.
            val deviceKeys: DeviceKeys?,
            // the one-time keys to send.
            val oneTimeKeys: JsonDict?,
            // the explicit device_id to use for upload (default is to use the same as that used during auth).
            val deviceId: String)
}

internal class DefaultUploadKeysTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val eventBus: EventBus
) : UploadKeysTask {

    override suspend fun execute(params: UploadKeysTask.Params): KeysUploadResponse {
        val encodedDeviceId = convertToUTF8(params.deviceId)

        val body = KeysUploadBody()

        if (null != params.deviceKeys) {
            body.deviceKeys = params.deviceKeys
        }

        if (null != params.oneTimeKeys) {
            body.oneTimeKeys = params.oneTimeKeys
        }

        return executeRequest(eventBus) {
            apiCall = if (encodedDeviceId.isBlank()) {
                cryptoApi.uploadKeys(body)
            } else {
                cryptoApi.uploadKeys(encodedDeviceId, body)
            }
        }
    }
}
