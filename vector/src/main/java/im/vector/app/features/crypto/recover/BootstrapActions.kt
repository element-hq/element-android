/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import im.vector.app.core.platform.VectorViewModelAction
import java.io.OutputStream

sealed class BootstrapActions : VectorViewModelAction {
    object Retry : BootstrapActions()

    // Navigation

    object GoBack : BootstrapActions()
    data class GoToConfirmPassphrase(val passphrase: String) : BootstrapActions()
    object GoToCompleted : BootstrapActions()
    object GoToEnterAccountPassword : BootstrapActions()

    data class Start(val userWantsToEnterPassphrase: Boolean) : BootstrapActions()

    object StartKeyBackupMigration : BootstrapActions()

    data class DoInitialize(val passphrase: String) : BootstrapActions()
    object DoInitializeGeneratedKey : BootstrapActions()
    data class UpdateCandidatePassphrase(val pass: String) : BootstrapActions()
    data class UpdateConfirmCandidatePassphrase(val pass: String) : BootstrapActions()

    //    data class ReAuth(val pass: String) : BootstrapActions()
    object RecoveryKeySaved : BootstrapActions()
    object Completed : BootstrapActions()
    object SaveReqQueryStarted : BootstrapActions()
    data class SaveKeyToUri(val os: OutputStream) : BootstrapActions()
    object SaveReqFailed : BootstrapActions()

    object HandleForgotBackupPassphrase : BootstrapActions()
    data class DoMigrateWithPassphrase(val passphrase: String) : BootstrapActions()
    data class DoMigrateWithRecoveryKey(val recoveryKey: String) : BootstrapActions()

    object SsoAuthDone : BootstrapActions()
    data class PasswordAuthDone(val password: String) : BootstrapActions()
    object ReAuthCancelled : BootstrapActions()
}
