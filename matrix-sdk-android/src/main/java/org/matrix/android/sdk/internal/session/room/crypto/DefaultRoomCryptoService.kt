/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.crypto

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
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
        cryptoService.prepareToEncrypt(roomId)
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
