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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.tools.withOlmDecryption
import org.matrix.olm.OlmPkMessage

class BackupRecoveryKey(private val key: ByteArray) : IBackupRecoveryKey {

    override fun equals(other: Any?): Boolean {
        if (other !is BackupRecoveryKey) return false
        return this.toBase58() == other.toBase58()
    }

    override fun hashCode(): Int {
        return key.contentHashCode()
    }

    override fun toBase58() = computeRecoveryKey(key)

    override fun toBase64() = key.toBase64NoPadding()

    override fun decryptV1(ephemeralKey: String, mac: String, ciphertext: String): String = withOlmDecryption {
        it.setPrivateKey(key)
        it.decrypt(OlmPkMessage().apply {
            this.mEphemeralKey = ephemeralKey
            this.mCipherText = ciphertext
            this.mMac = mac
        })
    }

    override fun megolmV1PublicKey() = v1pk

    private val v1pk = object : IMegolmV1PublicKey {
        override val publicKey: String
            get() = withOlmDecryption {
                it.setPrivateKey(key)
            }
        override val privateKeySalt: String?
            get() = null // not use in kotlin sdk
        override val privateKeyIterations: Int?
            get() = null // not use in kotlin sdk
        override val backupAlgorithm: String
            get() = "" // not use in kotlin sdk
    }
}
