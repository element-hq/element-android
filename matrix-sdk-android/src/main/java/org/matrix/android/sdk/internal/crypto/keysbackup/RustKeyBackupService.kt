/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.IBackupRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupLastVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrustSignature
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.SavedKeyBackupKeyInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.MegolmSessionImportManager
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.PerSessionBackupQueryRateLimiter
import org.matrix.android.sdk.internal.crypto.keysbackup.model.SignalableMegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeyBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysAlgorithmAndData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.UpdateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmException
import org.matrix.rustcomponents.sdk.crypto.Request
import org.matrix.rustcomponents.sdk.crypto.RequestType
import org.matrix.rustcomponents.sdk.crypto.SignatureState
import org.matrix.rustcomponents.sdk.crypto.SignatureVerification
import timber.log.Timber
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
        private val megolmSessionImportManager: MegolmSessionImportManager,
        private val cryptoCoroutineScope: CoroutineScope,
        private val matrixConfiguration: MatrixConfiguration,
        private val backupQueryRateLimiter: dagger.Lazy<PerSessionBackupQueryRateLimiter>,
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

    private val importScope = CoroutineScope(cryptoCoroutineScope.coroutineContext + SupervisorJob() + CoroutineName("backupImport"))

    private var keysBackupStateListener: KeysBackupStateListener? = null

    override fun isEnabled() = keysBackupStateManager.isEnabled

    override fun isStuck() = keysBackupStateManager.isStuck

    override fun getState() = keysBackupStateManager.state

    override val currentBackupVersion: String?
        get() = keysBackupVersion?.version

    override fun addListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.addListener(listener)
    }

    override fun removeListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.removeListener(listener)
    }

    override suspend fun prepareKeysBackupVersion(password: String?, progressListener: ProgressListener?): MegolmBackupCreationInfo {
        return withContext(coroutineDispatchers.computation) {
            val key = if (password != null) {
                // this might be a bit slow as it's stretching the password
                BackupRecoveryKey.newFromPassphrase(password)
            } else {
                BackupRecoveryKey()
            }

            val publicKey = key.megolmV1PublicKey()
            val backupAuthData = SignalableMegolmBackupAuthData(
                    publicKey = publicKey.publicKey,
                    privateKeySalt = publicKey.privateKeySalt,
                    privateKeyIterations = publicKey.privateKeyIterations
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
                    algorithm = publicKey.backupAlgorithm,
                    authData = signedMegolmBackupAuthData,
                    recoveryKey = key
            )
        }
    }

    override suspend fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo): KeysVersion {
        return withContext(coroutineDispatchers.crypto) {
            val createKeysBackupVersionBody = CreateKeysBackupVersionBody(
                    algorithm = keysBackupCreationInfo.algorithm,
                    authData = keysBackupCreationInfo.authData.toJsonDict()
            )

            keysBackupStateManager.state = KeysBackupState.Enabling

            try {
                val data = withContext(coroutineDispatchers.io) {
                    sender.createKeyBackup(createKeysBackupVersionBody)
                }
                // Reset backup markers.
                // Don't we need to join the task here? Isn't this a race condition?
                olmMachine.disableBackup()

                val keyBackupVersion = KeysVersionResult(
                        algorithm = createKeysBackupVersionBody.algorithm,
                        authData = createKeysBackupVersionBody.authData,
                        version = data.version,
                        // We can assume that the server does not have keys yet
                        count = 0,
                        hash = ""
                )
                enableKeysBackup(keyBackupVersion)
                data
            } catch (failure: Throwable) {
                keysBackupStateManager.state = KeysBackupState.Disabled
                throw failure
            }
        }
    }

    override fun saveBackupRecoveryKey(recoveryKey: IBackupRecoveryKey?, version: String?) {
        cryptoCoroutineScope.launch {
            olmMachine.saveRecoveryKey((recoveryKey as? BackupRecoveryKey)?.inner, version)
        }
    }

    private fun resetBackupAllGroupSessionsListeners() {
//        backupAllGroupSessionsCallback = null

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

    override suspend fun deleteBackup(version: String) {
        withContext(coroutineDispatchers.crypto) {
            if (keysBackupVersion != null && version == keysBackupVersion?.version) {
                resetKeysBackupData()
                keysBackupVersion = null
                keysBackupStateManager.state = KeysBackupState.Unknown
            }
            val state = getState()

            try {
                sender.deleteKeyBackup(version)
                // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                if (state == KeysBackupState.Unknown) {
                    checkAndStartKeysBackup()
                }
            } catch (failure: Throwable) {
                // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                if (state == KeysBackupState.Unknown) {
                    checkAndStartKeysBackup()
                }
            }
        }
    }

    override suspend fun canRestoreKeys(): Boolean {
        val keyCountOnServer = keysBackupVersion?.count ?: return false
        val keyCountLocally = getTotalNumbersOfKeys()

        // TODO is this sensible? We may have the same number of keys, or even more keys locally
        //  but the set of keys doesn't necessarily overlap
        return keyCountLocally < keyCountOnServer
    }

    override suspend fun getTotalNumbersOfKeys(): Int {
        return olmMachine.roomKeyCounts().total.toInt()
    }

    override suspend fun getTotalNumbersOfBackedUpKeys(): Int {
        return olmMachine.roomKeyCounts().backedUp.toInt()
    }

