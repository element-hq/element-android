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
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.SendToDeviceBody
import org.matrix.android.sdk.internal.network.DEFAULT_REQUEST_RETRY_COUNT
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

const val TO_DEVICE_TRACING_ID_KEY = "org.matrix.msgid"

fun Event.toDeviceTracingId(): String? = content?.get(TO_DEVICE_TRACING_ID_KEY) as? String

internal interface SendToDeviceTask : Task<SendToDeviceTask.Params, Unit> {
    data class Params(
            // the type of event to send
            val eventType: String,
            // the content to send. Map from user_id to device_id to content dictionary.
            val contentMap: MXUsersDevicesMap<Any>,
            // the transactionId. If not provided, a transactionId will be created by the task
            val transactionId: String? = null,
            // Number of retry before failing
            val retryCount: Int = DEFAULT_REQUEST_RETRY_COUNT,
            // add tracing id, notice that to device events that do signature on content might be broken by it
            val addTracingIds: Boolean = !EventType.isVerificationEvent(eventType),
    )
}

internal class DefaultSendToDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendToDeviceTask {

    override suspend fun execute(params: SendToDeviceTask.Params) {
        // If params.transactionId is not provided, we create a unique txnId.
        // It's important to do that outside the requestBlock parameter of executeRequest()
        // to use the same value if the request is retried
        val txnId = params.transactionId ?: createUniqueTxnId()

        // add id tracing to debug
        val decorated = if (params.addTracingIds) {
            decorateWithToDeviceTracingIds(params)
        } else {
            params.contentMap.map to emptyList()
        }

        val sendToDeviceBody = SendToDeviceBody(
                messages = decorated.first
        )

        return executeRequest(
                globalErrorReceiver,
                canRetry = true,
                maxRetriesCount = params.retryCount
        ) {
            cryptoApi.sendToDevice(
                    eventType = params.eventType,
                    transactionId = txnId,
                    body = sendToDeviceBody
            )
            Timber.i("Sent to device type=${params.eventType} txnid=$txnId [${decorated.second.joinToString(",")}]")
        }
    }

    /**
     * To make it easier to track down where to-device messages are getting lost,
     * add a custom property to each one, and that will be logged after sent and on reception. Synapse will also log
     * this property.
     * @return A pair, first is the decorated content, and second info to log out after sending
     */
    private fun decorateWithToDeviceTracingIds(params: SendToDeviceTask.Params): Pair<Map<String, Map<String, Any>>, List<String>> {
        val tracingInfo = mutableListOf<String>()
        val decoratedContent = params.contentMap.map.map { userToDeviceMap ->
            val userId = userToDeviceMap.key
            userId to userToDeviceMap.value.map {
                val deviceId = it.key
                deviceId to it.value.toContent().toMutableMap().apply {
                    put(
                            TO_DEVICE_TRACING_ID_KEY,
                            UUID.randomUUID().toString().also {
                                tracingInfo.add("$userId/$deviceId (msgid $it)")
                            }
                    )
                }
            }.toMap()
        }.toMap()

        return decoratedContent to tracingInfo
    }
}

internal fun createUniqueTxnId() = UUID.randomUUID().toString()
