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

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore

internal class SharedWithHelper(
        private val roomId: String,
        private val sessionId: String,
        private val cryptoStore: IMXCryptoStore) {

    fun sharedWithDevices(): MXUsersDevicesMap<Int> {
        return cryptoStore.getSharedWithInfo(roomId, sessionId)
    }

    fun wasSharedWith(userId: String, deviceId: String): Int? {
        return cryptoStore.wasSessionSharedWithUser(roomId, sessionId, userId, deviceId).chainIndex
    }

    fun markedSessionAsShared(userId: String, deviceId: String, chainIndex: Int) {
        cryptoStore.markedSessionAsShared(roomId, sessionId, userId, deviceId, chainIndex)
    }
}
