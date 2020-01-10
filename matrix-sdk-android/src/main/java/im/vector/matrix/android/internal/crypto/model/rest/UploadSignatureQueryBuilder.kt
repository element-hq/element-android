/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.model.rest

import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXKeysObject

/**
 * Helper class to build CryptoApi#uploadSignatures params
 */
data class UploadSignatureQueryBuilder(
        private val deviceInfoList: ArrayList<MXDeviceInfo> = ArrayList(),
        private val signingKeyInfoList: ArrayList<CrossSigningKeyInfo> = ArrayList()
) {

    fun withDeviceInfo(deviceInfo: MXDeviceInfo) = apply {
        deviceInfoList.add(deviceInfo)
    }

    fun withSigningKeyInfo(info: CrossSigningKeyInfo) = apply {
        signingKeyInfoList.add(info)
    }

    fun build(): Map<String, Map<String, MXKeysObject>> {
        val map = HashMap<String, HashMap<String, MXKeysObject>>()

        val usersList = (deviceInfoList.map { it.userId } + signingKeyInfoList.mapNotNull { it.userId }).distinct()

        usersList.forEach { userID ->
            val userMap = HashMap<String, MXKeysObject>()
            deviceInfoList.filter { it.userId == userID }.forEach { deviceInfo ->
                userMap[deviceInfo.deviceId] = deviceInfo.toDeviceKeys()
            }
            signingKeyInfoList.filter { it.userId == userID }.forEach { keyInfo ->
                keyInfo.unpaddedBase64PublicKey?.let { base64Key ->
                    userMap[base64Key] = keyInfo
                }
            }
            map[userID] = userMap
        }

        return map
    }

}
