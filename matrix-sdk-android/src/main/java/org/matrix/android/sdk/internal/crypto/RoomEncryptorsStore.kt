/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
