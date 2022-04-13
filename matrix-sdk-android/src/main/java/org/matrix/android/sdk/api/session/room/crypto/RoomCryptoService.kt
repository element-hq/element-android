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

package org.matrix.android.sdk.api.session.room.crypto

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM

interface RoomCryptoService {

    fun isEncrypted(): Boolean

    fun encryptionAlgorithm(): String?

    fun shouldEncryptForInvitedMembers(): Boolean

    /**
     * Enable encryption of the room.
     * @param Use force to ensure that this algorithm will be used. Otherwise this call
     * will throw if encryption is already setup or if the algorithm is not supported. Only to
     * be used by admins to fix misconfigured encryption.
     */
    suspend fun enableEncryption(algorithm: String = MXCRYPTO_ALGORITHM_MEGOLM, force: Boolean = false)

    /**
     * Ensures all members of the room are loaded and outbound session keys are shared.
     * If this method is not called, CryptoService will ensure it before sending events.
     */
    suspend fun prepareToEncrypt()
}
