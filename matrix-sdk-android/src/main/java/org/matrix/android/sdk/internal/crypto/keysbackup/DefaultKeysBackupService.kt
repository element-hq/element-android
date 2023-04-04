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
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeyBackupConfig
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
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.internal.crypto.InboundGroupSessionStore
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.ObjectSigner
import org.matrix.android.sdk.internal.crypto.actions.MegolmSessionDataImporter
import org.matrix.android.sdk.internal.crypto.crosssigning.CrossSigningOlm
import org.matrix.android.sdk.internal.crypto.keysbackup.algorithm.KeysBackupAlgorithm
import org.matrix.android.sdk.internal.crypto.keysbackup.algorithm.KeysBackupAlgorithmFactory
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.BackupKeysResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeyBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.RoomKeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.UpdateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.CreateKeysBackupVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.DeleteBackupTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupLastVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetRoomSessionDataTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetRoomSessionsDataTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetSessionsDataTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.StoreSessionsDataTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.UpdateKeysBackupVersionTask
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.TaskThread
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmException
import timber.log.Timber
import java.security.InvalidParameterException
import javax.inject.Inject
import kotlin.random.Random

/**
 * A DefaultKeysBackupService class instance manage incremental backup of e2e keys (megolm keys)
 * to the user's homeserver.
 */

private const val DEFAULT_ALGORITHM = MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP

