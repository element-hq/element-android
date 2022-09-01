/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.keysbackup.algorithm

import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.di.MoshiProvider

internal interface KeysBackupAlgorithm {

    val authData: MegolmBackupAuthData
    val untrusted: Boolean

    fun setPrivateKey(privateKey: ByteArray)
    fun encryptSession(sessionData: MegolmSessionData): JsonDict?
    fun decryptSessions(data: KeysBackupData): List<MegolmSessionData>
    fun keyMatches(privateKey: ByteArray): Boolean
    fun release()
}

internal fun KeysBackupAlgorithm.createMegolmSessionData(decrypted: String, sessionId: String, roomId: String): MegolmSessionData? {
    val moshi = MoshiProvider.providesMoshi()
    val adapter = moshi.adapter(MegolmSessionData::class.java)
    val sessionBackupData: MegolmSessionData = adapter.fromJson(decrypted) ?: return null
    return sessionBackupData.copy(
            sessionId = sessionId,
            roomId = roomId,
            untrusted = untrusted
    )
}

internal fun MegolmSessionData.asBackupJson(): String {
    val sessionBackupData = mapOf(
            "algorithm" to algorithm,
            "sender_key" to senderKey,
            "sender_claimed_keys" to senderClaimedKeys,
            "forwarding_curve25519_key_chain" to (forwardingCurve25519KeyChain.orEmpty()),
            "session_key" to sessionKey,
            "org.matrix.msc3061.shared_history" to sharedHistory,
            "untrusted" to untrusted
    )
    return MoshiProvider.providesMoshi()
            .adapter(Map::class.java)
            .toJson(sessionBackupData)
}
