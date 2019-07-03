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

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_OLM
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedMessage
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.convertToUTF8
import timber.log.Timber
import java.util.*
import javax.inject.Inject

internal class MessageEncrypter @Inject constructor(private val credentials: Credentials,
                                                    private val olmDevice: MXOlmDevice) {

    /**
     * Encrypt an event payload for a list of devices.
     * This method must be called from the getCryptoHandler() thread.
     *
     * @param payloadFields fields to include in the encrypted payload.
     * @param deviceInfos   list of device infos to encrypt for.
     * @return the content for an m.room.encrypted event.
     */
    fun encryptMessage(payloadFields: Map<String, Any>, deviceInfos: List<MXDeviceInfo>): EncryptedMessage {
        val deviceInfoParticipantKey = HashMap<String, MXDeviceInfo>()
        val participantKeys = ArrayList<String>()

        for (di in deviceInfos) {
            participantKeys.add(di.identityKey()!!)
            deviceInfoParticipantKey[di.identityKey()!!] = di
        }

        val payloadJson = HashMap(payloadFields)

        payloadJson["sender"] = credentials.userId
        payloadJson["sender_device"] = credentials.deviceId

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

        for (deviceKey in participantKeys) {
            val sessionId = olmDevice.getSessionId(deviceKey)

            if (!TextUtils.isEmpty(sessionId)) {
                Timber.v("Using sessionid $sessionId for device $deviceKey")
                val deviceInfo = deviceInfoParticipantKey[deviceKey]

                payloadJson["recipient"] = deviceInfo!!.userId

                val recipientsKeysMap = HashMap<String, String>()
                recipientsKeysMap["ed25519"] = deviceInfo.fingerprint()!!
                payloadJson["recipient_keys"] = recipientsKeysMap

                val payloadString = convertToUTF8(MoshiProvider.getCanonicalJson(Map::class.java, payloadJson))
                ciphertext[deviceKey] = olmDevice.encryptMessage(deviceKey, sessionId!!, payloadString!!)!!
            }
        }

        val res = EncryptedMessage()

        res.algorithm = MXCRYPTO_ALGORITHM_OLM
        res.senderKey = olmDevice.deviceCurve25519Key
        res.cipherText = ciphertext

        return res
    }

}