/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.actions

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_OLM
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedMessage
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.convertToUTF8
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("MessageEncrypter", LoggerTag.CRYPTO)

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
    suspend fun encryptMessage(payloadFields: Content, deviceInfos: List<CryptoDeviceInfo>): EncryptedMessage {
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
        payloadJson["keys"] = mapOf("ed25519" to olmDevice.deviceEd25519Key!!)

        val ciphertext = mutableMapOf<String, Any>()

        for ((deviceKey, deviceInfo) in deviceInfoParticipantKey) {
            val sessionId = olmDevice.getSessionId(deviceKey)

            if (!sessionId.isNullOrEmpty()) {
                Timber.tag(loggerTag.value).d("Using sessionid $sessionId for device $deviceKey")

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
