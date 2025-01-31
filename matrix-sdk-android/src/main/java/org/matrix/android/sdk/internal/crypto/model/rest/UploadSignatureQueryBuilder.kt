/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.toRest
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.toRest

/**
 * Helper class to build CryptoApi#uploadSignatures params.
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
