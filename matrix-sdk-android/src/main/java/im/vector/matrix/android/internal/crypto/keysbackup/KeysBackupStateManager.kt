/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.keysbackup

import android.os.Handler
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import timber.log.Timber
import java.util.*

internal class KeysBackupStateManager(private val uiHandler: Handler) {

    private val mListeners = ArrayList<KeysBackupService.KeysBackupStateListener>()

    // Backup state
    var state = KeysBackupState.Unknown
        set(newState) {
            Timber.d("KeysBackup", "setState: $field -> $newState")

            field = newState

            // Notify listeners about the state change, on the ui thread
            uiHandler.post {
                synchronized(mListeners) {
                    mListeners.forEach {
                        // Use newState because state may have already changed again
                        it.onStateChange(newState)
                    }
                }
            }
        }

    val isEnabled: Boolean
        get() = state == KeysBackupState.ReadyToBackUp
                || state == KeysBackupState.WillBackUp
                || state == KeysBackupState.BackingUp

    // True if unknown or bad state
    val isStucked: Boolean
        get() = state == KeysBackupState.Unknown
                || state == KeysBackupState.Disabled
                || state == KeysBackupState.WrongBackUpVersion
                || state == KeysBackupState.NotTrusted

    fun addListener(listener: KeysBackupService.KeysBackupStateListener) {
        synchronized(mListeners) {
            mListeners.add(listener)
        }
    }

    fun removeListener(listener: KeysBackupService.KeysBackupStateListener) {
        synchronized(mListeners) {
            mListeners.remove(listener)
        }
    }
}
