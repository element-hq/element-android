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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult

interface KeysBackupService {

    /**
     * Retrieve the current version of the backup from the homeserver.
     *
     * It can be different than keysBackupVersion.
     */
    suspend fun getCurrentVersion(): KeysBackupLastVersionResult?

    /**
     * Create a new keys backup version and enable it, using the information return from [prepareKeysBackupVersion].
     *
     * @param keysBackupCreationInfo the info object from [prepareKeysBackupVersion].
     * @return KeysVersion
     */
    @Throws
    suspend fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo): KeysVersion

    /**
     * Facility method to get the total number of locally stored keys.
     */
    suspend fun getTotalNumbersOfKeys(): Int

    /**
     * Facility method to get the number of backed up keys.
     */
    suspend fun getTotalNumbersOfBackedUpKeys(): Int

//    /**
//     * Start to back up keys immediately.
//     *
//     * @param progressListener the callback to follow the progress
//     * @param callback the main callback
//     */
//    fun backupAllGroupSessions(progressListener: ProgressListener?,
//                               callback: MatrixCallback<Unit>?)

    /**
     * Check trust on a key backup version.
     *
     * @param keysBackupVersion the backup version to check.
     */
    suspend fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult): KeysBackupVersionTrust

    /**
     * Return the current progress of the backup.
     */
    suspend fun getBackupProgress(progressListener: ProgressListener)

    /**
     * Get information about a backup version defined on the homeserver.
     *
     * It can be different than keysBackupVersion.
     * @param version the backup version
     */
    suspend fun getVersion(version: String): KeysVersionResult?

    /**
     * This method fetches the last backup version on the server, then compare to the currently backup version use.
     * If versions are not the same, the current backup is deleted (on server or locally), then the backup may be started again, using the last version.
     *
     * @return true if backup is already using the last version, and false if it is not the case
     */
    suspend fun forceUsingLastVersion(): Boolean

    /**
     * Check the server for an active key backup.
     *
     * If one is present and has a valid signature from one of the user's verified
     * devices, start backing up to it.
     */
    suspend fun checkAndStartKeysBackup()

    fun addListener(listener: KeysBackupStateListener)

    fun removeListener(listener: KeysBackupStateListener)

    /**
     * Set up the data required to create a new backup version.
     * The backup version will not be created and enabled until [createKeysBackupVersion]
     * is called.
     * The returned [MegolmBackupCreationInfo] object has a `recoveryKey` member with
     * the user-facing recovery key string.
     *
     * @param password an optional passphrase string that can be entered by the user
     * when restoring the backup as an alternative to entering the recovery key.
     * @param progressListener a progress listener, as generating private key from password may take a while
     */
    suspend fun prepareKeysBackupVersion(password: String?, progressListener: ProgressListener?): MegolmBackupCreationInfo

    /**
     * Delete a keys backup version. It will delete all backed up keys on the server, and the backup itself.
     * If we are backing up to this version. Backup will be stopped.
     *
     * @param version the backup version to delete.
     */
    @Throws
    suspend fun deleteBackup(version: String)

    /**
     * Ask if the backup on the server contains keys that we may do not have locally.
     * This should be called when entering in the state READY_TO_BACKUP
     */
    suspend fun canRestoreKeys(): Boolean

    /**
     * Set trust on a keys backup version.
     * It adds (or removes) the signature of the current device to the authentication part of the keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param trust the trust to set to the keys backup.
     */
    @Throws
    suspend fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult, trust: Boolean)

    /**
     * Set trust on a keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param recoveryKey the recovery key to challenge with the key backup public key.
     */
    suspend fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult, recoveryKey: IBackupRecoveryKey)

    /**
     * Set trust on a keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param password the pass phrase to challenge with the keyBackupVersion public key.
     */
    suspend fun trustKeysBackupVersionWithPassphrase(
            keysBackupVersion: KeysVersionResult,
            password: String
    )

    suspend fun onSecretKeyGossip(secret: String)

    /**
     * Restore a backup with a recovery key from a given backup version stored on the homeserver.
     *
     * @param keysVersionResult the backup version to restore from.
     * @param recoveryKey the recovery key to decrypt the retrieved backup.
     * @param roomId the id of the room to get backup data from.
     * @param sessionId the id of the session to restore.
     * @param stepProgressListener the step progress listener
     */
    suspend fun restoreKeysWithRecoveryKey(
            keysVersionResult: KeysVersionResult,
            recoveryKey: IBackupRecoveryKey,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?
    ): ImportRoomKeysResult

    /**
     * Restore a backup with a password from a given backup version stored on the homeserver.
     *
     * @param keysBackupVersion the backup version to restore from.
     * @param password the password to decrypt the retrieved backup.
     * @param roomId the id of the room to get backup data from.
     * @param sessionId the id of the session to restore.
     * @param stepProgressListener the step progress listener
     */
    suspend fun restoreKeyBackupWithPassword(
            keysBackupVersion: KeysVersionResult,
            password: String,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?
    ): ImportRoomKeysResult

    val keysBackupVersion: KeysVersionResult?

    val currentBackupVersion: String?
        get() = keysBackupVersion?.version

    fun isEnabled(): Boolean
    fun isStuck(): Boolean
    fun getState(): KeysBackupState

    // For gossiping
    fun saveBackupRecoveryKey(recoveryKey: IBackupRecoveryKey?, version: String?)
    suspend fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo?

    suspend fun isValidRecoveryKeyForCurrentVersion(recoveryKey: IBackupRecoveryKey): Boolean

    fun computePrivateKey(
            passphrase: String,
            privateKeySalt: String,
            privateKeyIterations: Int,
            progressListener: ProgressListener
    ): ByteArray
}
