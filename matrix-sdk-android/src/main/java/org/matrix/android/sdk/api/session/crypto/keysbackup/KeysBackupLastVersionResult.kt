/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

sealed interface KeysBackupLastVersionResult {
    // No Keys backup found (404 error)
    object NoKeysBackup : KeysBackupLastVersionResult
    data class KeysBackup(val keysVersionResult: KeysVersionResult) : KeysBackupLastVersionResult
}

fun KeysBackupLastVersionResult.toKeysVersionResult(): KeysVersionResult? = when (this) {
    is KeysBackupLastVersionResult.KeysBackup -> keysVersionResult
    KeysBackupLastVersionResult.NoKeysBackup -> null
}
