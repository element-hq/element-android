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

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCurve25519AuthData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.olm.OlmException
import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption
import org.matrix.olm.OlmPkMessage
import timber.log.Timber
import java.security.InvalidParameterException

internal class KeysBackupCurve25519Algorithm(keysVersions: KeysVersionResult) : KeysBackupAlgorithm {

    override val untrusted: Boolean = true

    private val curveAuthData: MegolmBackupCurve25519AuthData
    private val publicKey: String

    private val encryptionKey: OlmPkEncryption
    private var privateKey: ByteArray? = null

    init {
        if (keysVersions.algorithm != MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP) {
            throw IllegalStateException("Algorithm doesn't match")
        }
        curveAuthData = keysVersions.getAuthDataAsMegolmBackupAuthData() as MegolmBackupCurve25519AuthData
        publicKey = curveAuthData.publicKey ?: throw IllegalStateException("No public key")
        encryptionKey = OlmPkEncryption().apply {
            setRecipientKey(publicKey)
        }
    }

    override fun setPrivateKey(privateKey: ByteArray) {
        this.privateKey = privateKey
    }

    override fun encryptSession(sessionData: MegolmSessionData): JsonDict? {
        val encryptedSessionBackupData = try {
            val sessionDataJson = sessionData.asBackupJson()
            encryptionKey.encrypt(sessionDataJson)
        } catch (e: OlmException) {
            Timber.e(e, "Error while encrypting backup data.")
            throw e
        } ?: return null

        return mapOf(
                "ciphertext" to encryptedSessionBackupData.mCipherText,
                "mac" to encryptedSessionBackupData.mMac,
                "ephemeral" to encryptedSessionBackupData.mEphemeralKey
        )
    }

    override fun decryptSessions(data: KeysBackupData): List<MegolmSessionData> {
        fun pkDecryptionFromPrivateKey(privateKey: ByteArray): OlmPkDecryption? {
            var decryption: OlmPkDecryption? = null
            try {
                decryption = OlmPkDecryption()
                decryption.setPrivateKey(privateKey)
            } catch (e: OlmException) {
                Timber.e(e, "OlmException")
            }
            return decryption
        }

        val privateKey = this.privateKey
        if (privateKey == null || !keyMatches(privateKey)) {
            Timber.e("Invalid recovery key for this keys version")
            throw InvalidParameterException("Invalid recovery key")
        }
        // Get a PK decryption instance
        val decryption = pkDecryptionFromPrivateKey(privateKey)
        if (decryption == null) {
            // This should not happen anymore
            Timber.e("Invalid recovery key. Error")
            throw InvalidParameterException("Invalid recovery key")
        }
        val sessionsData = ArrayList<MegolmSessionData>()
        // Restore that data
        var sessionsFromHsCount = 0
        for ((roomIdLoop, backupData) in data.roomIdToRoomKeysBackupData) {
            for ((sessionIdLoop, keyBackupData) in backupData.sessionIdToKeyBackupData) {
                sessionsFromHsCount++
                val sessionData = decryptSession(keyBackupData.sessionData, sessionIdLoop, roomIdLoop, decryption)
                sessionData?.let {
                    sessionsData.add(it)
                }
            }
        }
        decryption.releaseDecryption()
        Timber.v(
                "Decrypted ${sessionsData.size} keys out of $sessionsFromHsCount from the backup store on the homeserver"
        )
        return sessionsData
    }

    private fun decryptSession(sessionData: JsonDict, sessionId: String, roomId: String, decryption: OlmPkDecryption): MegolmSessionData? {
        val ciphertext = sessionData["ciphertext"]?.toString()
        val mac = sessionData["mac"]?.toString()
        val ephemeralKey = sessionData["ephemeral"]?.toString()

        return if (ciphertext != null && mac != null && ephemeralKey != null) {
            val encrypted = OlmPkMessage()
            encrypted.mCipherText = ciphertext
            encrypted.mMac = mac
            encrypted.mEphemeralKey = ephemeralKey
            try {
                val decrypted = decryption.decrypt(encrypted)
                createMegolmSessionData(decrypted, sessionId, roomId)
            } catch (e: OlmException) {
                Timber.e(e, "Exception while decrypting")
                null
            }
        } else {
            null
        }
    }

    override fun release() {
        encryptionKey.releaseEncryption()
    }

    override val authData: MegolmBackupAuthData = curveAuthData

    override fun keyMatches(privateKey: ByteArray): Boolean {
        fun pkPublicKeyFromPrivateKey(privateKey: ByteArray): String? {
            // Built the PK decryption with it
            val decryption = OlmPkDecryption()
            val pkPublicKey = try {
                decryption.setPrivateKey(privateKey)
            } catch (e: OlmException) {
                null
            } finally {
                decryption.releaseDecryption()
            }
            return pkPublicKey
        }

        val publicKey = pkPublicKeyFromPrivateKey(privateKey)
        if (publicKey == null) {
            Timber.w("Public key is null")
            return false
        }
        // Compare both
        if (publicKey != this.publicKey) {
            Timber.w("Public keys mismatch")
            return false
        }
        // Public keys match!
        return true
    }
}
