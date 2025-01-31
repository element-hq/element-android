/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.crypto

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM

interface RoomCryptoService {

    fun isEncrypted(): Boolean

    fun encryptionAlgorithm(): String?

    fun shouldEncryptForInvitedMembers(): Boolean

    /**
     * Enable encryption of the room.
     * @param algorithm the algorithm to set, default to [MXCRYPTO_ALGORITHM_MEGOLM]
     * @param force Use force to ensure that this algorithm will be used. Otherwise this call
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
