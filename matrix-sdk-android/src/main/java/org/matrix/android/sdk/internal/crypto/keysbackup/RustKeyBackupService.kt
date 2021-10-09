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
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.RequestSender
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.SignalableMegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.store.SavedKeyBackupKeyInfo
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import uniffi.olm.BackupRecoveryKey
import javax.inject.Inject

/**
 * A DefaultKeysBackupService class instance manage incremental backup of e2e keys (megolm keys)
 * to the user's homeserver.
 */
@SessionScope
internal class RustKeyBackupService @Inject constructor(
        private val olmMachine: OlmMachine,
        private val sender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
) : KeysBackupService {

    private val uiHandler = Handler(Looper.getMainLooper())

    private val keysBackupStateManager = KeysBackupStateManager(uiHandler)

    // The backup version
    override var keysBackupVersion: KeysVersionResult? = null
        private set

    private var backupAllGroupSessionsCallback: MatrixCallback<Unit>? = null

    private var keysBackupStateListener: KeysBackupStateListener? = null

    override val isEnabled: Boolean
        get() = keysBackupStateManager.isEnabled

    override val isStucked: Boolean
        get() = keysBackupStateManager.isStucked

    override val state: KeysBackupState
        get() = keysBackupStateManager.state

    override val currentBackupVersion: String?
        get() = keysBackupVersion?.version

    override fun addListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.addListener(listener)
    }

    override fun removeListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.removeListener(listener)
    }

    override fun prepareKeysBackupVersion(password: String?,
                                          progressListener: ProgressListener?,
                                          callback: MatrixCallback<MegolmBackupCreationInfo>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                withContext(coroutineDispatchers.crypto) {
                    val key = if (password != null) {
                        BackupRecoveryKey.fromPassphrase(password)
                    } else {
                        BackupRecoveryKey()
                    }

                    val publicKey = key.publicKey()
                    val backupAuthData = SignalableMegolmBackupAuthData(
                            publicKey = publicKey.publicKey,
                            privateKeySalt = publicKey.passphraseInfo?.privateKeySalt,
                            privateKeyIterations = publicKey.passphraseInfo?.privateKeyIterations
                    )
                    val canonicalJson = JsonCanonicalizer.getCanonicalJson(
                            Map::class.java,
                            backupAuthData.signalableJSONDictionary()
                    )

                    val signedMegolmBackupAuthData = MegolmBackupAuthData(
                            publicKey = backupAuthData.publicKey,
                            privateKeySalt = backupAuthData.privateKeySalt,
                            privateKeyIterations = backupAuthData.privateKeyIterations,
                            signatures = olmMachine.sign(canonicalJson)
                    )

                    MegolmBackupCreationInfo(
                            algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP,
                            authData = signedMegolmBackupAuthData,
                            recoveryKey = key.toBase58()
                    )
                }
            }.foldToCallback(callback)
        }
    }

    override fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo,
                                         callback: MatrixCallback<KeysVersion>) {
        @Suppress("UNCHECKED_CAST")
        val createKeysBackupVersionBody = CreateKeysBackupVersionBody(
                algorithm = keysBackupCreationInfo.algorithm,
                authData = keysBackupCreationInfo.authData.toJsonDict()
        )

        keysBackupStateManager.state = KeysBackupState.Enabling

        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            try {
                val data = sender.createKeyBackup(createKeysBackupVersionBody)
                // Reset backup markers.
                // Don't we need to join the task here? Isn't this a race condition?
                cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                    // TODO reset our backup state here, i.e. the `backed_up` flag on inbound group sessions
                }

                olmMachine.enableBackup(keysBackupCreationInfo.authData.publicKey, data.version)

                callback.onSuccess(data)
            } catch (failure: Throwable) {
                keysBackupStateManager.state = KeysBackupState.Disabled
                callback.onFailure(failure)
            }
        }
    }

    override fun saveBackupRecoveryKey(recoveryKey: String?, version: String?) {
        cryptoCoroutineScope.launch {
            olmMachine.saveRecoveryKey(recoveryKey, version)
        }
    }

    private fun resetBackupAllGroupSessionsListeners() {
        backupAllGroupSessionsCallback = null

        keysBackupStateListener?.let {
            keysBackupStateManager.removeListener(it)
        }

        keysBackupStateListener = null
    }

    /**
     * Reset all local key backup data.
     *
     * Note: This method does not update the state
     */
    private fun resetKeysBackupData() {
        resetBackupAllGroupSessionsListeners()

        /*

        TODO reset data on the rust side
        cryptoStore.setKeyBackupVersion(null)
        cryptoStore.setKeysBackupData(null)
        backupOlmPkEncryption?.releaseEncryption()
        backupOlmPkEncryption = null

        // Reset backup markers
        cryptoStore.resetBackupMarkers()
        */
    }

    override fun deleteBackup(version: String, callback: MatrixCallback<Unit>?) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                if (keysBackupVersion != null && version == keysBackupVersion?.version) {
                    resetKeysBackupData()
                    keysBackupVersion = null
                    keysBackupStateManager.state = KeysBackupState.Unknown
                }

                fun eventuallyRestartBackup() {
                    // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                    if (state == KeysBackupState.Unknown) {
                        checkAndStartKeysBackup()
                    }
                }

                try {
                    sender.deleteKeyBackup(version)
                    eventuallyRestartBackup()
                    uiHandler.post { callback?.onSuccess(Unit) }
                } catch (failure: Throwable) {
                    eventuallyRestartBackup()
                    uiHandler.post { callback?.onFailure(failure) }
                }
            }
        }
    }

    override fun canRestoreKeys(): Boolean {
        // TODO
        return false
    }

    override fun getTotalNumbersOfKeys(): Int {
        return olmMachine.roomKeyCounts().total.toInt()
    }

    override fun getTotalNumbersOfBackedUpKeys(): Int {
        return olmMachine.roomKeyCounts().backedUp.toInt()
    }

    override fun backupAllGroupSessions(progressListener: ProgressListener?,
                                        callback: MatrixCallback<Unit>?) {
        TODO()
    }

    override fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult,
                                    callback: MatrixCallback<KeysBackupVersionTrust>) {
        Timber.d("BACKUP: HELLOO TRYING TO CHECK THE TRUST")
        // TODO
        callback.onSuccess(KeysBackupVersionTrust(false))
    }

    override fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult,
                                        trust: Boolean,
                                        callback: MatrixCallback<Unit>) {
        Timber.v("trustKeyBackupVersion: $trust, version ${keysBackupVersion.version}")
        TODO()
    }

    override fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult,
                                                       recoveryKey: String,
                                                       callback: MatrixCallback<Unit>) {
        Timber.v("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")
        TODO()
    }

    override fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult,
                                                      password: String,
                                                      callback: MatrixCallback<Unit>) {
        TODO()
    }

    override fun onSecretKeyGossip(secret: String) {
        Timber.i("## CrossSigning - onSecretKeyGossip")
        TODO()
    }

    override fun getBackupProgress(progressListener: ProgressListener) {
        val backedUpKeys = getTotalNumbersOfBackedUpKeys()
        val total = getTotalNumbersOfKeys()

        progressListener.onProgress(backedUpKeys, total)
    }

    override fun restoreKeysWithRecoveryKey(keysVersionResult: KeysVersionResult,
                                            recoveryKey: String,
                                            roomId: String?,
                                            sessionId: String?,
                                            stepProgressListener: StepProgressListener?,
                                            callback: MatrixCallback<ImportRoomKeysResult>) {
        // TODO
        Timber.v("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version}")
    }

    override fun restoreKeyBackupWithPassword(keysBackupVersion: KeysVersionResult,
                                              password: String,
                                              roomId: String?,
                                              sessionId: String?,
                                              stepProgressListener: StepProgressListener?,
                                              callback: MatrixCallback<ImportRoomKeysResult>) {
        // TODO
        Timber.v("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")
    }

    override fun getVersion(version: String, callback: MatrixCallback<KeysVersionResult?>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                sender.getKeyBackupVersion(version)
            }.foldToCallback(callback)
        }
    }

    override fun getCurrentVersion(callback: MatrixCallback<KeysVersionResult?>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                sender.getKeyBackupVersion()
            }.foldToCallback(callback)
        }
    }

    private suspend fun forceUsingLastVersionHelper(): Boolean {
        val response = sender.getKeyBackupVersion()
        val serverBackupVersion = response?.version
        val localBackupVersion = keysBackupVersion?.version

        Timber.d("BACKUP: $serverBackupVersion")

        return if (serverBackupVersion == null) {
            if (localBackupVersion == null) {
                // No backup on the server, and backup is not active
                true
            } else {
                // No backup on the server, and we are currently backing up, so stop backing up
                resetKeysBackupData()
                keysBackupVersion = null
                keysBackupStateManager.state = KeysBackupState.Disabled
                false
            }
        } else {
            if (localBackupVersion == null) {
                // Do a check
                checkAndStartWithKeysBackupVersion(response)
                // backup on the server, and backup is not active
                false
            } else {
                // Backup on the server, and we are currently backing up, compare version
                if (localBackupVersion == serverBackupVersion) {
                    // We are already using the last version of the backup
                    true
                } else {
                    // This will automatically check for the last version then
                    deleteBackup(localBackupVersion, null)
                    // We are not using the last version, so delete the current version we are using on the server
                    false
                }
            }
        }
    }

    override fun forceUsingLastVersion(callback: MatrixCallback<Boolean>) {
        cryptoCoroutineScope.launch {
            runCatching {
                forceUsingLastVersionHelper()
            }.foldToCallback(callback)
        }
    }

    override fun checkAndStartKeysBackup() {
        if (!isStucked) {
            // Try to start or restart the backup only if it is in unknown or bad state
            Timber.w("checkAndStartKeysBackup: invalid state: $state")

            return
        }

        keysBackupVersion = null
        keysBackupStateManager.state = KeysBackupState.CheckingBackUpOnHomeserver

        getCurrentVersion(object : MatrixCallback<KeysVersionResult?> {
            override fun onSuccess(data: KeysVersionResult?) {
                checkAndStartWithKeysBackupVersion(data)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "checkAndStartKeysBackup: Failed to get current version")
                keysBackupStateManager.state = KeysBackupState.Unknown
            }
        })
    }

    private fun checkAndStartWithKeysBackupVersion(keyBackupVersion: KeysVersionResult?) {
        Timber.v("checkAndStartWithKeyBackupVersion: ${keyBackupVersion?.version}")

        keysBackupVersion = keyBackupVersion

        if (keyBackupVersion == null) {
            Timber.v("checkAndStartWithKeysBackupVersion: Found no key backup version on the homeserver")
            resetKeysBackupData()
            keysBackupStateManager.state = KeysBackupState.Disabled
        } else {
            getKeysBackupTrust(keyBackupVersion, object : MatrixCallback<KeysBackupVersionTrust> {
                override fun onSuccess(data: KeysBackupVersionTrust) {
                    val versionInStore = getKeyBackupRecoveryKeyInfo()?.version

                    if (data.usable) {
                        Timber.v("checkAndStartWithKeysBackupVersion: Found usable key backup. version: ${keyBackupVersion.version}")
                        // Check the version we used at the previous app run
                        if (versionInStore != null && versionInStore != keyBackupVersion.version) {
                            Timber.v(" -> clean the previously used version $versionInStore")
                            resetKeysBackupData()
                        }

                        Timber.v("   -> enabling key backups")
                        // TODO
                        // enableKeysBackup(keyBackupVersion)
                    } else {
                        Timber.v("checkAndStartWithKeysBackupVersion: No usable key backup. version: ${keyBackupVersion.version}")
                        if (versionInStore != null) {
                            Timber.v("   -> disabling key backup")
                            resetKeysBackupData()
                        }

                        keysBackupStateManager.state = KeysBackupState.NotTrusted
                    }
                }

                override fun onFailure(failure: Throwable) {
                    // Cannot happen
                }
            })
        }
    }

    override fun isValidRecoveryKeyForCurrentVersion(recoveryKey: String, callback: MatrixCallback<Boolean>) {
        val keysBackupVersion = keysBackupVersion ?: return Unit.also { callback.onSuccess(false) }

        try {
            val key = BackupRecoveryKey.fromBase64(recoveryKey)
            val publicKey = key.publicKey().publicKey
            val authData = getMegolmBackupAuthData(keysBackupVersion) ?: return Unit.also { callback.onSuccess(false) }

            callback.onSuccess(authData.publicKey == publicKey)
        } catch (error: Throwable) {
            callback.onFailure(error)
        }
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        val info = olmMachine.getBackupKeys() ?: return null
        return SavedKeyBackupKeyInfo(info.recoveryKey, info.backupVersion)
    }

    /**
     * Extract MegolmBackupAuthData data from a backup version.
     *
     * @param keysBackupData the key backup data
     *
     * @return the authentication if found and valid, null in other case
     */
    private fun getMegolmBackupAuthData(keysBackupData: KeysVersionResult): MegolmBackupAuthData? {
        return keysBackupData
                .takeIf { it.version.isNotEmpty() && it.algorithm == MXCRYPTO_ALGORITHM_MEGOLM_BACKUP }
                ?.getAuthDataAsMegolmBackupAuthData()
                ?.takeIf { it.publicKey.isNotEmpty() }
    }
}
