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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_OLM
import org.matrix.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.algorithms.olm.MXOlmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class RoomEncryptorsStore @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        private val megolmEncryptionFactory: MXMegolmEncryptionFactory,
        private val olmEncryptionFactory: MXOlmEncryptionFactory,
) {

    // MXEncrypting instance for each room.
    private val roomEncryptors = mutableMapOf<String, IMXEncrypting>()

    fun put(roomId: String, alg: IMXEncrypting) {
        synchronized(roomEncryptors) {
            roomEncryptors.put(roomId, alg)
        }
    }

    fun get(roomId: String): IMXEncrypting? {
        return synchronized(roomEncryptors) {
            val cache = roomEncryptors[roomId]
            if (cache != null) {
                return@synchronized cache
            } else {
                val alg: IMXEncrypting? = when (cryptoStore.getRoomAlgorithm(roomId)) {
                    MXCRYPTO_ALGORITHM_MEGOLM -> megolmEncryptionFactory.create(roomId)
                    MXCRYPTO_ALGORITHM_OLM -> olmEncryptionFactory.create(roomId)
                    else -> null
                }
                alg?.let { roomEncryptors.put(roomId, it) }
                return@synchronized alg
            }
        }
    }
}
