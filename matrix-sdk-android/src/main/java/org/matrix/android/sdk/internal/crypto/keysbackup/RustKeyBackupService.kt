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
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.RequestSender
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.SignalableMegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeyBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.UpdateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.store.SavedKeyBackupKeyInfo
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.olm.OlmException
import timber.log.Timber
import uniffi.olm.BackupRecoveryKey
import uniffi.olm.Request
import uniffi.olm.RequestType
import java.security.InvalidParameterException
import javax.inject.Inject
import kotlin.random.Random

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
    companion object {
        // Maximum delay in ms in {@link maybeBackupKeys}
        private const val KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS = 10_000L
    }

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
                        BackupRecoveryKey.newFromPassphrase(password)
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
                    olmMachine.disableBackup()
                }

                val keyBackupVersion = KeysVersionResult(
                        algorithm = createKeysBackupVersionBody.algorithm,
                        authData = createKeysBackupVersionBody.authData,
                        version = data.version,
                        // We can assume that the server does not have keys yet
                        count = 0,
                        hash = ""
                )

                enableKeysBackup(keyBackupVersion)

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
        olmMachine.disableBackup()
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
        val keyCountOnServer = keysBackupVersion?.count ?: return false
        val keyCountLocally = getTotalNumbersOfKeys()

        // TODO is this sensible? We may have the same number of keys, or even more keys locally
        //  but the set of keys doesn't necessarily overlap
        return keyCountLocally < keyCountOnServer
    }

    override fun getTotalNumbersOfKeys(): Int {
        return olmMachine.roomKeyCounts().total.toInt()
    }

    override fun getTotalNumbersOfBackedUpKeys(): Int {
        return olmMachine.roomKeyCounts().backedUp.toInt()
    }

    override fun backupAllGroupSessions(progressListener: ProgressListener?,
                                        callback: MatrixCallback<Unit>?) {
        // This is only used in tests?
        TODO()
    }

    private suspend fun checkBackupTrust(authData: MegolmBackupAuthData?): KeysBackupVersionTrust {
        return if (authData == null || authData.publicKey.isEmpty() || authData.signatures.isEmpty()) {
            Timber.v("getKeysBackupTrust: Key backup is absent or missing required data")
            KeysBackupVersionTrust()
        } else {
            KeysBackupVersionTrust(olmMachine.checkAuthDataSignature(authData))
        }
    }

    override fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult,
                                    callback: MatrixCallback<KeysBackupVersionTrust>) {
        val authData = keysBackupVersion.getAuthDataAsMegolmBackupAuthData()

        cryptoCoroutineScope.launch {
            try {
                callback.onSuccess(checkBackupTrust(authData))
            } catch (exception: Throwable) {
                callback.onFailure(exception)
            }
        }
    }

    override fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult,
                                        trust: Boolean,
                                        callback: MatrixCallback<Unit>) {
        Timber.v("trustKeyBackupVersion: $trust, version ${keysBackupVersion.version}")

        // Get auth data to update it
        val authData = getMegolmBackupAuthData(keysBackupVersion)

        if (authData == null) {
            Timber.w("trustKeyBackupVersion:trust: Key backup is missing required data")

            callback.onFailure(IllegalArgumentException("Missing element"))
        } else {
            cryptoCoroutineScope.launch(coroutineDispatchers.main) {
                val body = withContext(coroutineDispatchers.crypto) {
                    // Get current signatures, or create an empty set
                    val userId = olmMachine.userId()
                    val signatures = authData.signatures[userId].orEmpty().toMutableMap()

                    if (trust) {
                        // Add current device signature
                        val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, authData.signalableJSONDictionary())
                        val deviceSignature = olmMachine.sign(canonicalJson)

                        deviceSignature[userId]?.forEach { entry ->
                            signatures[entry.key] = entry.value
                        }
                    } else {
                        signatures.remove("ed25519:${olmMachine.deviceId()}")
                    }

                    val newAuthData = authData.copy()
                    val newSignatures = newAuthData.signatures.toMutableMap()
                    newSignatures[userId] = signatures

                    @Suppress("UNCHECKED_CAST")
                    UpdateKeysBackupVersionBody(
                            algorithm = keysBackupVersion.algorithm,
                            authData = newAuthData.copy(signatures = newSignatures).toJsonDict(),
                            version = keysBackupVersion.version)
                }
                try {
                    sender.updateBackup(keysBackupVersion, body)

                    val newKeysBackupVersion = KeysVersionResult(
                            algorithm = keysBackupVersion.algorithm,
                            authData = body.authData,
                            version = keysBackupVersion.version,
                            hash = keysBackupVersion.hash,
                            count = keysBackupVersion.count
                    )

                    checkAndStartWithKeysBackupVersion(newKeysBackupVersion)
                    callback.onSuccess(Unit)
                } catch (exception: Throwable) {
                    callback.onFailure(exception)
                }
            }
        }
    }

    // Check that the recovery key matches to the public key that we downloaded from the server.
    // If they match, we can trust the public key and enable backups since we have the private key.
    private fun checkRecoveryKey(recoveryKey: BackupRecoveryKey, keysBackupData: KeysVersionResult) {
        val backupKey = recoveryKey.publicKey()
        val authData = getMegolmBackupAuthData(keysBackupData)

        when {
            authData == null                          -> {
                Timber.w("isValidRecoveryKeyForKeysBackupVersion: Key backup is missing required data")
                throw IllegalArgumentException("Missing element")
            }
            backupKey.publicKey != authData.publicKey -> {
                Timber.w("isValidRecoveryKeyForKeysBackupVersion: Public keys mismatch")
                throw IllegalArgumentException("Invalid recovery key or password")
            }
            else                                      -> {
                // This case is fine, the public key on the server matches the public key the
                // recovery key produced.
            }
        }
    }

    override fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult,
                                                       recoveryKey: String,
                                                       callback: MatrixCallback<Unit>) {
        Timber.v("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")

        cryptoCoroutineScope.launch {
            try {
                // This is ~nowhere mentioned, the string here is actually a base58 encoded key.
                // This not really supported by the spec for the backup key, the 4S key supports
                // base58 encoding and the same method seems to be used here.
                val key = BackupRecoveryKey.fromBase58(recoveryKey)
                checkRecoveryKey(key, keysBackupVersion)
                trustKeysBackupVersion(keysBackupVersion, true, callback)
            } catch (exception: Throwable) {
                callback.onFailure(exception)
            }
        }
    }

    override fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult,
                                                      password: String,
                                                      callback: MatrixCallback<Unit>) {
        cryptoCoroutineScope.launch {
            try {
                val key = recoveryKeyFromPassword(password, keysBackupVersion)

                if (key == null) {
                    Timber.w("trustKeysBackupVersionWithPassphrase: Key backup is missing required data")
                    callback.onFailure(IllegalArgumentException("Missing element"))
                } else {
                    checkRecoveryKey(key, keysBackupVersion)
                    trustKeysBackupVersion(keysBackupVersion, true, callback)
                }
            } catch (exception: Throwable) {
                callback.onFailure(exception)
            }
        }
    }

    override fun onSecretKeyGossip(secret: String) {
        Timber.i("## CrossSigning - onSecretKeyGossip")
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            try {
                val version = sender.getKeyBackupVersion()

                if (version != null) {
                    val key = BackupRecoveryKey.fromBase64(secret)

                    awaitCallback<Unit> {
                        trustKeysBackupVersion(version, true, it)
                    }
                    val importResult = awaitCallback<ImportRoomKeysResult> {
                        cryptoCoroutineScope.launch {
                            restoreBackup(version, key, null, null, null)
                        }
                    }
                    Timber.i("onSecretKeyGossip: Recovered keys ${importResult.successfullyNumberOfImportedKeys} out of ${importResult.totalNumberOfKeys}")

                    saveBackupRecoveryKey(secret, version.version)
                } else {
                    Timber.e("onSecretKeyGossip: Failed to import backup recovery key, no backup version was found on the server")
                }
            } catch (failure: Throwable) {
                Timber.e("onSecretKeyGossip: failed to trust key backup version ${keysBackupVersion?.version}: $failure")
            }
        }
    }

    override fun getBackupProgress(progressListener: ProgressListener) {
        val backedUpKeys = getTotalNumbersOfBackedUpKeys()
        val total = getTotalNumbersOfKeys()

        progressListener.onProgress(backedUpKeys, total)
    }

    /**
     * Same method as [RoomKeysRestClient.getRoomKey] except that it accepts nullable
     * parameters and always returns a KeysBackupData object through the Callback
     */
    private suspend fun getKeys(sessionId: String?, roomId: String?, version: String): KeysBackupData {
        return when {
            roomId != null && sessionId != null -> {
                sender.downloadBackedUpKeys(version, roomId, sessionId)
            }
            roomId != null                      -> {
                sender.downloadBackedUpKeys(version, roomId)
            }
            else                                -> {
                sender.downloadBackedUpKeys(version)
            }
        }
    }

    @VisibleForTesting
    @WorkerThread
    fun decryptKeyBackupData(keyBackupData: KeyBackupData, sessionId: String, roomId: String, key: BackupRecoveryKey): MegolmSessionData? {
        var sessionBackupData: MegolmSessionData? = null

        val jsonObject = keyBackupData.sessionData

        val ciphertext = jsonObject["ciphertext"]?.toString()
        val mac = jsonObject["mac"]?.toString()
        val ephemeralKey = jsonObject["ephemeral"]?.toString()

        if (ciphertext != null && mac != null && ephemeralKey != null) {
            try {
                val decrypted = key.decrypt(ephemeralKey, mac, ciphertext)

                val moshi = MoshiProvider.providesMoshi()
                val adapter = moshi.adapter(MegolmSessionData::class.java)

                sessionBackupData = adapter.fromJson(decrypted)
            } catch (e: Throwable) {
                Timber.e(e, "OlmException")
            }

            if (sessionBackupData != null) {
                sessionBackupData = sessionBackupData.copy(
                        sessionId = sessionId,
                        roomId = roomId
                )
            }
        }

        return sessionBackupData
    }

    private suspend fun restoreBackup(
            keysVersionResult: KeysVersionResult,
            recoveryKey: BackupRecoveryKey,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?,
    ): ImportRoomKeysResult {
        withContext(coroutineDispatchers.crypto) {
            // Check if the recovery is valid before going any further
            if (!isValidRecoveryKey(recoveryKey, keysVersionResult)) {
                Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key for this keys version")
                throw InvalidParameterException("Invalid recovery key")
            }
        }

        stepProgressListener?.onStepProgress(StepProgressListener.Step.DownloadingKey)

        // Get backed up keys from the homeserver
        val data = getKeys(sessionId, roomId, keysVersionResult.version)

        return withContext(coroutineDispatchers.computation) {
            val sessionsData = ArrayList<MegolmSessionData>()
            // Restore that data
            var sessionsFromHsCount = 0
            for ((roomIdLoop, backupData) in data.roomIdToRoomKeysBackupData) {
                for ((sessionIdLoop, keyBackupData) in backupData.sessionIdToKeyBackupData) {
                    sessionsFromHsCount++

                    val sessionData = decryptKeyBackupData(keyBackupData, sessionIdLoop, roomIdLoop, recoveryKey)

                    sessionData?.let {
                        sessionsData.add(it)
                    }
                }
            }
            Timber.v("restoreKeysWithRecoveryKey: Decrypted ${sessionsData.size} keys out" +
                    " of $sessionsFromHsCount from the backup store on the homeserver")

            // Do not trigger a backup for them if they come from the backup version we are using
            val backUp = keysVersionResult.version != keysBackupVersion?.version
            if (backUp) {
                Timber.v("restoreKeysWithRecoveryKey: Those keys will be backed up" +
                        " to backup version: ${keysBackupVersion?.version}")
            }

            // Import them into the crypto store
            val progressListener = if (stepProgressListener != null) {
                object : ProgressListener {
                    override fun onProgress(progress: Int, total: Int) {
                        // Note: no need to post to UI thread, importMegolmSessionsData() will do it
                        stepProgressListener.onStepProgress(StepProgressListener.Step.ImportingKey(progress, total))
                    }
                }
            } else {
                null
            }

            val result = olmMachine.importDecryptedKeys(sessionsData, progressListener)

            // Do not back up the key if it comes from a backup recovery
            if (backUp) {
                maybeBackupKeys()
            }

            // Save for next time and for gossiping
            saveBackupRecoveryKey(recoveryKey.toBase64(), keysVersionResult.version)
            result
        }
    }

    override fun restoreKeysWithRecoveryKey(keysVersionResult: KeysVersionResult,
                                            recoveryKey: String,
                                            roomId: String?,
                                            sessionId: String?,
                                            stepProgressListener: StepProgressListener?,
                                            callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.v("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version}")
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                val key = BackupRecoveryKey.fromBase58(recoveryKey)
                restoreBackup(keysVersionResult, key, roomId, sessionId, stepProgressListener)
            }.foldToCallback(callback)
        }
    }

    override fun restoreKeyBackupWithPassword(keysBackupVersion: KeysVersionResult,
                                              password: String,
                                              roomId: String?,
                                              sessionId: String?,
                                              stepProgressListener: StepProgressListener?,
                                              callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.v("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                val recoveryKey = withContext(coroutineDispatchers.crypto) {
                    val key = recoveryKeyFromPassword(password, keysBackupVersion)

                    if (key == null) {
                        Timber.w("trustKeysBackupVersionWithPassphrase: Key backup is missing required data")

                        throw IllegalArgumentException("Missing element")
                    } else {
                        key
                    }
                }

                restoreBackup(keysBackupVersion, recoveryKey, roomId, sessionId, stepProgressListener)
            }.foldToCallback(callback)
        }
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
                        cryptoCoroutineScope.launch {
                            enableKeysBackup(keyBackupVersion)
                        }
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

    private fun isValidRecoveryKey(recoveryKey: BackupRecoveryKey, version: KeysVersionResult): Boolean {
        val publicKey = recoveryKey.publicKey().publicKey
        val authData = getMegolmBackupAuthData(version) ?: return false
        return authData.publicKey == publicKey
    }

    override fun isValidRecoveryKeyForCurrentVersion(recoveryKey: String, callback: MatrixCallback<Boolean>) {
        val keysBackupVersion = keysBackupVersion ?: return Unit.also { callback.onSuccess(false) }

        try {
            val key = BackupRecoveryKey.fromBase64(recoveryKey)
            callback.onSuccess(isValidRecoveryKey(key, keysBackupVersion))
        } catch (error: Throwable) {
            callback.onFailure(error)
        }
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        val info = olmMachine.getBackupKeys() ?: return null
        return SavedKeyBackupKeyInfo(info.recoveryKey, info.backupVersion)
    }

    /**
     * Compute the recovery key from a password and key backup version.
     *
     * @param password the password.
     * @param keysBackupData the backup and its auth data.
     *
     * @return the recovery key if successful, null in other cases
     */
    @WorkerThread
    private fun recoveryKeyFromPassword(password: String, keysBackupData: KeysVersionResult): BackupRecoveryKey? {
        val authData = getMegolmBackupAuthData(keysBackupData)

        if (authData == null) {
            Timber.w("recoveryKeyFromPassword: invalid parameter")
            return null
        }

        if (authData.privateKeySalt.isNullOrBlank()
                || authData.privateKeyIterations == null) {
            Timber.w("recoveryKeyFromPassword: Salt and/or iterations not found in key backup auth data")

            return null
        }

        return BackupRecoveryKey.fromPassphrase(password, authData.privateKeySalt, authData.privateKeyIterations)
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

    /**
     * Enable backing up of keys.
     * This method will update the state and will start sending keys in nominal case
     *
     * @param keysVersionResult backup information object as returned by [getCurrentVersion].
     */
    private suspend fun enableKeysBackup(keysVersionResult: KeysVersionResult) {
        val retrievedMegolmBackupAuthData = keysVersionResult.getAuthDataAsMegolmBackupAuthData()

        if (retrievedMegolmBackupAuthData != null) {
            try {
                olmMachine.enableBackup(retrievedMegolmBackupAuthData.publicKey, keysVersionResult.version)
                keysBackupVersion = keysVersionResult
            } catch (e: OlmException) {
                Timber.e(e, "OlmException")
                keysBackupStateManager.state = KeysBackupState.Disabled
                return
            }

            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
            maybeBackupKeys()
        } else {
            Timber.e("Invalid authentication data")
            keysBackupStateManager.state = KeysBackupState.Disabled
        }
    }

    /**
     * Do a backup if there are new keys, with a delay
     */
    fun maybeBackupKeys() {
        when {
            isStucked                              -> {
                // If not already done, or in error case, check for a valid backup version on the homeserver.
                // If there is one, maybeBackupKeys will be called again.
                checkAndStartKeysBackup()
            }
            state == KeysBackupState.ReadyToBackUp -> {
                keysBackupStateManager.state = KeysBackupState.WillBackUp

                // Wait between 0 and 10 seconds, to avoid backup requests from
                // different clients hitting the server all at the same time when a
                // new key is sent
                val delayInMs = Random.nextLong(KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS)

                cryptoCoroutineScope.launch {
                    delay(delayInMs)
                    // TODO is this correct? we used to call uiHandler.post() instead of this
                    withContext(Dispatchers.Main) {
                        backupKeys()
                    }
                }
            }
            else                                   -> {
                Timber.v("maybeBackupKeys: Skip it because state: $state")
            }
        }
    }

    /**
     * Send a chunk of keys to backup
     */
    @UiThread
    private suspend fun backupKeys(forceRecheck: Boolean = false) {
        Timber.v("backupKeys")

        // Sanity check, as this method can be called after a delay, the state may have change during the delay
        if (!isEnabled || !olmMachine.backupEnabled() || keysBackupVersion == null) {
            Timber.v("backupKeys: Invalid configuration $isEnabled ${olmMachine.backupEnabled()} $keysBackupVersion")
            backupAllGroupSessionsCallback?.onFailure(IllegalStateException("Invalid configuration"))
            resetBackupAllGroupSessionsListeners()

            return
        }

        if (state === KeysBackupState.BackingUp && !forceRecheck) {
            // Do nothing if we are already backing up
            Timber.v("backupKeys: Invalid state: $state")
            return
        }

        Timber.d("BACKUP: CREATING REQUEST")

        val request = olmMachine.backupRoomKeys()

        Timber.d("BACKUP: GOT REQUEST $request")

        if (request == null) {
            // Backup is up to date
            // Note: Changing state will trigger the call to backupAllGroupSessionsCallback.onSuccess()
            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp

            backupAllGroupSessionsCallback?.onSuccess(Unit)
            resetBackupAllGroupSessionsListeners()
        } else {
            try {
                if (request is Request.KeysBackup) {
                    keysBackupStateManager.state = KeysBackupState.BackingUp

                    Timber.d("BACKUP SENDING REQUEST")
                    val response = sender.backupRoomKeys(request)
                    Timber.d("BACKUP GOT RESPONSE $response")
                    olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_BACKUP, response)
                    Timber.d("BACKUP MARKED REQUEST AS SENT")

                    // TODO, again is this correct?
                    withContext(Dispatchers.Main) {
                        backupKeys(true)
                    }
                } else {
                    // Can't happen, do we want to panic?
                }
            } catch (failure: Throwable) {
                if (failure is Failure.ServerError) {
                    withContext(Dispatchers.Main) {
                        Timber.e(failure, "backupKeys: backupKeys failed.")

                        when (failure.error.code) {
                            MatrixError.M_NOT_FOUND,
                            MatrixError.M_WRONG_ROOM_KEYS_VERSION -> {
                                // Backup has been deleted on the server, or we are not using
                                // the last backup version
                                keysBackupStateManager.state = KeysBackupState.WrongBackUpVersion
                                backupAllGroupSessionsCallback?.onFailure(failure)
                                resetBackupAllGroupSessionsListeners()
                                resetKeysBackupData()
                                keysBackupVersion = null

                                // Do not stay in KeysBackupState.WrongBackUpVersion but check what
                                // is available on the homeserver
                                checkAndStartKeysBackup()
                            }
                            else                                  ->
                                // Come back to the ready state so that we will retry on the next received key
                                keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        backupAllGroupSessionsCallback?.onFailure(failure)
                        resetBackupAllGroupSessionsListeners()

                        Timber.e("backupKeys: backupKeys failed: $failure")

                        // Retry a bit later
                        keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                        maybeBackupKeys()
                    }
                }
            }
        }
    }
}
