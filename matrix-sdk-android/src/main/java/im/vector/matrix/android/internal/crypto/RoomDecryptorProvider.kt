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

package im.vector.matrix.android.internal.crypto

import android.text.TextUtils
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmDecryptionFactory
import im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmDecryptionFactory
import timber.log.Timber
import java.util.*

internal class RoomDecryptorProvider(
        private val olmDecryptionFactory: MXOlmDecryptionFactory,
        private val megolmDecryptionFactory: MXMegolmDecryptionFactory
) {

    // A map from algorithm to MXDecrypting instance, for each room
    private val roomDecryptors: MutableMap<String /* room id */, MutableMap<String /* algorithm */, IMXDecrypting>> = HashMap()

    /**
     * Get a decryptor for a given room and algorithm.
     * If we already have a decryptor for the given room and algorithm, return
     * it. Otherwise try to instantiate it.
     *
     * @param roomId    the room id
     * @param algorithm the crypto algorithm
     * @return the decryptor
     * // TODO Create another method for the case of roomId is null
     */
    fun getOrCreateRoomDecryptor(roomId: String?, algorithm: String?): IMXDecrypting? {
        // sanity check
        if (algorithm.isNullOrEmpty()) {
            Timber.e("## getRoomDecryptor() : null algorithm")
            return null
        }
        if(roomId != null && roomId.isNotEmpty()) {
            synchronized(roomDecryptors) {
                if (!roomDecryptors.containsKey(roomId)) {
                    roomDecryptors[roomId] = HashMap()
                }
                val alg = roomDecryptors[roomId]?.get(algorithm)
                if (alg != null) {
                    return alg
                }
            }
        }
        val decryptingClass = MXCryptoAlgorithms.hasDecryptorClassForAlgorithm(algorithm)
        if (decryptingClass) {
            val alg = when (algorithm) {
                MXCRYPTO_ALGORITHM_MEGOLM -> megolmDecryptionFactory.create()
                else                      -> olmDecryptionFactory.create()
            }
            if (roomId != null && !TextUtils.isEmpty(roomId)) {
                synchronized(roomDecryptors) {
                    roomDecryptors[roomId]?.put(algorithm, alg)
                }
            }
            return alg
        }
        return null
    }

    fun getRoomDecryptor(roomId: String?, algorithm: String?): IMXDecrypting? {
        if (roomId == null || algorithm == null) {
            return null
        }
        return roomDecryptors[roomId]?.get(algorithm)
    }
}