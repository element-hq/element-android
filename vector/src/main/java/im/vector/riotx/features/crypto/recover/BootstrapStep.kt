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

package im.vector.riotx.features.crypto.recover

/**
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
 *                             │                                      is password needed?           │
 *                             │                                               │                    │
 *                             │                                               ▼                    │
 *                             │                            ┌────────────────────────────────────┐  │
 *                             │                            │   BootstrapStep.AccountPassword    │  │
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
    object SetupSecureBackup : BootstrapStep()

    data class SetupPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()
    data class ConfirmPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()

    data class AccountPassword(val isPasswordVisible: Boolean, val failure: String? = null) : BootstrapStep()
    object CheckingMigration : BootstrapStep()

    abstract class GetBackupSecretForMigration : BootstrapStep()
    data class GetBackupSecretPassForMigration(val isPasswordVisible: Boolean, val useKey: Boolean) : GetBackupSecretForMigration()
    object GetBackupSecretKeyForMigration : GetBackupSecretForMigration()

    object Initializing : BootstrapStep()
    data class SaveRecoveryKey(val isSaved: Boolean) : BootstrapStep()
    object DoneSuccess : BootstrapStep()
}
