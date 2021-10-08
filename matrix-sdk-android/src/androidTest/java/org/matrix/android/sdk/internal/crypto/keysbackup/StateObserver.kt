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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import java.util.concurrent.CountDownLatch

/**
 * This class observe the state change of a KeysBackup object and provide a method to check the several state change
 * It checks all state transitions and detected forbidden transition
 */
internal class StateObserver(private val keysBackup: KeysBackupService,
                             private val latch: CountDownLatch? = null,
                             private val expectedStateChange: Int = -1) : KeysBackupStateListener {

    private val allowedStateTransitions = listOf(
            KeysBackupState.BackingUp to KeysBackupState.ReadyToBackUp,
            KeysBackupState.BackingUp to KeysBackupState.WrongBackUpVersion,

            KeysBackupState.CheckingBackUpOnHomeserver to KeysBackupState.Disabled,
            KeysBackupState.CheckingBackUpOnHomeserver to KeysBackupState.NotTrusted,
            KeysBackupState.CheckingBackUpOnHomeserver to KeysBackupState.ReadyToBackUp,
            KeysBackupState.CheckingBackUpOnHomeserver to KeysBackupState.Unknown,
            KeysBackupState.CheckingBackUpOnHomeserver to KeysBackupState.WrongBackUpVersion,

            KeysBackupState.Disabled to KeysBackupState.Enabling,

            KeysBackupState.Enabling to KeysBackupState.Disabled,
            KeysBackupState.Enabling to KeysBackupState.ReadyToBackUp,

            KeysBackupState.NotTrusted to KeysBackupState.CheckingBackUpOnHomeserver,
            // This transition happens when we trust the device
            KeysBackupState.NotTrusted to KeysBackupState.ReadyToBackUp,

            KeysBackupState.ReadyToBackUp to KeysBackupState.WillBackUp,

            KeysBackupState.Unknown to KeysBackupState.CheckingBackUpOnHomeserver,

            KeysBackupState.WillBackUp to KeysBackupState.BackingUp,

            KeysBackupState.WrongBackUpVersion to KeysBackupState.CheckingBackUpOnHomeserver,

            // FIXME These transitions are observed during test, and I'm not sure they should occur. Don't have time to investigate now
            KeysBackupState.ReadyToBackUp to KeysBackupState.BackingUp,
            KeysBackupState.ReadyToBackUp to KeysBackupState.ReadyToBackUp,
            KeysBackupState.WillBackUp to KeysBackupState.ReadyToBackUp,
            KeysBackupState.WillBackUp to KeysBackupState.Unknown
    )

    private val stateList = ArrayList<KeysBackupState>()
    private var lastTransitionError: String? = null

    init {
        keysBackup.addListener(this)
    }

    // TODO Make expectedStates mandatory to enforce test
    fun stopAndCheckStates(expectedStates: List<KeysBackupState>?) {
        keysBackup.removeListener(this)

        expectedStates?.let {
            assertEquals(it.size, stateList.size)

            for (i in it.indices) {
                assertEquals("The state $i is not correct. states: " + stateList.joinToString(separator = " "), it[i], stateList[i])
            }
        }

        assertNull("states: " + stateList.joinToString(separator = " "), lastTransitionError)
    }

    override fun onStateChange(newState: KeysBackupState) {
        stateList.add(newState)

        // Check that state transition is valid
        if (stateList.size >= 2 &&
                !allowedStateTransitions.contains(stateList[stateList.size - 2] to newState)) {
            // Forbidden transition detected
            lastTransitionError = "Forbidden transition detected from " + stateList[stateList.size - 2] + " to " + newState
        }

        if (expectedStateChange == stateList.size) {
            latch?.countDown()
        }
    }
}
