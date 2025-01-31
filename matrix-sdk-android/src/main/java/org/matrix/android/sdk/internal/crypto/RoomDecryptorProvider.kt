/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.internal.crypto.algorithms.IMXDecrypting
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmDecryptionFactory
import org.matrix.android.sdk.internal.crypto.algorithms.olm.MXOlmDecryptionFactory
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class RoomDecryptorProvider @Inject constructor(
        private val olmDecryptionFactory: MXOlmDecryptionFactory,
        private val megolmDecryptionFactory: MXMegolmDecryptionFactory
) {

    // A map from algorithm to MXDecrypting instance, for each room
    private val roomDecryptors: MutableMap<String /* room id */, MutableMap<String /* algorithm */, IMXDecrypting>> = HashMap()

    private val newSessionListeners = ArrayList<NewSessionListener>()

    fun addNewSessionListener(listener: NewSessionListener) {
        if (!newSessionListeners.contains(listener)) newSessionListeners.add(listener)
    }

    fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
    }

    /**
     * Get a decryptor for a given room and algorithm.
     * If we already have a decryptor for the given room and algorithm, return
     * it. Otherwise try to instantiate it.
     *
     * @param roomId the room id
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
        if (roomId != null && roomId.isNotEmpty()) {
            synchronized(roomDecryptors) {
                val decryptors = roomDecryptors.getOrPut(roomId) { mutableMapOf() }
                val alg = decryptors[algorithm]
                if (alg != null) {
                    return alg
                }
            }
        }
        val decryptingClass = MXCryptoAlgorithms.hasDecryptorClassForAlgorithm(algorithm)
        if (decryptingClass) {
            val alg = when (algorithm) {
                MXCRYPTO_ALGORITHM_MEGOLM -> megolmDecryptionFactory.create().apply {
                    this.newSessionListener = object : NewSessionListener {
                        override fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
                            // PR reviewer: the parameter has been renamed so is now in conflict with the parameter of getOrCreateRoomDecryptor
                            newSessionListeners.toList().forEach {
                                try {
                                    it.onNewSession(roomId, senderKey, sessionId)
                                } catch (ignore: Throwable) {
                                }
                            }
                        }
                    }
                }
                else -> olmDecryptionFactory.create()
            }
            if (!roomId.isNullOrEmpty()) {
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