//    override fun backupAllGroupSessions(progressListener: ProgressListener?,
//                                        callback: MatrixCallback<Unit>?) {
//        // This is only used in tests? While it's fine have methods that are
//        // only used for tests, this one has a lot of logic that is nowhere else used.
//        TODO()
//    }

    private suspend fun checkBackupTrust(algAndData: KeysAlgorithmAndData?): KeysBackupVersionTrust {
        if (algAndData == null) return KeysBackupVersionTrust(usable = false)
        try {
            val authData = olmMachine.checkAuthDataSignature(algAndData)
            val signatures = authData.mapRustToAPI()
            return KeysBackupVersionTrust(authData.trusted, signatures)
        } catch (failure: Throwable) {
            Timber.w(failure, "Failed to trust backup")
            return KeysBackupVersionTrust(usable = false)
        }
    }

    private suspend fun SignatureVerification.mapRustToAPI(): List<KeysBackupVersionTrustSignature> {
        val signatures = mutableListOf<KeysBackupVersionTrustSignature>()
        // signature state of own device
        val ownDeviceState = this.deviceSignature
        if (ownDeviceState != SignatureState.MISSING && ownDeviceState != SignatureState.INVALID) {
            // we can add it
            signatures.add(
                    KeysBackupVersionTrustSignature.DeviceSignature(
                            olmMachine.deviceId(),
                            olmMachine.getCryptoDeviceInfo(olmMachine.userId(), olmMachine.deviceId()),
                            ownDeviceState == SignatureState.VALID_AND_TRUSTED
                    )
            )
        }
        // signature state of our own identity
        val ownIdentityState = this.userIdentitySignature
        if (ownIdentityState != SignatureState.MISSING && ownIdentityState != SignatureState.INVALID) {
            // we can add it
            val masterKey = olmMachine.getIdentity(olmMachine.userId())?.toMxCrossSigningInfo()?.masterKey()
            signatures.add(
                    KeysBackupVersionTrustSignature.UserSignature(
                            masterKey?.unpaddedBase64PublicKey,
                            masterKey,
                            ownIdentityState == SignatureState.VALID_AND_TRUSTED
                    )
            )
        }
        signatures.addAll(
                this.otherDevicesSignatures
                        .filter { it.value == SignatureState.VALID_AND_TRUSTED || it.value == SignatureState.VALID_BUT_NOT_TRUSTED }
                        .map {
                            KeysBackupVersionTrustSignature.DeviceSignature(
                                    it.key,
                                    olmMachine.getCryptoDeviceInfo(olmMachine.userId(), it.key),
                                    ownDeviceState == SignatureState.VALID_AND_TRUSTED
                            )
                        }
        )
        return signatures
    }

    override suspend fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult): KeysBackupVersionTrust {
        return withContext(coroutineDispatchers.crypto) {
            checkBackupTrust(keysBackupVersion)
        }
    }

    override suspend fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult, trust: Boolean) {
        withContext(coroutineDispatchers.crypto) {
            Timber.v("trustKeyBackupVersion: $trust, version ${keysBackupVersion.version}")

            // Get auth data to update it
            val authData = getMegolmBackupAuthData(keysBackupVersion)

            if (authData == null) {
                Timber.w("trustKeyBackupVersion:trust: Key backup is missing required data")
                throw IllegalArgumentException("Missing element")
            } else {
                // Get current signatures, or create an empty set
                val userId = olmMachine.userId()
                val signatures = authData.signatures?.get(userId).orEmpty().toMutableMap()

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
                val newSignatures = newAuthData.signatures.orEmpty().toMutableMap()
                newSignatures[userId] = signatures

                val body = UpdateKeysBackupVersionBody(
                        algorithm = keysBackupVersion.algorithm,
                        authData = newAuthData.copy(signatures = newSignatures).toJsonDict(),
                        version = keysBackupVersion.version
                )

                withContext(coroutineDispatchers.io) {
                    sender.updateBackup(keysBackupVersion, body)
                }

                val newKeysBackupVersion = KeysVersionResult(
                        algorithm = keysBackupVersion.algorithm,
                        authData = body.authData,
                        version = keysBackupVersion.version,
                        hash = keysBackupVersion.hash,
                        count = keysBackupVersion.count
                )

                checkAndStartWithKeysBackupVersion(newKeysBackupVersion)
            }
        }
    }

    // Check that the recovery key matches to the public key that we downloaded from the server.
