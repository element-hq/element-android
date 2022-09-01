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

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_AES_256_BACKUP
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAes256AuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.tools.AesHmacSha2
import org.matrix.olm.OlmException
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.Arrays

internal class KeysBackupAes256Algorithm(keysVersions: KeysVersionResult) : KeysBackupAlgorithm {

    override val untrusted: Boolean = false

    private val aesAuthData: MegolmBackupAes256AuthData
    private var privateKey: ByteArray? = null

    init {
        if (keysVersions.algorithm != MXCRYPTO_ALGORITHM_AES_256_BACKUP) {
            throw IllegalStateException("Algorithm doesn't match")
        }
        aesAuthData = keysVersions.getAuthDataAsMegolmBackupAuthData() as MegolmBackupAes256AuthData
    }

    override fun setPrivateKey(privateKey: ByteArray) {
        this.privateKey = privateKey
    }

    override fun encryptSession(sessionData: MegolmSessionData): JsonDict? {
        val privateKey = privateKey
        if (privateKey == null || !keyMatches(privateKey)) {
            Timber.e("Key does not match")
            throw IllegalStateException("Key does not match")
        }
        val encryptedSessionBackupData = try {
            val sessionDataJson = sessionData.asBackupJson()
            AesHmacSha2.encrypt(privateKey = privateKey, secretName = sessionData.sessionId ?: "", sessionDataJson)
        } catch (e: OlmException) {
            Timber.e(e, "Error while encrypting backup data.")
            null
        } ?: return null

        return mapOf(
                "ciphertext" to encryptedSessionBackupData.cipherRawBytes.toBase64NoPadding(),
                "mac" to encryptedSessionBackupData.mac.toBase64NoPadding(),
                "iv" to encryptedSessionBackupData.initializationVector.toBase64NoPadding()
        )
    }

    override fun decryptSessions(data: KeysBackupData): List<MegolmSessionData> {
        val privateKey = privateKey
        if (privateKey == null || !keyMatches(privateKey)) {
            Timber.e("Invalid recovery key for this keys version")
            throw InvalidParameterException("Invalid recovery key")
        }
        val sessionsData = ArrayList<MegolmSessionData>()
        // Restore that data
        var sessionsFromHsCount = 0
        for ((roomIdLoop, backupData) in data.roomIdToRoomKeysBackupData) {
            for ((sessionIdLoop, keyBackupData) in backupData.sessionIdToKeyBackupData) {
                sessionsFromHsCount++
                val sessionData = decryptSession(keyBackupData.sessionData, sessionIdLoop, roomIdLoop, privateKey)
                sessionData?.let {
                    sessionsData.add(it)
                }
            }
        }
        Timber.v(
                "Decrypted ${sessionsData.size} keys out of $sessionsFromHsCount from the backup store on the homeserver"
        )
        return sessionsData
    }

    private fun decryptSession(sessionData: JsonDict, sessionId: String, roomId: String, privateKey: ByteArray): MegolmSessionData? {
        val cipherRawBytes = sessionData["ciphertext"]?.toString()?.fromBase64() ?: return null
        val mac = sessionData["mac"]?.toString()?.fromBase64() ?: throw IllegalStateException("Bad mac")
        val iv = sessionData["iv"]?.toString()?.fromBase64() ?: ByteArray(16)

        val encryptionInfo = AesHmacSha2.EncryptionInfo(
                cipherRawBytes = cipherRawBytes,
                mac = mac,
                initializationVector = iv
        )
        return try {
            val decrypted = AesHmacSha2.decrypt(privateKey, sessionId, encryptionInfo)
            createMegolmSessionData(decrypted, sessionId, roomId)
        } catch (e: Exception) {
            Timber.e(e, "Exception while decrypting")
            null
        }
    }

    override fun release() {
        privateKey?.apply {
            Arrays.fill(this, 0)
        }
        privateKey = null
    }

    override val authData: MegolmBackupAuthData = aesAuthData

    override fun keyMatches(privateKey: ByteArray): Boolean {
        return if (aesAuthData.mac != null) {
            val keyCheckMac = AesHmacSha2.calculateKeyCheck(privateKey, aesAuthData.iv).mac
            val authDataMac = aesAuthData.mac.fromBase64()
            return keyCheckMac.contentEquals(authDataMac)
        } else {
            // if we have no information, we have to assume the key is right
            true
        }
    }
}
