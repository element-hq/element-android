/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

/**
 * Data retrieved from Olm library. algorithm and authData will be send to the homeserver, and recoveryKey will be displayed to the user
 */
data class MegolmBackupCreationInfo(
        /**
         * The algorithm used for storing backups [org.matrix.androidsdk.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP].
         */
        val algorithm: String,

        /**
         * Authentication data.
         */
        val authData: MegolmBackupAuthData,

        /**
         * The Base58 recovery key.
         */
        val recoveryKey: String
)
