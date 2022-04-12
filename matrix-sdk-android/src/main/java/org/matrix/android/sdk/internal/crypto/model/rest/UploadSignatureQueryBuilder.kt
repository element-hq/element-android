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
package org.matrix.android.sdk.internal.crypto.model.rest

import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.toRest
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.toRest

/**
 * Helper class to build CryptoApi#uploadSignatures params
 */
internal data class UploadSignatureQueryBuilder(
        private val deviceInfoList: MutableList<CryptoDeviceInfo> = mutableListOf(),
        private val signingKeyInfoList: MutableList<CryptoCrossSigningKey> = mutableListOf()
) {

    fun withDeviceInfo(deviceInfo: CryptoDeviceInfo) = apply {
        deviceInfoList.add(deviceInfo)
    }

    fun withSigningKeyInfo(info: CryptoCrossSigningKey) = apply {
        signingKeyInfoList.add(info)
    }

    fun build(): Map<String, Map<String, @JvmSuppressWildcards Any>> {
        val map = HashMap<String, HashMap<String, Any>>()

        val usersList = (deviceInfoList.map { it.userId } + signingKeyInfoList.map { it.userId })
                .distinct()

        usersList.forEach { userID ->
            val userMap = HashMap<String, Any>()
            deviceInfoList.filter { it.userId == userID }.forEach { deviceInfo ->
                userMap[deviceInfo.deviceId] = deviceInfo.toRest()
            }
            signingKeyInfoList.filter { it.userId == userID }.forEach { keyInfo ->
                keyInfo.unpaddedBase64PublicKey?.let { base64Key ->
                    userMap[base64Key] = keyInfo.toRest()
                }
            }
            map[userID] = userMap
        }

        return map
    }
}
