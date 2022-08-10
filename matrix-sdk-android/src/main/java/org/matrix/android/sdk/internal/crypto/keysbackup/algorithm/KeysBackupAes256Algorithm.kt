/*
 * Copyright (c) 2022 New Vector Ltd
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
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData

internal class KeysBackupAes256Algorithm(keysVersions: KeysVersionResult) : KeysBackupAlgorithm {

    override val untrusted: Boolean = true

    private val aesAuthData: MegolmBackupAes256AuthData

    init {
        if (keysVersions.algorithm != MXCRYPTO_ALGORITHM_AES_256_BACKUP) {
            throw IllegalStateException("Algorithm doesn't match")
        }
        aesAuthData = keysVersions.getAuthDataAsMegolmBackupAuthData() as MegolmBackupAes256AuthData
    }

    override fun encryptSession(sessionData: MegolmSessionData): JsonDict? {
        TODO("Not yet implemented")
    }

    override fun decryptSessions(recoveryKey: String, data: KeysBackupData): List<MegolmSessionData> {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }

    override val authData: MegolmBackupAuthData = aesAuthData

    override fun keyMatches(key: String): Boolean {
        TODO("Not yet implemented")
    }
}
