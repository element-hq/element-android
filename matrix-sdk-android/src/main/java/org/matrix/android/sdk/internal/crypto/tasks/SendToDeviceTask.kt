/*
 * Copyright 2019 New Vector Ltd
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
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.rest.SendToDeviceBody
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.random.Random

internal interface SendToDeviceTask : Task<SendToDeviceTask.Params, Unit> {
    data class Params(
            // the type of event to send
            val eventType: String,
            // the content to send. Map from user_id to device_id to content dictionary.
            val contentMap: MXUsersDevicesMap<Any>,
            // the transactionId
            val transactionId: String? = null
    )
}

internal class DefaultSendToDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val eventBus: EventBus
) : SendToDeviceTask {

    override suspend fun execute(params: SendToDeviceTask.Params) {
        val sendToDeviceBody = SendToDeviceBody(
                messages = params.contentMap.map
        )

        return executeRequest(eventBus) {
            apiCall = cryptoApi.sendToDevice(
                    params.eventType,
                    params.transactionId ?: Random.nextInt(Integer.MAX_VALUE).toString(),
                    sendToDeviceBody
            )
            isRetryable = true
            maxRetryCount = 3
        }
    }
}
