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

import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.SendToDeviceBody
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import java.util.UUID
import javax.inject.Inject

internal interface SendToDeviceTask : Task<SendToDeviceTask.Params, Unit> {
    data class Params(
            // the type of event to send
            val eventType: String,
            // the content to send. Map from user_id to device_id to content dictionary.
            val contentMap: MXUsersDevicesMap<Any>,
            // the transactionId. If not provided, a transactionId will be created by the task
            val transactionId: String? = null
    )
}

internal class DefaultSendToDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendToDeviceTask {

    override suspend fun execute(params: SendToDeviceTask.Params) {
        val sendToDeviceBody = SendToDeviceBody(
                messages = params.contentMap.map
        )

        // If params.transactionId is not provided, we create a unique txnId.
        // It's important to do that outside the requestBlock parameter of executeRequest()
        // to use the same value if the request is retried
        val txnId = params.transactionId ?: createUniqueTxnId()

        return executeRequest(
                globalErrorReceiver,
                canRetry = true,
                maxRetriesCount = 3
        ) {
            cryptoApi.sendToDevice(
                    eventType = params.eventType,
                    transactionId = txnId,
                    body = sendToDeviceBody
            )
        }
    }
}

internal fun createUniqueTxnId() = UUID.randomUUID().toString()
