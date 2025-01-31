/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

/**
 * Data model for response to [KeysBackup.getKeysBackupTrust()].
 */
data class KeysBackupVersionTrust(
        /**
         * Flag to indicate if the backup is trusted.
         * true if there is a signature that is valid & from a trusted device.
         */
        val usable: Boolean,

        /**
         * Signatures found in the backup version.
         */
        val signatures: List<KeysBackupVersionTrustSignature> = emptyList()
)
