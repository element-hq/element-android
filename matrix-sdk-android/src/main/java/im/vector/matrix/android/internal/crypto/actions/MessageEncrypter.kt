/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.actions

import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_OLM
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedMessage
import im.vector.matrix.android.internal.di.DeviceId
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import im.vector.matrix.android.internal.util.convertToUTF8
import timber.log.Timber
import javax.inject.Inject

internal class MessageEncrypter @Inject constructor(
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        private val olmDevice: MXOlmDevice) {
    /**
     * Encrypt an event payload for a list of devices.
     * This method must be called from the getCryptoHandler() thread.
     *
     * @param payloadFields fields to include in the encrypted payload.
     * @param deviceInfos   list of device infos to encrypt for.
     * @return the content for an m.room.encrypted event.
     */
    fun encryptMessage(payloadFields: Content, deviceInfos: List<CryptoDeviceInfo>): EncryptedMessage {
        val deviceInfoParticipantKey = deviceInfos.associateBy { it.identityKey()!! }

        val payloadJson = payloadFields.toMutableMap()

        payloadJson["sender"] = userId
        payloadJson["sender_device"] = deviceId!!

        // Include the Ed25519 key so that the recipient knows what
        // device this message came from.
        // We don't need to include the curve25519 key since the
        // recipient will already know this from the olm headers.
        // When combined with the device keys retrieved from the
        // homeserver signed by the ed25519 key this proves that
        // the curve25519 key and the ed25519 key are owned by
        // the same device.
        val keysMap = HashMap<String, String>()
        keysMap["ed25519"] = olmDevice.deviceEd25519Key!!
        payloadJson["keys"] = keysMap

        val ciphertext = HashMap<String, Any>()

        for ((deviceKey, deviceInfo) in deviceInfoParticipantKey) {
            val sessionId = olmDevice.getSessionId(deviceKey)

            if (!sessionId.isNullOrEmpty()) {
                Timber.v("Using sessionid $sessionId for device $deviceKey")

                payloadJson["recipient"] = deviceInfo.userId
                payloadJson["recipient_keys"] = mapOf("ed25519" to deviceInfo.fingerprint()!!)

                val payloadString = convertToUTF8(JsonCanonicalizer.getCanonicalJson(Map::class.java, payloadJson))
                ciphertext[deviceKey] = olmDevice.encryptMessage(deviceKey, sessionId, payloadString)!!
            }
        }

        return EncryptedMessage(
                algorithm = MXCRYPTO_ALGORITHM_OLM,
                senderKey = olmDevice.deviceCurve25519Key,
                cipherText = ciphertext
        )
    }
}
