/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.keysbackup

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener

internal class BackupStateHelper(
        private val keysBackup: KeysBackupService) : KeysBackupStateListener {

    init {
        keysBackup.addListener(this)
    }

    val hasBackedUpOnce = CompletableDeferred<Unit>()

    var backingUpOnce = false

    override fun onStateChange(newState: KeysBackupState) {
        Log.d("#E2E", "Keybackup onStateChange $newState")
        if (newState == KeysBackupState.BackingUp) {
            backingUpOnce = true
        }
        if (newState == KeysBackupState.ReadyToBackUp || newState == KeysBackupState.WillBackUp) {
            if (backingUpOnce) {
                hasBackedUpOnce.complete(Unit)
            }
        }
    }
}
