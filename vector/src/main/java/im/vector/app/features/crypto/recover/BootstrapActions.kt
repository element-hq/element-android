/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.crypto.recover

import im.vector.app.core.platform.VectorViewModelAction
import java.io.OutputStream

sealed class BootstrapActions : VectorViewModelAction {

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
