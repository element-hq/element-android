/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore

internal class SharedWithHelper(
        private val roomId: String,
        private val sessionId: String,
        private val cryptoStore: IMXCryptoStore
) {

    fun sharedWithDevices(): MXUsersDevicesMap<Int> {
        return cryptoStore.getSharedWithInfo(roomId, sessionId)
    }

    fun markedSessionAsShared(deviceInfo: CryptoDeviceInfo, chainIndex: Int) {
        cryptoStore.markedSessionAsShared(
                roomId = roomId,
                sessionId = sessionId,
                userId = deviceInfo.userId,
                deviceId = deviceInfo.deviceId,
                deviceIdentityKey = deviceInfo.identityKey() ?: "",
                chainIndex = chainIndex
        )
    }
}