// If they match, we can trust the public key and enable backups since we have the private key.
    private fun checkRecoveryKey(recoveryKey: IBackupRecoveryKey, keysBackupData: KeysVersionResult) {
        val backupKey = recoveryKey.megolmV1PublicKey()
        val authData = getMegolmBackupAuthData(keysBackupData)

        when {
            authData == null -> {
                Timber.w("isValidRecoveryKeyForKeysBackupVersion: Key backup is missing required data")
                throw IllegalArgumentException("Missing element")
            }
            backupKey.publicKey != authData.publicKey -> {
                Timber.w("isValidRecoveryKeyForKeysBackupVersion: Public keys mismatch")
                throw IllegalArgumentException("Invalid recovery key or password")
            }
            else -> {
                // This case is fine, the public key on the server matches the public key the
                // recovery key produced.
            }
        }
    }

    override suspend fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult, recoveryKey: IBackupRecoveryKey) {
        Timber.v("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")
        withContext(coroutineDispatchers.crypto) {
            // This is ~nowhere mentioned, the string here is actually a base58 encoded key.
            // This not really supported by the spec for the backup key, the 4S key supports
            // base58 encoding and the same method seems to be used here.
            checkRecoveryKey(recoveryKey, keysBackupVersion)
            trustKeysBackupVersion(keysBackupVersion, true)
        }
    }

    override suspend fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult, password: String) {
        withContext(coroutineDispatchers.crypto) {
            val key = recoveryKeyFromPassword(password, keysBackupVersion)
            checkRecoveryKey(key, keysBackupVersion)
            trustKeysBackupVersion(keysBackupVersion, true)
        }
    }

    override suspend fun onSecretKeyGossip(secret: String) {
        Timber.i("## CrossSigning - onSecretKeyGossip")
        withContext(coroutineDispatchers.crypto) {
            try {
                val version = sender.getKeyBackupLastVersion()?.toKeysVersionResult()
                Timber.v("Keybackup version: $version")
                if (version != null) {
                    val key = BackupRecoveryKey.fromBase64(secret)
                    if (isValidRecoveryKey(key, version)) {
                        // we can save, it's valid
                        saveBackupRecoveryKey(key, version.version)
                        importScope.launch {
                            backupQueryRateLimiter.get().refreshBackupInfoIfNeeded(true)
                        }
                        // we don't want to wait for that
//                        importScope.launch {
//                            try {
//                                val importResult = restoreBackup(version, key, null, null, null)
//                                val recoveredKeys = importResult.successfullyNumberOfImportedKeys
//                                Timber.i("onSecretKeyGossip: Recovered keys $recoveredKeys out of ${importResult.totalNumberOfKeys}")
//                            } catch (failure: Throwable) {
//                                // fail silently..
//                                Timber.e(failure, "onSecretKeyGossip: Failed to import keys from backup")
//                            }
//                        }
                    } else {
                        Timber.d("Invalid recovery key")
                    }
                } else {
                    Timber.e("onSecretKeyGossip: Failed to import backup recovery key, no backup version was found on the server")
                }
            } catch (failure: Throwable) {
                Timber.e("onSecretKeyGossip: failed to trust key backup version ${keysBackupVersion?.version}: $failure")
            }
        }
    }

    override suspend fun getBackupProgress(progressListener: ProgressListener) {
        val backedUpKeys = getTotalNumbersOfBackedUpKeys()
        val total = getTotalNumbersOfKeys()

        progressListener.onProgress(backedUpKeys, total)
    }

    /**
     * Same method as [RoomKeysRestClient.getRoomKey] except that it accepts nullable
     * parameters and always returns a KeysBackupData object through the Callback.
     */
    private suspend fun getKeys(sessionId: String?, roomId: String?, version: String): KeysBackupData {
        return when {
            roomId != null && sessionId != null -> {
                sender.downloadBackedUpKeys(version, roomId, sessionId)
            }
            roomId != null -> {
                sender.downloadBackedUpKeys(version, roomId)
            }
            else -> {
                sender.downloadBackedUpKeys(version)
            }
        }
    }

    @VisibleForTesting
    @WorkerThread
    fun decryptKeyBackupData(keyBackupData: KeyBackupData, sessionId: String, roomId: String, key: IBackupRecoveryKey): MegolmSessionData? {
        var sessionBackupData: MegolmSessionData? = null

        val jsonObject = keyBackupData.sessionData

        val ciphertext = jsonObject["ciphertext"]?.toString()
        val mac = jsonObject["mac"]?.toString()
        val ephemeralKey = jsonObject["ephemeral"]?.toString()

        if (ciphertext != null && mac != null && ephemeralKey != null) {
            try {
                val decrypted = key.decryptV1(ephemeralKey, mac, ciphertext)

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
            recoveryKey: IBackupRecoveryKey,
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

            // Save for next time and for gossiping
            saveBackupRecoveryKey(recoveryKey, keysVersionResult.version)
        }

        withContext(coroutineDispatchers.main) {
            stepProgressListener?.onStepProgress(StepProgressListener.Step.DownloadingKey)
        }

        // Get backed up keys from the homeserver
        val data = getKeys(sessionId, roomId, keysVersionResult.version)

        return withContext(coroutineDispatchers.computation) {
            withContext(coroutineDispatchers.main) {
                stepProgressListener?.onStepProgress(StepProgressListener.Step.DecryptingKey(0, data.roomIdToRoomKeysBackupData.size))
            }
            // Decrypting by chunk of 500 keys in parallel
            // we loose proper progress report but tested 3x faster on big backup
            val sessionsData = data.roomIdToRoomKeysBackupData
                    .mapValues {
                        it.value.sessionIdToKeyBackupData
                    }
                    .flatMap { flat ->
                        flat.value.entries.map { flat.key to it }
                    }
                    .chunked(500)
                    .map { slice ->
                        async {
                            slice.mapNotNull { pair ->
                                decryptKeyBackupData(pair.second.value, pair.second.key, pair.first, recoveryKey)
                            }
                        }
                    }
                    .awaitAll()
                    .flatten()

            withContext(coroutineDispatchers.main) {
                val stepProgress = StepProgressListener.Step.DecryptingKey(data.roomIdToRoomKeysBackupData.size, data.roomIdToRoomKeysBackupData.size)
                stepProgressListener?.onStepProgress(stepProgress)
            }

            Timber.v(
                    "restoreKeysWithRecoveryKey: Decrypted ${sessionsData.size} keys out" +
                            " of ${data.roomIdToRoomKeysBackupData.size} rooms from the backup store on the homeserver"
            )

            // Do not trigger a backup for them if they come from the backup version we are using
            val backUp = keysVersionResult.version != keysBackupVersion?.version
            if (backUp) {
                Timber.v(
                        "restoreKeysWithRecoveryKey: Those keys will be backed up" +
                                " to backup version: ${keysBackupVersion?.version}"
                )
            }

            // Import them into the crypto store
            val progressListener = if (stepProgressListener != null) {
                object : ProgressListener {
                    override fun onProgress(progress: Int, total: Int) {
                        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
                            val stepProgress = StepProgressListener.Step.ImportingKey(progress, total)
                            stepProgressListener.onStepProgress(stepProgress)
                        }
                    }
                }
            } else {
                null
            }

            val result = olmMachine.importDecryptedKeys(sessionsData, progressListener).also {
                sessionsData.onEach { sessionData ->
                    matrixConfiguration.cryptoAnalyticsPlugin
                            ?.onRoomKeyImported(sessionData.sessionId.orEmpty(), keysVersionResult.algorithm)
                }
                megolmSessionImportManager.dispatchKeyImportResults(it)
            }

            // Do not back up the key if it comes from a backup recovery
            if (backUp) {
                maybeBackupKeys()
            }

            result
        }
    }

    override suspend fun restoreKeysWithRecoveryKey(
            keysVersionResult: KeysVersionResult,
            recoveryKey: IBackupRecoveryKey,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?
    ): ImportRoomKeysResult {
        Timber.v("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version}")
        return restoreBackup(keysVersionResult, recoveryKey, roomId, sessionId, stepProgressListener)
    }

    override suspend fun restoreKeyBackupWithPassword(
            keysBackupVersion: KeysVersionResult,
            password: String,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?
    ): ImportRoomKeysResult {
        Timber.v("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")
        val recoveryKey = withContext(coroutineDispatchers.crypto) {
            recoveryKeyFromPassword(password, keysBackupVersion)
        }
        return restoreBackup(keysBackupVersion, recoveryKey, roomId, sessionId, stepProgressListener)
    }

    override suspend fun getVersion(version: String): KeysVersionResult? {
        return sender.getKeyBackupVersion(version)
    }

    @Throws
    override suspend fun getCurrentVersion(): KeysBackupLastVersionResult? {
        return sender.getKeyBackupLastVersion()
    }

    override suspend fun forceUsingLastVersion(): Boolean {
        val response = withContext(coroutineDispatchers.io) {
            sender.getKeyBackupLastVersion()?.toKeysVersionResult()
        }

        return withContext(coroutineDispatchers.crypto) {
            val serverBackupVersion = response?.version
            val localBackupVersion = keysBackupVersion?.version

            Timber.d("BACKUP: $serverBackupVersion")

            if (serverBackupVersion == null) {
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
                        tryOrNull("Failed to automatically check for the last version") {
                            deleteBackup(localBackupVersion)
                        }
                        // We are not using the last version, so delete the current version we are using on the server
                        false
                    }
                }
            }
        }
    }

    override suspend fun checkAndStartKeysBackup() {
        withContext(coroutineDispatchers.crypto) {
            if (!isStuck()) {
                // Try to start or restart the backup only if it is in unknown or bad state
                Timber.w("checkAndStartKeysBackup: invalid state: ${getState()}")
                return@withContext
            }
            keysBackupVersion = null
            keysBackupStateManager.state = KeysBackupState.CheckingBackUpOnHomeserver
            try {
                val data = getCurrentVersion()?.toKeysVersionResult()
                withContext(coroutineDispatchers.crypto) {
                    checkAndStartWithKeysBackupVersion(data)
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "checkAndStartKeysBackup: Failed to get current version")
                withContext(coroutineDispatchers.crypto) {
                    keysBackupStateManager.state = KeysBackupState.Unknown
                }
            }
        }
    }

    private suspend fun checkAndStartWithKeysBackupVersion(keyBackupVersion: KeysVersionResult?) {
        Timber.v("checkAndStartWithKeyBackupVersion: ${keyBackupVersion?.version}")

        keysBackupVersion = keyBackupVersion

        if (keyBackupVersion == null) {
            Timber.v("checkAndStartWithKeysBackupVersion: Found no key backup version on the homeserver")
            resetKeysBackupData()
            keysBackupStateManager.state = KeysBackupState.Disabled
        } else {
            try {
                val data = getKeysBackupTrust(keyBackupVersion)
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
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to checkAndStartWithKeysBackupVersion $keyBackupVersion")
            }
        }
    }

    private fun isValidRecoveryKey(recoveryKey: IBackupRecoveryKey, version: KeysVersionResult): Boolean {
        val publicKey = recoveryKey.megolmV1PublicKey().publicKey
        val authData = getMegolmBackupAuthData(version) ?: return false
        Timber.v("recoveryKey.megolmV1PublicKey().publicKey $publicKey == getMegolmBackupAuthData(version).publicKey ${authData.publicKey}")
        return authData.publicKey == publicKey
    }

    override suspend fun isValidRecoveryKeyForCurrentVersion(recoveryKey: IBackupRecoveryKey): Boolean {
        return withContext(coroutineDispatchers.crypto) {
            val keysBackupVersion = keysBackupVersion ?: return@withContext false
            try {
                isValidRecoveryKey(recoveryKey, keysBackupVersion)
            } catch (failure: Throwable) {
                Timber.i("isValidRecoveryKeyForCurrentVersion: Invalid recovery key")
                false
            }
        }
    }

    override fun computePrivateKey(passphrase: String, privateKeySalt: String, privateKeyIterations: Int, progressListener: ProgressListener): ByteArray {
        return deriveKey(passphrase, privateKeySalt, privateKeyIterations, progressListener)
    }

    override suspend fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        val info = olmMachine.getBackupKeys() ?: return null
        val backupRecoveryKey = BackupRecoveryKey(info.recoveryKey())
        return SavedKeyBackupKeyInfo(backupRecoveryKey, info.backupVersion())
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
    private fun recoveryKeyFromPassword(password: String, keysBackupData: KeysVersionResult): BackupRecoveryKey {
        val authData = getMegolmBackupAuthData(keysBackupData)

        return when {
            authData == null -> {
                throw IllegalArgumentException("recoveryKeyFromPassword: invalid parameter")
            }
            authData.privateKeySalt.isNullOrBlank() || authData.privateKeyIterations == null -> {
                throw java.lang.IllegalArgumentException("recoveryKeyFromPassword: Salt and/or iterations not found in key backup auth data")
            }
            else -> {
                BackupRecoveryKey.fromPassphrase(password, authData.privateKeySalt, authData.privateKeyIterations)
            }
        }
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
                olmMachine.enableBackupV1(retrievedMegolmBackupAuthData.publicKey, keysVersionResult.version)
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
     * Do a backup if there are new keys, with a delay.
     */
    suspend fun maybeBackupKeys() {
        withContext(coroutineDispatchers.crypto) {
            when {
                isStuck() -> {
                    // If not already done, or in error case, check for a valid backup version on the homeserver.
                    // If there is one, maybeBackupKeys will be called again.
                    checkAndStartKeysBackup()
                }
                getState() == KeysBackupState.ReadyToBackUp -> {
                    keysBackupStateManager.state = KeysBackupState.WillBackUp

                    // Wait between 0 and 10 seconds, to avoid backup requests from
                    // different clients hitting the server all at the same time when a
                    // new key is sent
                    val delayInMs = Random.nextLong(KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS)

                    importScope.launch {
                        delay(delayInMs)
                        tryOrNull("AUTO backup failed") { backupKeys() }
                    }
                }
                else -> {
                    Timber.v("maybeBackupKeys: Skip it because state: ${getState()}")
                }
            }
        }
    }

    /**
     * Send a chunk of keys to backup.
     */
    private suspend fun backupKeys(forceRecheck: Boolean = false) {
        Timber.v("backupKeys")
        withContext(coroutineDispatchers.crypto) {
            val isEnabled = isEnabled()
            val state = getState()
            // Sanity check, as this method can be called after a delay, the state may have change during the delay
            if (!isEnabled || !olmMachine.backupEnabled() || keysBackupVersion == null) {
                Timber.v("backupKeys: Invalid configuration $isEnabled ${olmMachine.backupEnabled()} $keysBackupVersion")
//            backupAllGroupSessionsCallback?.onFailure(IllegalStateException("Invalid configuration"))
                resetBackupAllGroupSessionsListeners()

                return@withContext
            }

            if (state === KeysBackupState.BackingUp && !forceRecheck) {
                // Do nothing if we are already backing up
                Timber.v("backupKeys: Invalid state: $state")
                return@withContext
            }

            Timber.d("BACKUP: CREATING REQUEST")

            val request = olmMachine.backupRoomKeys()

            Timber.d("BACKUP: GOT REQUEST $request")

            if (request == null) {
                // Backup is up to date
                // Note: Changing state will trigger the call to backupAllGroupSessionsCallback.onSuccess()
                keysBackupStateManager.state = KeysBackupState.ReadyToBackUp

//            backupAllGroupSessionsCallback?.onSuccess(Unit)
                resetBackupAllGroupSessionsListeners()
            } else {
                try {
                    if (request is Request.KeysBackup) {
                        keysBackupStateManager.state = KeysBackupState.BackingUp

                        Timber.d("BACKUP SENDING REQUEST")
                        val response = withContext(coroutineDispatchers.io) { sender.backupRoomKeys(request) }
                        Timber.d("BACKUP GOT RESPONSE $response")
                        olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_BACKUP, response)
                        Timber.d("BACKUP MARKED REQUEST AS SENT")

                        backupKeys(true)
                    } else {
                        // Can't happen, do we want to panic?
                    }
                } catch (failure: Throwable) {
                    if (failure is Failure.ServerError) {
                        withContext(coroutineDispatchers.main) {
                            Timber.e(failure, "backupKeys: backupKeys failed.")

                            when (failure.error.code) {
                                MatrixError.M_NOT_FOUND,
                                MatrixError.M_WRONG_ROOM_KEYS_VERSION -> {
                                    // Backup has been deleted on the server, or we are not using
                                    // the last backup version
                                    keysBackupStateManager.state = KeysBackupState.WrongBackUpVersion
//                                backupAllGroupSessionsCallback?.onFailure(failure)
                                    resetBackupAllGroupSessionsListeners()
                                    resetKeysBackupData()
                                    keysBackupVersion = null

                                    // Do not stay in KeysBackupState.WrongBackUpVersion but check what
                                    // is available on the homeserver
                                    checkAndStartKeysBackup()
                                }
                                else ->
                                    // Come back to the ready state so that we will retry on the next received key
                                    keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                            }
                        }
                    } else {
//                        backupAllGroupSessionsCallback?.onFailure(failure)
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
