/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import im.vector.app.features.raw.wellknown.SecureBackupMethod

/**
 * TODO The schema is not up to date
 *
 *                        ┌───────────────────────────────────┐
 *                        │  BootstrapStep.SetupSecureBackup  │
 *                        └───────────────────────────────────┘
 *                                          │
 *                                          │
 *                                          ▼
 *                             ┌─────────────────────────┐
 *                             │ User has signing keys?  │──────────── Account
 *                             └─────────────────────────┘            Creation ?
 *                                          │                              │
 *                                          No                             │
 *                                          │                              │
 *                                          │                              │
 *                                          ▼                              │
 *                        ┌───────────────────────────────────┐            │
 *                        │  BootstrapStep.CheckingMigration  │            │
 *                        └───────────────────────────────────┘            │
 *                                          │                              │
 *                                          │                              │
 *                           Existing       ├─────────No ───────┐          │
 *                     ┌────Keybackup───────┘     KeyBackup     │          │
 *                     │                                        │          │
 *                     │                                        ▼          ▼
 *                     ▼                                    ┌────────────────────────────────────┐
 *     ┌─────────────────────────────────────────┐          │   BootstrapStep.SetupPassphrase    │◀─┐
 *     │BootstrapStep.GetBackupSecretForMigration│          └────────────────────────────────────┘  │
 *     └─────────────────────────────────────────┘                             │                    │
 *                             │                                               │                 ┌Back
 *                             │                                               ▼                 │
 *                             │                            ┌────────────────────────────────────┤
 *                             │                            │  BootstrapStep.ConfirmPassphrase   │──┐
 *                             │                            └────────────────────────────────────┘  │
 *                             │                                               │                    │
 *                             │                                      is password/reauth needed?    │
 *                             │                                               │                    │
 *                             │                                               ▼                    │
 *                             │                            ┌────────────────────────────────────┐  │
 *                             │                            │   BootstrapStep.AccountReAuth      │  │
 *                             │                            └────────────────────────────────────┘  │
 *                             │                                               │                    │
 *                             │                                               │                    │
 *                             │                            ┌──────────────────┘         password not needed (in
 *                             │                            │                                    memory)
 *                             │                            │                                       │
 *                             │                            ▼                                       │
 *                             │         ┌────────────────────────────────────┐                     │
 *                             └────────▶│     BootstrapStep.Initializing     │◀────────────────────┘
 *                                       └────────────────────────────────────┘
 *                                                          │
 *                                                          │
 *                                                          │
 *                                                          ▼
 *                                        ┌────────────────────────────────────┐
 *                                        │   BootstrapStep.SaveRecoveryKey    │
 *                                        └────────────────────────────────────┘
 *                                                          │
 *                                                          │
 *                                                          │
 *                                                          ▼
 *                                       ┌────────────────────────────────────────┐
 *                                       │       BootstrapStep.DoneSuccess        │
 *                                       └────────────────────────────────────────┘
 *
 */

sealed class BootstrapStep {
    // This is the first step
    object CheckingMigration : BootstrapStep()

    // Use will be asked to choose between passphrase or recovery key, or to start process if a key backup exists
    data class FirstForm(val keyBackUpExist: Boolean, val reset: Boolean = false, val methods: SecureBackupMethod) : BootstrapStep()

    object SetupPassphrase : BootstrapStep()
    object ConfirmPassphrase : BootstrapStep()

    data class AccountReAuth(val failure: String? = null) : BootstrapStep()

    abstract class GetBackupSecretForMigration : BootstrapStep()
    data class GetBackupSecretPassForMigration(val useKey: Boolean) : GetBackupSecretForMigration()
    object GetBackupSecretKeyForMigration : GetBackupSecretForMigration()

    object Initializing : BootstrapStep()
    data class SaveRecoveryKey(val isSaved: Boolean) : BootstrapStep()
    object DoneSuccess : BootstrapStep()

    data class Error(val error: Throwable) : BootstrapStep()
}

fun BootstrapStep.GetBackupSecretForMigration.useKey(): Boolean {
    return when (this) {
        is BootstrapStep.GetBackupSecretPassForMigration -> useKey
        else -> true
    }
}
