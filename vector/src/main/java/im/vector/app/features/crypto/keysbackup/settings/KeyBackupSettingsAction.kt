/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.crypto.keysbackup.settings

import im.vector.app.core.platform.VectorViewModelAction

sealed class KeyBackupSettingsAction : VectorViewModelAction {
    object Init : KeyBackupSettingsAction()
    object GetKeyBackupTrust : KeyBackupSettingsAction()
    object DeleteKeyBackup : KeyBackupSettingsAction()
    object SetUpKeyBackup : KeyBackupSettingsAction()
    data class StoreIn4SSuccess(val recoveryKey: String, val alias: String) : KeyBackupSettingsAction()
    object StoreIn4SReset : KeyBackupSettingsAction()
    object StoreIn4SFailure : KeyBackupSettingsAction()
}
