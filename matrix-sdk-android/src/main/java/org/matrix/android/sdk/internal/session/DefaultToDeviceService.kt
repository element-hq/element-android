/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.session.ToDeviceService
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import javax.inject.Inject

internal class DefaultToDeviceService @Inject constructor(
        private val sendToDeviceTask: SendToDeviceTask,
) : ToDeviceService {

    override suspend fun sendToDevice(eventType: String, targets: Map<String, List<String>>, content: Content, txnId: String?) {
        val sendToDeviceMap = MXUsersDevicesMap<Any>()
        targets.forEach { (userId, deviceIdList) ->
            deviceIdList.forEach { deviceId ->
                sendToDeviceMap.setObject(userId, deviceId, content)
            }
        }
        sendToDevice(eventType, sendToDeviceMap, txnId)
    }

    override suspend fun sendToDevice(eventType: String, contentMap: MXUsersDevicesMap<Any>, txnId: String?) {
        sendToDeviceTask.execute(
                SendToDeviceTask.Params(
                        eventType = eventType,
                        contentMap = contentMap,
                        transactionId = txnId
                )
        )
    }

    override suspend fun sendEncryptedToDevice(eventType: String, targets: Map<String, List<String>>, content: Content, txnId: String?) {
        // TODO add to rust-ffi
        /*
        val payloadJson = mapOf(
                "type" to eventType,
                "content" to content
        )
        val sendToDeviceMap = MXUsersDevicesMap<Any>()

        // Should I do an ensure olm session?
        targets.forEach { (userId, deviceIdList) ->
            deviceIdList.forEach { deviceId ->
                cryptoStore.getUserDevice(userId, deviceId)?.let { deviceInfo ->
                    sendToDeviceMap.setObject(userId, deviceId, encryptEventContent(payloadJson, listOf(deviceInfo)))
                }
            }
        }

        sendToDevice(EventType.ENCRYPTED, sendToDeviceMap, txnId)

         */
    }
}
