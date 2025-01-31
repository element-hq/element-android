/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * The algorithm used for storing backups. Currently, only "m.megolm_backup.v1.curve25519-aes-sha2" is defined.
     */
    val algorithm: String

    /**
     * algorithm-dependent data, for "m.megolm_backup.v1.curve25519-aes-sha2" see [org.matrix.android.sdk.internal.crypto.keysbackup.MegolmBackupAuthData].
     */
    val authData: JsonDict

    /**
     * Facility method to convert authData to a MegolmBackupAuthData object.
     */
    fun getAuthDataAsMegolmBackupAuthData(): MegolmBackupAuthData? {
        return MoshiProvider.providesMoshi()
                .takeIf { algorithm == MXCRYPTO_ALGORITHM_MEGOLM_BACKUP }
                ?.adapter(MegolmBackupAuthData::class.java)
                ?.fromJsonValue(authData)
    }
}
