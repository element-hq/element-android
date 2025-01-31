/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    val isStuck: Boolean
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
