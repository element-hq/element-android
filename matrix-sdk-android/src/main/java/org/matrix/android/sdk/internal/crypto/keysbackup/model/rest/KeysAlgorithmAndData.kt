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

package org.matrix.android.sdk.internal.crypto.keysbackup.model.rest

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * <pre>
 *     Example:
 *
 *     {
 *         "algorithm": "m.megolm_backup.v1.curve25519-aes-sha2",
 *         "auth_data": {
 *             "public_key": "abcdefg",
 *             "signatures": {
 *                 "something": {
 *                     "ed25519:something": "hijklmnop"
 *                 }
 *             }
 *         }
 *     }
 * </pre>
 */
internal interface KeysAlgorithmAndData {

    /**
     * The algorithm used for storing backups. Currently, only "m.megolm_backup.v1.curve25519-aes-sha2" is defined
     */
    val algorithm: String

    /**
     * algorithm-dependent data, for "m.megolm_backup.v1.curve25519-aes-sha2" see [org.matrix.android.sdk.internal.crypto.keysbackup.MegolmBackupAuthData]
     */
    val authData: JsonDict

    /**
     * Facility method to convert authData to a MegolmBackupAuthData object
     */
    fun getAuthDataAsMegolmBackupAuthData(): MegolmBackupAuthData? {
        return MoshiProvider.providesMoshi()
                .takeIf { algorithm == MXCRYPTO_ALGORITHM_MEGOLM_BACKUP }
                ?.adapter(MegolmBackupAuthData::class.java)
                ?.fromJsonValue(authData)
    }
}
