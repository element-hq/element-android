/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms

internal interface IMXGroupEncryption {

    /**
     * In Megolm, each recipient maintains a record of the ratchet value which allows
     * them to decrypt any messages sent in the session after the corresponding point
     * in the conversation. If this value is compromised, an attacker can similarly
     * decrypt past messages which were encrypted by a key derived from the
     * compromised or subsequent ratchet values. This gives 'partial' forward
     * secrecy.
     *
     * To mitigate this issue, the application should offer the user the option to
     * discard historical conversations, by winding forward any stored ratchet values,
     * or discarding sessions altogether.
     */
    fun discardSessionKey()

    suspend fun preshareKey(userIds: List<String>)

    /**
     * Re-shares a session key with devices if the key has already been
     * sent to them.
     *
     * @param groupSessionId The id of the outbound session to share.
     * @param userId The id of the user who owns the target device.
     * @param deviceId The id of the target device.
     * @param senderKey The key of the originating device for the session.
     *
     * @return true in case of success
     */
    suspend fun reshareKey(
            groupSessionId: String,
            userId: String,
            deviceId: String,
            senderKey: String
    ): Boolean
}
