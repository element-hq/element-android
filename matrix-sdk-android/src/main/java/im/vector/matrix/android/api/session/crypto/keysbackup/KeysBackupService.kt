/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto.keysbackup

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.listeners.StepProgressListener
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.KeysVersion
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.KeysVersionResult

// TODO Add doc from implementation
interface KeysBackupService {
    fun getCurrentVersion(callback: MatrixCallback<KeysVersionResult?>)
    fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo, callback: MatrixCallback<KeysVersion>)
    fun getTotalNumbersOfKeys(): Int
    fun getTotalNumbersOfBackedUpKeys(): Int
    fun backupAllGroupSessions(progressListener: ProgressListener?, callback: MatrixCallback<Unit>?)
    fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult, callback: MatrixCallback<KeysBackupVersionTrust>)
    fun getBackupProgress(progressListener: ProgressListener)
    fun getVersion(version: String, callback: MatrixCallback<KeysVersionResult?>)
    fun forceUsingLastVersion(callback: MatrixCallback<Boolean>)
    fun checkAndStartKeysBackup()
    fun addListener(listener: KeysBackupStateListener)
    fun removeListener(listener: KeysBackupStateListener)

    fun prepareKeysBackupVersion(password: String?, progressListener: ProgressListener?, callback: MatrixCallback<MegolmBackupCreationInfo>)
    fun deleteBackup(version: String, callback: MatrixCallback<Unit>?)
    fun canRestoreKeys(): Boolean
    fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult, trust: Boolean, callback: MatrixCallback<Unit>)
    fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult, recoveryKey: String, callback: MatrixCallback<Unit>)
    fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult, password: String, callback: MatrixCallback<Unit>)
    fun restoreKeysWithRecoveryKey(keysVersionResult: KeysVersionResult, recoveryKey: String, roomId: String?, sessionId: String?, stepProgressListener: StepProgressListener?, callback: MatrixCallback<ImportRoomKeysResult>)
    fun restoreKeyBackupWithPassword(keysBackupVersion: KeysVersionResult, password: String, roomId: String?, sessionId: String?, stepProgressListener: StepProgressListener?, callback: MatrixCallback<ImportRoomKeysResult>)

    val keysBackupVersion: KeysVersionResult?
    val currentBackupVersion: String?
    val isEnabled: Boolean
    val isStucked: Boolean
    val state: KeysBackupState

    interface KeysBackupStateListener {
        fun onStateChange(newState: KeysBackupState)
    }

}