/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.rustcomponents.sdk.crypto.BackupRecoveryKey as InnerBackupRecoveryKey

class BackupRecoveryKey internal constructor(internal val inner: InnerBackupRecoveryKey) : IBackupRecoveryKey {

    constructor() : this(InnerBackupRecoveryKey())

    companion object {

        fun fromBase58(key: String): BackupRecoveryKey {
            val inner = InnerBackupRecoveryKey.fromBase58(key)
            return BackupRecoveryKey(inner)
        }

        fun fromBase64(key: String): BackupRecoveryKey {
            val inner = InnerBackupRecoveryKey.fromBase64(key)
            return BackupRecoveryKey(inner)
        }

        fun fromPassphrase(passphrase: String, salt: String, rounds: Int): BackupRecoveryKey {
            val inner = InnerBackupRecoveryKey.fromPassphrase(passphrase, salt, rounds)
            return BackupRecoveryKey(inner)
        }

        fun newFromPassphrase(passphrase: String): BackupRecoveryKey {
            val inner = InnerBackupRecoveryKey.newFromPassphrase(passphrase)
            return BackupRecoveryKey(inner)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BackupRecoveryKey) return false
        return this.toBase58() == other.toBase58()
    }

    override fun hashCode(): Int {
        return toBase58().hashCode()
    }

    override fun toBase58() = inner.toBase58()

    override fun toBase64() = inner.toBase64()

    override fun decryptV1(ephemeralKey: String, mac: String, ciphertext: String) = inner.decryptV1(ephemeralKey, mac, ciphertext)

    override fun megolmV1PublicKey() = megolmV1Key

    private val megolmV1Key = object : IMegolmV1PublicKey {
        override val publicKey: String
            get() = inner.megolmV1PublicKey().publicKey
        override val privateKeySalt: String?
            get() = inner.megolmV1PublicKey().passphraseInfo?.privateKeySalt
        override val privateKeyIterations: Int?
            get() = inner.megolmV1PublicKey().passphraseInfo?.privateKeyIterations

        override val backupAlgorithm: String
            get() = inner.megolmV1PublicKey().backupAlgorithm
    }
}
