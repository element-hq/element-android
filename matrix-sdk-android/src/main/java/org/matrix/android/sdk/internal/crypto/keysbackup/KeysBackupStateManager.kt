/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

import android.os.Handler
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import timber.log.Timber

internal class KeysBackupStateManager(private val uiHandler: Handler) {

    private val listeners = ArrayList<KeysBackupStateListener>()

    // Backup state
    var state = KeysBackupState.Unknown
        set(newState) {
            Timber.v("KeysBackup: setState: $field -> $newState")

            field = newState

            // Notify listeners about the state change, on the ui thread
            uiHandler.post {
                synchronized(listeners) {
                    listeners.forEach {
                        // Use newState because state may have already changed again
                        it.onStateChange(newState)
                    }
                }
            }
        }

    val isEnabled: Boolean
        get() = state == KeysBackupState.ReadyToBackUp ||
                state == KeysBackupState.WillBackUp ||
                state == KeysBackupState.BackingUp

    // True if unknown or bad state
    val isStucked: Boolean
        get() = state == KeysBackupState.Unknown ||
                state == KeysBackupState.Disabled ||
                state == KeysBackupState.WrongBackUpVersion ||
                state == KeysBackupState.NotTrusted

    fun addListener(listener: KeysBackupStateListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: KeysBackupStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
}
