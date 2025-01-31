/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.crypto

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import java.security.InvalidParameterException

internal class DefaultRoomCryptoService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val cryptoService: CryptoService,
        private val sendStateTask: SendStateTask,
) : RoomCryptoService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultRoomCryptoService
    }

    override fun isEncrypted(): Boolean {
        return cryptoService.isRoomEncrypted(roomId)
    }

    override fun encryptionAlgorithm(): String? {
        return cryptoService.getEncryptionAlgorithm(roomId)
    }

    override fun shouldEncryptForInvitedMembers(): Boolean {
        return cryptoService.shouldEncryptForInvitedMembers(roomId)
    }

    override suspend fun prepareToEncrypt() {
        awaitCallback<Unit> {
            cryptoService.prepareToEncrypt(roomId, it)
        }
    }

    override suspend fun enableEncryption(algorithm: String, force: Boolean) {
        when {
            (!force && isEncrypted() && encryptionAlgorithm() == MXCRYPTO_ALGORITHM_MEGOLM) -> {
                throw IllegalStateException("Encryption is already enabled for this room")
            }
            (!force && algorithm != MXCRYPTO_ALGORITHM_MEGOLM) -> {
                throw InvalidParameterException("Only MXCRYPTO_ALGORITHM_MEGOLM algorithm is supported")
            }
            else -> {
                val params = SendStateTask.Params(
                        roomId = roomId,
                        stateKey = "",
                        eventType = EventType.STATE_ROOM_ENCRYPTION,
                        body = mapOf(
                                "algorithm" to algorithm
                        )
                )

                sendStateTask.execute(params)
            }
        }
    }
}
