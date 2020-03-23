/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmDecryptionFactory
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.event.SecretSendEventContent
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class ShareSecretCryptoProvider @Inject constructor(
        val messageEncrypter: MessageEncrypter,
        val sendToDeviceTask: SendToDeviceTask,
        val deviceListManager: DeviceListManager,
        @UserId val myUserId: String,
        private val olmDecryptionFactory: MXOlmDecryptionFactory,
        val cryptoCoroutineScope: CoroutineScope,
        val cryptoStore: IMXCryptoStore,
        val coroutineDispatchers: MatrixCoroutineDispatchers
) {
    fun shareSecretWithDevice(request: IncomingSecretShareRequest, secretValue: String) {
        val userId = request.userId ?: return
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            //            runCatching { deviceListManager.downloadKeys(listOf(userId), false) }
//                    .mapCatching {
            val deviceId = request.deviceId
            val deviceInfo = cryptoStore.getUserDevice(userId, deviceId ?: "") ?: return@launch


            Timber.i("## shareSecretWithDevice() : sharing secret ${request.secretName} with device $userId:$deviceId")

            val payloadJson = mutableMapOf<String, Any>("type" to EventType.SEND_SECRET)
            val secretContent = SecretSendEventContent(
                    requestId = request.requestId ?: "",
                    secretValue = secretValue
            )
            payloadJson["content"] = secretContent.toContent()

            cryptoStore.saveGossipingEvent(Event(
                    type = EventType.SEND_SECRET,
                    content = secretContent.toContent(),
                    senderId = myUserId
            ))

            val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
            val sendToDeviceMap = MXUsersDevicesMap<Any>()
            sendToDeviceMap.setObject(userId, deviceId, encodedPayload)
            Timber.i("## shareSecretWithDevice() : sending to $userId:$deviceId")
            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
            sendToDeviceTask.execute(sendToDeviceParams)
//                    }
        }
    }

    fun decryptEvent(event: Event): MXEventDecryptionResult {
        return olmDecryptionFactory.create().decryptEvent(event, ShareSecretCryptoProvider::class.java.name ?: "")
    }
}