@SessionScope
internal class DefaultKeysBackupService @Inject constructor(
        @UserId private val userId: String,
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val olmDevice: MXOlmDevice,
        private val objectSigner: ObjectSigner,
        private val crossSigningOlm: CrossSigningOlm,
        // Actions
        private val megolmSessionDataImporter: MegolmSessionDataImporter,
        // Tasks
        private val createKeysBackupVersionTask: CreateKeysBackupVersionTask,
        private val deleteBackupTask: DeleteBackupTask,
        private val getKeysBackupLastVersionTask: GetKeysBackupLastVersionTask,
        private val getKeysBackupVersionTask: GetKeysBackupVersionTask,
        private val getRoomSessionDataTask: GetRoomSessionDataTask,
        private val getRoomSessionsDataTask: GetRoomSessionsDataTask,
        private val getSessionsDataTask: GetSessionsDataTask,
        private val storeSessionDataTask: StoreSessionsDataTask,
        private val updateKeysBackupVersionTask: UpdateKeysBackupVersionTask,
        // Task executor
        private val taskExecutor: TaskExecutor,
        private val algorithmFactory: KeysBackupAlgorithmFactory,
        private val inboundGroupSessionStore: InboundGroupSessionStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val prepareKeysBackup: PrepareKeysBackupUseCase,
        private val keysBackupStateManager: KeysBackupStateManager,
        private val uiHandler: Handler,
) : KeysBackupService {

    override var keyBackupConfig = KeyBackupConfig(
            defaultAlgorithm = MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP,
            supportedAlgorithms = listOf(MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP)
    )

    // The backup version
    override var keysBackupVersion: KeysVersionResult? = null
        private set

    private var algorithm: KeysBackupAlgorithm? = null

    private var backupAllGroupSessionsCallback: MatrixCallback<Unit>? = null

    private var keysBackupStateListener: KeysBackupStateListener? = null

    override fun isEnabled(): Boolean = keysBackupStateManager.isEnabled

    override fun isStuck(): Boolean = keysBackupStateManager.isStuck

    override fun getState(): KeysBackupState = keysBackupStateManager.state

    override fun addListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.addListener(listener)
    }

    override fun removeListener(listener: KeysBackupStateListener) {
        keysBackupStateManager.removeListener(listener)
    }

    override fun prepareKeysBackupVersion(
            password: String?,
            algorithm: String?,
            progressListener: ProgressListener?,
            callback: MatrixCallback<MegolmBackupCreationInfo>
    ) {
        cryptoCoroutineScope.launch {
            prepareKeysBackup(
                    algorithm = algorithm ?: keyBackupConfig.defaultAlgorithm,
                    password = password,
                    progressListener = progressListener,
                    config = keyBackupConfig,
                    callback = callback
            )
        }
    }

    override fun createKeysBackupVersion(
            keysBackupCreationInfo: MegolmBackupCreationInfo,
            callback: MatrixCallback<KeysVersion>
    ) {
        if (!keyBackupConfig.isAlgorithmSupported(keysBackupCreationInfo.algorithm)) return Unit.also {
            callback.onFailure(IllegalArgumentException("Unsupported algorithm"))
        }
        @Suppress("UNCHECKED_CAST")
        val createKeysBackupVersionBody = CreateKeysBackupVersionBody(
                algorithm = keysBackupCreationInfo.algorithm,
                authData = keysBackupCreationInfo.authData.toJsonDict()
        )

        keysBackupStateManager.state = KeysBackupState.Enabling

        createKeysBackupVersionTask
                .configureWith(createKeysBackupVersionBody) {
                    this.callback = object : MatrixCallback<KeysVersion> {
                        override fun onSuccess(data: KeysVersion) {
                            // Reset backup markers.
                            cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                                // move tx out of UI thread
                                cryptoStore.resetBackupMarkers()
                            }

                            val keyBackupVersion = KeysVersionResult(
                                    algorithm = createKeysBackupVersionBody.algorithm,
                                    authData = createKeysBackupVersionBody.authData,
                                    version = data.version,
                                    // We can consider that the server does not have keys yet
                                    count = 0,
                                    hash = ""
                            )

                            enableKeysBackup(keyBackupVersion)

                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            keysBackupStateManager.state = KeysBackupState.Disabled
                            callback.onFailure(failure)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun deleteBackup(version: String, callback: MatrixCallback<Unit>?) {
        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            // If we're currently backing up to this backup... stop.
            // (We start using it automatically in createKeysBackupVersion so this is symmetrical).
            if (keysBackupVersion != null && version == keysBackupVersion?.version) {
                resetKeysBackupData()
                keysBackupVersion = null
                keysBackupStateManager.state = KeysBackupState.Unknown
            }

            deleteBackupTask
                    .configureWith(DeleteBackupTask.Params(version)) {
                        this.callback = object : MatrixCallback<Unit> {
                            private fun eventuallyRestartBackup() {
                                // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                                if (getState() == KeysBackupState.Unknown) {
                                    checkAndStartKeysBackup()
                                }
                            }

                            override fun onSuccess(data: Unit) {
                                eventuallyRestartBackup()

                                uiHandler.post { callback?.onSuccess(Unit) }
                            }

                            override fun onFailure(failure: Throwable) {
                                eventuallyRestartBackup()

                                uiHandler.post { callback?.onFailure(failure) }
                            }
                        }
                    }
                    .executeBy(taskExecutor)
        }
    }

    override fun canRestoreKeys(): Boolean {
        // Server contains more keys than locally
        val totalNumberOfKeysLocally = getTotalNumbersOfKeys()

        val keysBackupData = cryptoStore.getKeysBackupData()

        val totalNumberOfKeysServer = keysBackupData?.backupLastServerNumberOfKeys ?: -1
        // Not used for the moment
        // val hashServer = keysBackupData?.backupLastServerHash

        return when {
            totalNumberOfKeysLocally < totalNumberOfKeysServer -> {
                // Server contains more keys than this device
                true
            }
            totalNumberOfKeysLocally == totalNumberOfKeysServer -> {
                // Same number, compare hash?
                // TODO We have not found any algorithm to determine if a restore is recommended here. Return false for the moment
                false
            }
            else -> false
        }
    }

    override fun getTotalNumbersOfKeys(): Int {
        return cryptoStore.inboundGroupSessionsCount(false)
    }

    override fun getTotalNumbersOfBackedUpKeys(): Int {
        return cryptoStore.inboundGroupSessionsCount(true)
    }

    override fun backupAllGroupSessions(
            progressListener: ProgressListener?,
            callback: MatrixCallback<Unit>?
    ) {
        if (!isEnabled() || algorithm == null || keysBackupVersion == null) {
            callback?.onFailure(Throwable("Backup not enabled"))
            return
        }
        // Get a status right now
        getBackupProgress(object : ProgressListener {
            override fun onProgress(progress: Int, total: Int) {
                // Reset previous listeners if any
                resetBackupAllGroupSessionsListeners()
                Timber.v("backupAllGroupSessions: backupProgress: $progress/$total")
                try {
                    progressListener?.onProgress(progress, total)
                } catch (e: Exception) {
                    Timber.e(e, "backupAllGroupSessions: onProgress failure")
                }

                if (progress == total) {
                    Timber.v("backupAllGroupSessions: complete")
                    callback?.onSuccess(Unit)
                    return
                }

                backupAllGroupSessionsCallback = callback

                // Listen to `state` change to determine when to call onBackupProgress and onComplete
                keysBackupStateListener = object : KeysBackupStateListener {
                    override fun onStateChange(newState: KeysBackupState) {
                        getBackupProgress(object : ProgressListener {
                            override fun onProgress(progress: Int, total: Int) {
                                try {
                                    progressListener?.onProgress(progress, total)
                                } catch (e: Exception) {
                                    Timber.e(e, "backupAllGroupSessions: onProgress failure 2")
                                }

                                // If backup is finished, notify the main listener
                                if (getState() === KeysBackupState.ReadyToBackUp) {
                                    backupAllGroupSessionsCallback?.onSuccess(Unit)
                                    resetBackupAllGroupSessionsListeners()
                                }
                            }
                        })
                    }
                }.also { keysBackupStateManager.addListener(it) }

                backupKeys()
            }
        })
    }

    override fun getKeysBackupTrust(
            keysBackupVersion: KeysVersionResult,
            callback: MatrixCallback<KeysBackupVersionTrust>
    ) {
        // TODO Validate with François that this is correct
        object : Task<KeysVersionResult, KeysBackupVersionTrust> {
            override suspend fun execute(params: KeysVersionResult): KeysBackupVersionTrust {
                return getKeysBackupTrustBg(params)
            }
        }
                .configureWith(keysBackupVersion) {
                    this.callback = callback
                    this.executionThread = TaskThread.COMPUTATION
                }
                .executeBy(taskExecutor)
    }

    /**
     * Check trust on a key backup version.
     * This has to be called on background thread.
     *
     * @param keysBackupVersion the backup version to check.
     * @return a KeysBackupVersionTrust object
     */
    @WorkerThread
    private fun getKeysBackupTrustBg(keysBackupVersion: KeysVersionResult): KeysBackupVersionTrust {
        val authData = keysBackupVersion.getAuthDataAsMegolmBackupAuthData()

        if (!keyBackupConfig.isAlgorithmSupported(keysBackupVersion.algorithm)) {
            return KeysBackupVersionTrust(usable = false)
        }

        if (authData == null || authData.signatures.isNullOrEmpty()) {
            Timber.v("getKeysBackupTrust: Key backup is absent or missing required data")
            return KeysBackupVersionTrust(usable = false)
        }
        val signatures = authData.signatures!!
        val mySigs = authData.signatures?.get(userId)
        if (mySigs.isNullOrEmpty()) {
            Timber.v("getKeysBackupTrust: Ignoring key backup because it lacks any signatures from this user")
            return KeysBackupVersionTrust(usable = false)
        }

        var keysBackupVersionTrustIsUsable = false
        val keysBackupVersionTrustSignatures = mutableListOf<KeysBackupVersionTrustSignature>()

        for ((keyId, mySignature) in mySigs) {
            // XXX: is this how we're supposed to get the device id?
            var deviceOrCrossSigningKeyId: String? = null
            val components = keyId.split(":")
            if (components.size == 2) {
                deviceOrCrossSigningKeyId = components[1]
            }

            // Let's check if it's my master key
            val myMSKPKey = cryptoStore.getMyCrossSigningInfo()?.masterKey()?.unpaddedBase64PublicKey
            if (deviceOrCrossSigningKeyId == myMSKPKey) {
                // we have to check if we can trust

                var isSignatureValid = false
                try {
                    crossSigningOlm.verifySignature(CrossSigningOlm.KeyType.MASTER, authData.toSignalableJsonDict(), signatures)
                    isSignatureValid = true
                } catch (failure: Throwable) {
                    Timber.w(failure, "getKeysBackupTrust: Bad signature from my user MSK")
                }
                val mskTrusted = cryptoStore.getMyCrossSigningInfo()?.masterKey()?.trustLevel?.isVerified() == true
                if (isSignatureValid && mskTrusted) {
                    keysBackupVersionTrustIsUsable = true
                }
                val signature = KeysBackupVersionTrustSignature.UserSignature(
                        keyId = deviceOrCrossSigningKeyId,
                        cryptoCrossSigningKey = cryptoStore.getMyCrossSigningInfo()?.masterKey(),
                        valid = isSignatureValid
                )

                keysBackupVersionTrustSignatures.add(signature)
            } else if (deviceOrCrossSigningKeyId != null) {
                val device = cryptoStore.getUserDevice(userId, deviceOrCrossSigningKeyId)
                var isSignatureValid = false

                if (device == null) {
                    Timber.v("getKeysBackupTrust: Signature from unknown device $deviceOrCrossSigningKeyId")
                } else {
                    val fingerprint = device.fingerprint()
                    if (fingerprint != null) {
                        try {
                            olmDevice.verifySignature(fingerprint, authData.toSignalableJsonDict(), mySignature)
                            isSignatureValid = true
                        } catch (e: OlmException) {
                            Timber.w(e, "getKeysBackupTrust: Bad signature from device ${device.deviceId}")
                        }
                    }

                    if (isSignatureValid && device.isVerified) {
                        keysBackupVersionTrustIsUsable = true
                    }
                }

                val signature = KeysBackupVersionTrustSignature.DeviceSignature(
                        deviceId = deviceOrCrossSigningKeyId,
                        device = device,
                        valid = isSignatureValid,
                )
                keysBackupVersionTrustSignatures.add(signature)
            }
        }

        return KeysBackupVersionTrust(
                usable = keysBackupVersionTrustIsUsable,
                signatures = keysBackupVersionTrustSignatures
        )
    }

    override fun trustKeysBackupVersion(
            keysBackupVersion: KeysVersionResult,
            trust: Boolean,
            callback: MatrixCallback<Unit>
    ) {
        Timber.v("trustKeyBackupVersion: $trust, version ${keysBackupVersion.version}")

        if (!keyBackupConfig.isAlgorithmSupported(keysBackupVersion.algorithm)) {
            Timber.w("trustKeyBackupVersion:trust unsupported algorithm ${keysBackupVersion.algorithm}")
            uiHandler.post {
                callback.onFailure(IllegalArgumentException("Missing element"))
            }
            return
        }

        // Get auth data to update it
        val authData = keysBackupVersion.getValidAuthData()

        if (authData == null) {
            Timber.w("trustKeyBackupVersion:trust: Key backup is missing required data")
            uiHandler.post {
                callback.onFailure(IllegalArgumentException("Missing element"))
            }
        } else {
            cryptoCoroutineScope.launch(coroutineDispatchers.io) {
                val updateKeysBackupVersionBody = withContext(coroutineDispatchers.crypto) {
                    // Get current signatures, or create an empty set
                    val myUserSignatures = authData.signatures?.get(userId).orEmpty().toMutableMap()

                    if (trust) {
                        // Add current device signature
                        val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, authData.toSignalableJsonDict())

                        val deviceSignatures = objectSigner.signObject(canonicalJson)

                        deviceSignatures[userId]?.forEach { entry ->
                            myUserSignatures[entry.key] = entry.value
                        }
                    } else {
                        // Remove current device signature
                        myUserSignatures.remove("ed25519:${credentials.deviceId}")
                    }

                    // Create an updated version of KeysVersionResult
                    val newSignatures = authData.signatures.orEmpty().toMutableMap()
                    newSignatures[userId] = myUserSignatures

                    val newMegolmBackupAuthDataWithNewSignature = authData.copy(newSignatures)

                    @Suppress("UNCHECKED_CAST")
                    UpdateKeysBackupVersionBody(
                            algorithm = keysBackupVersion.algorithm,
                            authData = newMegolmBackupAuthDataWithNewSignature.toJsonDict(),
                            version = keysBackupVersion.version
                    )
                }

                // And send it to the homeserver
                updateKeysBackupVersionTask
                        .configureWith(UpdateKeysBackupVersionTask.Params(keysBackupVersion.version, updateKeysBackupVersionBody)) {
                            this.callback = object : MatrixCallback<Unit> {
                                override fun onSuccess(data: Unit) {
                                    // Relaunch the state machine on this updated backup version
                                    val newKeysBackupVersion = KeysVersionResult(
                                            algorithm = keysBackupVersion.algorithm,
                                            authData = updateKeysBackupVersionBody.authData,
                                            version = keysBackupVersion.version,
                                            hash = keysBackupVersion.hash,
                                            count = keysBackupVersion.count
                                    )

                                    checkAndStartWithKeysBackupVersion(newKeysBackupVersion)

                                    uiHandler.post {
                                        callback.onSuccess(data)
                                    }
                                }

                                override fun onFailure(failure: Throwable) {
                                    uiHandler.post {
                                        callback.onFailure(failure)
                                    }
                                }
                            }
                        }
                        .executeBy(taskExecutor)
            }
        }
    }

    override fun trustKeysBackupVersionWithRecoveryKey(
            keysBackupVersion: KeysVersionResult,
            recoveryKey: String,
            callback: MatrixCallback<Unit>
    ) {
        Timber.v("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")

        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            val isValid = isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysBackupVersion)

            if (!isValid) {
                Timber.w("trustKeyBackupVersionWithRecoveryKey: Invalid recovery key.")
                uiHandler.post {
                    callback.onFailure(IllegalArgumentException("Invalid recovery key or password"))
                }
            } else {
                trustKeysBackupVersion(keysBackupVersion, true, callback)
            }
        }
    }

    override fun trustKeysBackupVersionWithPassphrase(
            keysBackupVersion: KeysVersionResult,
            password: String,
            callback: MatrixCallback<Unit>
    ) {
        Timber.v("trustKeysBackupVersionWithPassphrase: version ${keysBackupVersion.version}")

        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            val recoveryKey = recoveryKeyFromPassword(password, keysBackupVersion, null)

            if (recoveryKey == null) {
                Timber.w("trustKeysBackupVersionWithPassphrase: Key backup is missing required data")
                uiHandler.post {
                    callback.onFailure(IllegalArgumentException("Missing element"))
                }
            } else {
                // Check trust using the recovery key
                trustKeysBackupVersionWithRecoveryKey(keysBackupVersion, recoveryKey, callback)
            }
        }
    }

    fun onSecretKeyGossip(secret: String) {
        Timber.i("## CrossSigning - onSecretKeyGossip")

        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            try {
                val keysBackupVersion = getKeysBackupLastVersionTask.execute(Unit).toKeysVersionResult()
                        ?: return@launch Unit.also {
                            Timber.d("Failed to get backup last version")
                        }
                val recoveryKey = computeRecoveryKey(secret.fromBase64())
                if (isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysBackupVersion)) {
                    // we don't want to start immediately downloading all as it can take very long
                    withContext(coroutineDispatchers.crypto) {
                        cryptoStore.saveBackupRecoveryKey(recoveryKey, keysBackupVersion.version)
                    }
                    Timber.i("onSecretKeyGossip: saved valid backup key")
                } else {
                    Timber.e("onSecretKeyGossip: Recovery key is not valid ${keysBackupVersion.version}")
                }
            } catch (failure: Throwable) {
                Timber.e("onSecretKeyGossip: failed to trust key backup version ${keysBackupVersion?.version}")
            }
        }
    }

    private fun resetBackupAllGroupSessionsListeners() {
        backupAllGroupSessionsCallback = null

        keysBackupStateListener?.let {
            keysBackupStateManager.removeListener(it)
        }

        keysBackupStateListener = null
    }

    override fun getBackupProgress(progressListener: ProgressListener) {
        val backedUpKeys = cryptoStore.inboundGroupSessionsCount(true)
        val total = cryptoStore.inboundGroupSessionsCount(false)

        progressListener.onProgress(backedUpKeys, total)
    }

    override fun restoreKeysWithRecoveryKey(
            keysVersionResult: KeysVersionResult,
            recoveryKey: String,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?,
            callback: MatrixCallback<ImportRoomKeysResult>
    ) {
        Timber.v("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version} alg:${keysVersionResult.algorithm}")

        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            runCatching {
                if (!keyBackupConfig.isAlgorithmSupported(keysVersionResult.algorithm)) {
                    throw IllegalArgumentException("Unsupported algorithm")
                }
                val backupAlgorithm = algorithmFactory.create(keysVersionResult)
                val privateKey = extractCurveKeyFromRecoveryKey(recoveryKey)
                if (privateKey == null || !backupAlgorithm.keyMatches(privateKey)) {
                    Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key for this keys version")
                    throw InvalidParameterException("Invalid recovery key")
                }
                // Save for next time and for gossiping
                // Save now as it's valid, don't wait for the import as it could take long.
                backupAlgorithm.setPrivateKey(privateKey)
                saveBackupRecoveryKey(recoveryKey, keysVersionResult.version)
                stepProgressListener?.onStepProgress(StepProgressListener.Step.DownloadingKey)

                // Get backed up keys from the homeserver
                val data = getKeys(sessionId, roomId, keysVersionResult.version)
                extractCurveKeyFromRecoveryKey(recoveryKey)?.also { privateKey ->
                    backupAlgorithm.setPrivateKey(privateKey)
                }
                val sessionsData = withContext(coroutineDispatchers.computation) {
                    backupAlgorithm.decryptSessions(data)
                }
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
                            // Note: no need to post to UI thread, importMegolmSessionsData() will do it
                            stepProgressListener.onStepProgress(StepProgressListener.Step.ImportingKey(progress, total))
                        }
                    }
                } else {
                    null
                }

                val result = megolmSessionDataImporter.handle(sessionsData, !backUp, progressListener)

                // Do not back up the key if it comes from a backup recovery
                if (backUp) {
                    maybeBackupKeys()
                }
                result
            }.foldToCallback(object : MatrixCallback<ImportRoomKeysResult> {
                override fun onSuccess(data: ImportRoomKeysResult) {
                    uiHandler.post {
                        callback.onSuccess(data)
                    }
                }

                override fun onFailure(failure: Throwable) {
                    uiHandler.post {
                        callback.onFailure(failure)
                    }
                }
            })
        }
    }

    override fun restoreKeyBackupWithPassword(
            keysBackupVersion: KeysVersionResult,
            password: String,
            roomId: String?,
            sessionId: String?,
            stepProgressListener: StepProgressListener?,
            callback: MatrixCallback<ImportRoomKeysResult>
    ) {
        Timber.v("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")

        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            runCatching {
                val progressListener = if (stepProgressListener != null) {
                    object : ProgressListener {
                        override fun onProgress(progress: Int, total: Int) {
                            uiHandler.post {
                                stepProgressListener.onStepProgress(StepProgressListener.Step.ComputingKey(progress, total))
                            }
                        }
                    }
                } else {
                    null
                }

                val recoveryKey = withContext(coroutineDispatchers.crypto) {
                    recoveryKeyFromPassword(password, keysBackupVersion, progressListener)
                }
                if (recoveryKey == null) {
                    Timber.v("backupKeys: Invalid configuration")
                    throw IllegalStateException("Invalid configuration")
                } else {
                    awaitCallback<ImportRoomKeysResult> {
                        restoreKeysWithRecoveryKey(keysBackupVersion, recoveryKey, roomId, sessionId, stepProgressListener, it)
                    }
                }
            }.foldToCallback(object : MatrixCallback<ImportRoomKeysResult> {
                override fun onSuccess(data: ImportRoomKeysResult) {
                    uiHandler.post {
                        callback.onSuccess(data)
                    }
                }

                override fun onFailure(failure: Throwable) {
                    uiHandler.post {
                        callback.onFailure(failure)
                    }
                }
            })
        }
    }

    /**
     * Same method as [RoomKeysRestClient.getRoomKey] except that it accepts nullable
     * parameters and always returns a KeysBackupData object through the Callback.
     */
    private suspend fun getKeys(
            sessionId: String?,
            roomId: String?,
            version: String
    ): KeysBackupData {
        return if (roomId != null && sessionId != null) {
            // Get key for the room and for the session
            val data = getRoomSessionDataTask.execute(GetRoomSessionDataTask.Params(roomId, sessionId, version))
            // Convert to KeysBackupData
            KeysBackupData(
                    mutableMapOf(
                            roomId to RoomKeysBackupData(
                                    mutableMapOf(
                                            sessionId to data
                                    )
                            )
                    )
            )
        } else if (roomId != null) {
            // Get all keys for the room
            val data = withContext(coroutineDispatchers.io) {
                getRoomSessionsDataTask.execute(GetRoomSessionsDataTask.Params(roomId, version))
            }
            // Convert to KeysBackupData
            KeysBackupData(mutableMapOf(roomId to data))
        } else {
            // Get all keys
            withContext(coroutineDispatchers.io) {
                getSessionsDataTask.execute(GetSessionsDataTask.Params(version))
            }
        }
    }

    /**
     * Do a backup if there are new keys, with a delay.
     */
    fun maybeBackupKeys() {
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

                cryptoCoroutineScope.launch {
                    delay(delayInMs)
                    uiHandler.post { backupKeys() }
                }
            }
            else -> {
                Timber.v("maybeBackupKeys: Skip it because state: ${getState()}")
            }
        }
    }

    override fun getVersion(
            version: String,
            callback: MatrixCallback<KeysVersionResult?>
    ) {
        getKeysBackupVersionTask
                .configureWith(version) {
                    this.callback = object : MatrixCallback<KeysVersionResult> {
                        override fun onSuccess(data: KeysVersionResult) {
                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            if (failure is Failure.ServerError &&
                                    failure.error.code == MatrixError.M_NOT_FOUND) {
                                // Workaround because the homeserver currently returns M_NOT_FOUND when there is no key backup
                                callback.onSuccess(null)
                            } else {
                                // Transmit the error
                                callback.onFailure(failure)
                            }
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun getCurrentVersion(callback: MatrixCallback<KeysBackupLastVersionResult>) {
        getKeysBackupLastVersionTask
                .configureWith {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun forceUsingLastVersion(callback: MatrixCallback<Boolean>) {
        getCurrentVersion(object : MatrixCallback<KeysBackupLastVersionResult> {
            override fun onSuccess(data: KeysBackupLastVersionResult) {
                val localBackupVersion = keysBackupVersion?.version
                when (data) {
                    KeysBackupLastVersionResult.NoKeysBackup -> {
                        if (localBackupVersion == null) {
                            // No backup on the server, and backup is not active
                            callback.onSuccess(true)
                        } else {
                            // No backup on the server, and we are currently backing up, so stop backing up
                            callback.onSuccess(false)
                            resetKeysBackupData()
                            keysBackupVersion = null
                            keysBackupStateManager.state = KeysBackupState.Disabled
                        }
                    }
                    is KeysBackupLastVersionResult.KeysBackup -> {
                        if (localBackupVersion == null) {
                            // backup on the server, and backup is not active
                            callback.onSuccess(false)
                            // Do a check
                            checkAndStartWithKeysBackupVersion(data.keysVersionResult)
                        } else {
                            // Backup on the server, and we are currently backing up, compare version
                            if (localBackupVersion == data.keysVersionResult.version) {
                                // We are already using the last version of the backup
                                callback.onSuccess(true)
                            } else {
                                // We are not using the last version, so delete the current version we are using on the server
                                callback.onSuccess(false)

                                // This will automatically check for the last version then
                                deleteBackup(localBackupVersion, null)
                            }
                        }
                    }
                }
            }

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
    }

    override fun checkAndStartKeysBackup() {
        if (!isStuck()) {
            // Try to start or restart the backup only if it is in unknown or bad state
            Timber.w("checkAndStartKeysBackup: invalid state: ${getState()}")

            return
        }

        keysBackupVersion = null
        keysBackupStateManager.state = KeysBackupState.CheckingBackUpOnHomeserver

        getCurrentVersion(object : MatrixCallback<KeysBackupLastVersionResult> {
            override fun onSuccess(data: KeysBackupLastVersionResult) {
                checkAndStartWithKeysBackupVersion(data.toKeysVersionResult())
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
                    val versionInStore = cryptoStore.getKeyBackupVersion()

                    if (data.usable) {
                        Timber.v("checkAndStartWithKeysBackupVersion: Found usable key backup. version: ${keyBackupVersion.version}")
                        // Check the version we used at the previous app run
                        if (versionInStore != null && versionInStore != keyBackupVersion.version) {
                            Timber.v(" -> clean the previously used version $versionInStore")
                            resetKeysBackupData()
                        }

                        Timber.v("   -> enabling key backups")
                        enableKeysBackup(keyBackupVersion)
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

/* ==========================================================================================
 * Private
 * ========================================================================================== */

    /**
     * Extract MegolmBackupAuthData data from a backup version.
     *
     * @return the authentication if found and valid, null in other case
     */
    private fun KeysVersionResult.getValidAuthData(): MegolmBackupAuthData? {
        return getAuthDataAsMegolmBackupAuthData()
                ?.takeIf { it.isValid() }
    }

    /**
     * Compute the recovery key from a password and key backup version.
     *
     * @param password the password.
     * @param keysBackupData the backup and its auth data.
     * @param progressListener listener to track progress
     *
     * @return the recovery key if successful, null in other cases
     */
    @WorkerThread
    private fun recoveryKeyFromPassword(password: String, keysBackupData: KeysVersionResult, progressListener: ProgressListener?): String? {
        val authData = keysBackupData.getValidAuthData()

        if (authData == null) {
            Timber.w("recoveryKeyFromPassword: invalid parameter")
            return null
        }

        if (authData.privateKeySalt.isNullOrBlank() ||
                authData.privateKeyIterations == null) {
            Timber.w("recoveryKeyFromPassword: Salt and/or iterations not found in key backup auth data")

            return null
        }

        val salt = authData.privateKeySalt!!
        val iterations = authData.privateKeyIterations!!
        // Extract the recovery key from the passphrase
        val data = retrievePrivateKeyWithPassword(password, salt, iterations, progressListener)

        return computeRecoveryKey(data)
    }

    /**
     * Check if a recovery key matches key backup authentication data.
     *
     * @param recoveryKey the recovery key to challenge.
     * @param keysBackupData the backup and its auth data.
     *
     * @return true if successful.
     */
    @WorkerThread
    private fun isValidRecoveryKeyForKeysBackupVersion(recoveryKey: String, keysBackupData: KeysVersionResult): Boolean {
        return try {
            if (!keyBackupConfig.isAlgorithmSupported(keysBackupData.algorithm)) {
                Timber.w("isValidRecoveryKeyForKeysBackupVersion: unsupported algorithm ${keysBackupData.algorithm}")
                return false
            }
            val algorithm = algorithmFactory.create(keysBackupData)
            val privateKey = extractCurveKeyFromRecoveryKey(recoveryKey) ?: return false
            val isValid = algorithm.keyMatches(privateKey)
            algorithm.release()
            isValid
        } catch (failure: Throwable) {
            Timber.e(failure, "Can't check validity of recoveryKey")
            false
        }
    }

    override fun isValidRecoveryKeyForCurrentVersion(recoveryKey: String, callback: MatrixCallback<Boolean>) {
        val safeKeysBackupVersion = keysBackupVersion ?: return Unit.also { callback.onSuccess(false) }

        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            isValidRecoveryKeyForKeysBackupVersion(recoveryKey, safeKeysBackupVersion).let {
                callback.onSuccess(it)
            }
        }
    }

    override fun computePrivateKey(
            passphrase: String,
            privateKeySalt: String,
            privateKeyIterations: Int,
            progressListener: ProgressListener
    ): ByteArray {
        return deriveKey(passphrase, privateKeySalt, privateKeyIterations, progressListener)
    }

    /**
     * Enable backing up of keys.
     * This method will update the state and will start sending keys in nominal case
     *
     * @param keysVersionResult backup information object as returned by [getCurrentVersion].
     */
    private fun enableKeysBackup(keysVersionResult: KeysVersionResult) {
        val retrievedMegolmBackupAuthData = keysVersionResult.getAuthDataAsMegolmBackupAuthData()
        if (!keyBackupConfig.isAlgorithmSupported(keysVersionResult.algorithm)) {
            Timber.w("enableKeysBackup: unsupported algorithm ${keysVersionResult.algorithm}")
            keysBackupStateManager.state = KeysBackupState.Disabled
            return
        }
        if (retrievedMegolmBackupAuthData != null) {
            keysBackupVersion = keysVersionResult
            cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                cryptoStore.setKeyBackupVersion(keysVersionResult.version)
            }

            onServerDataRetrieved(keysVersionResult.count, keysVersionResult.hash)

            try {
                algorithm = algorithmFactory.create(keysVersionResult)
            } catch (e: Exception) {
                Timber.e(e, "Error while creating algorithm")
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
     * Update the DB with data fetch from the server.
     */
    private fun onServerDataRetrieved(count: Int?, etag: String?) {
        cryptoStore.setKeysBackupData(KeysBackupDataEntity()
                .apply {
                    backupLastServerNumberOfKeys = count
                    backupLastServerHash = etag
                }
        )
    }

    /**
     * Reset all local key backup data.
     *
     * Note: This method does not update the state
     */
    private fun resetKeysBackupData() {
        resetBackupAllGroupSessionsListeners()

        cryptoStore.setKeyBackupVersion(null)
        cryptoStore.setKeysBackupData(null)
        algorithm?.release()
        algorithm = null

        // Reset backup markers
        cryptoStore.resetBackupMarkers()
    }

    /**
     * Send a chunk of keys to backup.
     */
    @UiThread
    private fun backupKeys() {
        Timber.v("backupKeys")

        // Sanity check, as this method can be called after a delay, the state may have change during the delay
        if (!isEnabled() || algorithm == null || keysBackupVersion == null) {
            Timber.v("backupKeys: Invalid configuration")
            backupAllGroupSessionsCallback?.onFailure(IllegalStateException("Invalid configuration"))
            resetBackupAllGroupSessionsListeners()
            return
        }

        if (getState() === KeysBackupState.BackingUp) {
            // Do nothing if we are already backing up
            Timber.v("backupKeys: Invalid state: ${getState()}")
            return
        }
        val recoveryKey = cryptoStore.getKeyBackupRecoveryKeyInfo()?.recoveryKey
        extractCurveKeyFromRecoveryKey(recoveryKey)?.also { privateKey ->
            // symmetric algorithm need private key to backup :/
            // a bit hugly, but all this code needs refactoring
            algorithm?.setPrivateKey(privateKey)
        }

        // Get a chunk of keys to backup
        val olmInboundGroupSessionWrappers = cryptoStore.inboundGroupSessionsToBackup(KEY_BACKUP_SEND_KEYS_MAX_COUNT)

        Timber.v("backupKeys: 1 - ${olmInboundGroupSessionWrappers.size} sessions to back up")

        if (olmInboundGroupSessionWrappers.isEmpty()) {
            // Backup is up to date
            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp

            backupAllGroupSessionsCallback?.onSuccess(Unit)
            resetBackupAllGroupSessionsListeners()
            return
        }

        keysBackupStateManager.state = KeysBackupState.BackingUp

        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                Timber.v("backupKeys: 2 - Encrypting keys")

                // Gather data to send to the homeserver
                // roomId -> sessionId -> MXKeyBackupData
                val keysBackupData = KeysBackupData()
                val recoveryKey = cryptoStore.getKeyBackupRecoveryKeyInfo()?.recoveryKey
                extractCurveKeyFromRecoveryKey(recoveryKey)?.also { privateKey ->
                    algorithm?.setPrivateKey(privateKey)
                }
                olmInboundGroupSessionWrappers.forEach { olmInboundGroupSessionWrapper ->
                    val roomId = olmInboundGroupSessionWrapper.roomId ?: return@forEach
                    val olmInboundGroupSession = olmInboundGroupSessionWrapper.session

                    try {
                        encryptGroupSession(olmInboundGroupSessionWrapper)
                                ?.let {
                                    keysBackupData.roomIdToRoomKeysBackupData
                                            .getOrPut(roomId) { RoomKeysBackupData() }
                                            .sessionIdToKeyBackupData[olmInboundGroupSession.sessionIdentifier()] = it
                                }
                    } catch (e: OlmException) {
                        Timber.e(e, "OlmException")
                    }
                }

                Timber.v("backupKeys: 4 - Sending request")

                // Make the request
                val version = keysBackupVersion?.version ?: return@withContext

                storeSessionDataTask
                        .configureWith(StoreSessionsDataTask.Params(version, keysBackupData)) {
                            this.callback = object : MatrixCallback<BackupKeysResult> {
                                override fun onSuccess(data: BackupKeysResult) {
                                    uiHandler.post {
                                        Timber.v("backupKeys: 5a - Request complete")

                                        // Mark keys as backed up
                                        cryptoStore.markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers)
                                        // we can release the sessions now
                                        olmInboundGroupSessionWrappers.onEach { it.session.releaseSession() }

                                        if (olmInboundGroupSessionWrappers.size < KEY_BACKUP_SEND_KEYS_MAX_COUNT) {
                                            Timber.v("backupKeys: All keys have been backed up")
                                            onServerDataRetrieved(data.count, data.hash)

                                            // Note: Changing state will trigger the call to backupAllGroupSessionsCallback.onSuccess()
                                            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                        } else {
                                            Timber.v("backupKeys: Continue to back up keys")
                                            keysBackupStateManager.state = KeysBackupState.WillBackUp

                                            backupKeys()
                                        }
                                    }
                                }

                                override fun onFailure(failure: Throwable) {
                                    if (failure is Failure.ServerError) {
                                        uiHandler.post {
                                            Timber.e(failure, "backupKeys: backupKeys failed.")

                                            when (failure.error.code) {
                                                MatrixError.M_NOT_FOUND,
                                                MatrixError.M_WRONG_ROOM_KEYS_VERSION -> {
                                                    // Backup has been deleted on the server, or we are not using the last backup version
                                                    keysBackupStateManager.state = KeysBackupState.WrongBackUpVersion
                                                    backupAllGroupSessionsCallback?.onFailure(failure)
                                                    resetBackupAllGroupSessionsListeners()
                                                    resetKeysBackupData()
                                                    keysBackupVersion = null

                                                    // Do not stay in KeysBackupState.WrongBackUpVersion but check what is available on the homeserver
                                                    checkAndStartKeysBackup()
                                                }
                                                else ->
                                                    // Come back to the ready state so that we will retry on the next received key
                                                    keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                            }
                                        }
                                    } else {
                                        uiHandler.post {
                                            backupAllGroupSessionsCallback?.onFailure(failure)
                                            resetBackupAllGroupSessionsListeners()

                                            Timber.e("backupKeys: backupKeys failed.")

                                            // Retry a bit later
                                            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                            maybeBackupKeys()
                                        }
                                    }
                                }
                            }
                        }
                        .executeBy(taskExecutor)
            }
        }
    }

    @VisibleForTesting
    @WorkerThread
    suspend fun encryptGroupSession(olmInboundGroupSessionWrapper: MXInboundMegolmSessionWrapper): KeyBackupData? {
        olmInboundGroupSessionWrapper.safeSessionId ?: return null
        olmInboundGroupSessionWrapper.senderKey ?: return null
        // Gather information for each key
        val device = cryptoStore.deviceWithIdentityKey(olmInboundGroupSessionWrapper.senderKey)

        val sessionData = inboundGroupSessionStore
                .getInboundGroupSession(olmInboundGroupSessionWrapper.safeSessionId, olmInboundGroupSessionWrapper.senderKey)
                ?.let {
                    withContext(coroutineDispatchers.computation) {
                        it.mutex.withLock { it.wrapper.exportKeys() }
                    }
                }
                ?: return null

        val sessionBackupData = tryOrNull {
            algorithm?.encryptSession(sessionData)
        } ?: return null
        return KeyBackupData(
                firstMessageIndex = try {
                    olmInboundGroupSessionWrapper.session.firstKnownIndex
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                    0L
                },
                forwardedCount = olmInboundGroupSessionWrapper.sessionData.forwardingCurve25519KeyChain.orEmpty().size,
                isVerified = device?.isVerified == true,
                sharedHistory = olmInboundGroupSessionWrapper.getSharedKey(),
                sessionData = sessionBackupData
        )
    }

    /**
     * Returns boolean shared key flag, if enabled with respect to matrix configuration.
     */
    private fun MXInboundMegolmSessionWrapper.getSharedKey(): Boolean {
        if (!cryptoStore.isShareKeysOnInviteEnabled()) return false
        return sessionData.sharedHistory
    }

    private fun getPrivateKey(): ByteArray {
        return byteArrayOf()
    }

    /* ==========================================================================================
     * For test only
     * ========================================================================================== */

    // Direct access for test only
    @VisibleForTesting
    val store
        get() = cryptoStore

    @VisibleForTesting
    fun createFakeKeysBackupVersion(
            keysBackupCreationInfo: MegolmBackupCreationInfo,
            callback: MatrixCallback<KeysVersion>
    ) {
        @Suppress("UNCHECKED_CAST")
        val createKeysBackupVersionBody = CreateKeysBackupVersionBody(
                algorithm = keysBackupCreationInfo.algorithm,
                authData = keysBackupCreationInfo.authData.toJsonDict()
        )

        createKeysBackupVersionTask
                .configureWith(createKeysBackupVersionBody) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        return cryptoStore.getKeyBackupRecoveryKeyInfo()
    }

    override fun saveBackupRecoveryKey(recoveryKey: String?, version: String?) {
        cryptoStore.saveBackupRecoveryKey(recoveryKey, version)
    }

    companion object {
        // Maximum delay in ms in {@link maybeBackupKeys}
        private const val KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS = 10_000L

        // Maximum number of keys to send at a time to the homeserver.
        private const val KEY_BACKUP_SEND_KEYS_MAX_COUNT = 100
    }

/* ==========================================================================================
 * DEBUG INFO
 * ========================================================================================== */

    override fun toString() = "KeysBackup for $userId"
}

internal fun MegolmBackupAuthData.toSignalableJsonDict(): JsonDict {
    return HashMap(toJsonDict()).apply {
        remove("signatures")
    }
}
