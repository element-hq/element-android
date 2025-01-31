/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.session.ToDeviceService
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import javax.inject.Inject

internal class DefaultToDeviceService @Inject constructor(
        private val sendToDeviceTask: SendToDeviceTask,
        private val messageEncrypter: MessageEncrypter,
        private val cryptoStore: IMXCryptoStore
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
        sendToDeviceTask.executeRetry(
                SendToDeviceTask.Params(
                        eventType = eventType,
                        contentMap = contentMap,
                        transactionId = txnId
                ),
                3
        )
    }

    override suspend fun sendEncryptedToDevice(eventType: String, targets: Map<String, List<String>>, content: Content, txnId: String?) {
        val payloadJson = mapOf(
                "type" to eventType,
                "content" to content
        )
        val sendToDeviceMap = MXUsersDevicesMap<Any>()

        // Should I do an ensure olm session?
        targets.forEach { (userId, deviceIdList) ->
            deviceIdList.forEach { deviceId ->
                cryptoStore.getUserDevice(userId, deviceId)?.let { deviceInfo ->
                    sendToDeviceMap.setObject(userId, deviceId, messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo)))
                }
            }
        }

        sendToDevice(EventType.ENCRYPTED, sendToDeviceMap, txnId)
    }
}
