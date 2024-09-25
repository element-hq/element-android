/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.crypto.keysbackup.settings

import im.vector.app.core.platform.VectorViewEvents

sealed class KeysBackupViewEvents : VectorViewEvents {
    object OpenLegacyCreateBackup : KeysBackupViewEvents()
    data class RequestStore4SSecret(val recoveryKey: String) : KeysBackupViewEvents()
}
