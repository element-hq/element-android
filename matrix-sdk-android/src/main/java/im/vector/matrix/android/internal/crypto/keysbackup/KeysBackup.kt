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
import android.os.Looper
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import arrow.core.Try
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.listeners.StepProgressListener
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupStateListener
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.MegolmSessionData
import im.vector.matrix.android.internal.crypto.ObjectSigner
import im.vector.matrix.android.internal.crypto.actions.MegolmSessionDataImporter
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrustSignature
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupAuthData
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.*
import im.vector.matrix.android.internal.crypto.keysbackup.tasks.*
import im.vector.matrix.android.internal.crypto.keysbackup.util.computeRecoveryKey
import im.vector.matrix.android.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.OlmInboundGroupSessionWrapper
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.extensions.foldToCallback
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.olm.OlmException
import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption
import org.matrix.olm.OlmPkMessage
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * A KeysBackup class instance manage incremental backup of e2e keys (megolm keys)
 * to the user's homeserver.
 */

@SessionScope
internal class KeysBackup @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val olmDevice: MXOlmDevice,
        private val objectSigner: ObjectSigner,
        // Actions
        private val megolmSessionDataImporter: MegolmSessionDataImporter,
        // Tasks
        private val createKeysBackupVersionTask: CreateKeysBackupVersionTask,
        private val deleteBackupTask: DeleteBackupTask,
        private val deleteRoomSessionDataTask: DeleteRoomSessionDataTask,
        private val deleteRoomSessionsDataTask: DeleteRoomSessionsDataTask,
        private val deleteSessionDataTask: DeleteSessionsDataTask,
        private val getKeysBackupLastVersionTask: GetKeysBackupLastVersionTask,
        private val getKeysBackupVersionTask: GetKeysBackupVersionTask,
        private val getRoomSessionDataTask: GetRoomSessionDataTask,
        private val getRoomSessionsDataTask: GetRoomSessionsDataTask,
        private val getSessionsDataTask: GetSessionsDataTask,
        private val storeRoomSessionDataTask: StoreRoomSessionDataTask,
        private val storeSessionsDataTask: StoreRoomSessionsDataTask,
        private val storeSessionDataTask: StoreSessionsDataTask,
        private val updateKeysBackupVersionTask: UpdateKeysBackupVersionTask,
        // Task executor
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : KeysBackupService {

    private val uiHandler = Handler(Looper.getMainLooper())

    private val keysBackupStateManager = KeysBackupStateManager(uiHandler)

    // The backup version
    override var keysBackupVersion: KeysVersionResult? = null
        private set

    // The backup key being used.
    private var backupOlmPkEncryption: OlmPkEncryption? = null

    private val random = Random()

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
        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                Try {
                    val olmPkDecryption = OlmPkDecryption()
                    val megolmBackupAuthData = MegolmBackupAuthData()

                    if (password != null) {
                        // Generate a private key from the password
                        val backgroundProgressListener = if (progressListener == null) {
                            null
                        } else {
                            object : ProgressListener {
                                override fun onProgress(progress: Int, total: Int) {
                                    uiHandler.post {
                                        try {
                                            progressListener.onProgress(progress, total)
                                        } catch (e: Exception) {
                                            Timber.e(e, "prepareKeysBackupVersion: onProgress failure")
                                        }
                                    }
                                }
                            }
                        }

                        val generatePrivateKeyResult = generatePrivateKeyWithPassword(password, backgroundProgressListener)
                        megolmBackupAuthData.publicKey = olmPkDecryption.setPrivateKey(generatePrivateKeyResult.privateKey)
                        megolmBackupAuthData.privateKeySalt = generatePrivateKeyResult.salt
                        megolmBackupAuthData.privateKeyIterations = generatePrivateKeyResult.iterations
                    } else {
                        val publicKey = olmPkDecryption.generateKey()

                        megolmBackupAuthData.publicKey = publicKey
                    }

                    val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, megolmBackupAuthData.signalableJSONDictionary())

                    megolmBackupAuthData.signatures = objectSigner.signObject(canonicalJson)


                    val megolmBackupCreationInfo = MegolmBackupCreationInfo()
                    megolmBackupCreationInfo.algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
                    megolmBackupCreationInfo.authData = megolmBackupAuthData
                    megolmBackupCreationInfo.recoveryKey = computeRecoveryKey(olmPkDecryption.privateKey())

                    megolmBackupCreationInfo
                }
            }.foldToCallback(callback)
        }
    }

    override fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo,
                                         callback: MatrixCallback<KeysVersion>) {
        val createKeysBackupVersionBody = CreateKeysBackupVersionBody()
        createKeysBackupVersionBody.algorithm = keysBackupCreationInfo.algorithm
        createKeysBackupVersionBody.authData = MoshiProvider.providesMoshi().adapter(Map::class.java)
                .fromJson(keysBackupCreationInfo.authData?.toJsonString()) as Map<String, Any>?

        keysBackupStateManager.state = KeysBackupState.Enabling

        createKeysBackupVersionTask
                .configureWith(createKeysBackupVersionBody) {
                    this.callback = object : MatrixCallback<KeysVersion> {
                        override fun onSuccess(info: KeysVersion) {
                            // Reset backup markers.
                            cryptoStore.resetBackupMarkers()

                            val keyBackupVersion = KeysVersionResult()
                            keyBackupVersion.algorithm = createKeysBackupVersionBody.algorithm
                            keyBackupVersion.authData = createKeysBackupVersionBody.authData
                            keyBackupVersion.version = info.version

                            // We can consider that the server does not have keys yet
                            keyBackupVersion.count = 0
                            keyBackupVersion.hash = null

                            enableKeysBackup(keyBackupVersion)

                            callback.onSuccess(info)
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
        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                // If we're currently backing up to this backup... stop.
                // (We start using it automatically in createKeysBackupVersion so this is symmetrical).
                if (keysBackupVersion != null && version == keysBackupVersion!!.version) {
                    resetKeysBackupData()
                    keysBackupVersion = null
                    keysBackupStateManager.state = KeysBackupState.Unknown
                }

                deleteBackupTask
                        .configureWith(DeleteBackupTask.Params(version)) {
                            this.callback = object : MatrixCallback<Unit> {
                                private fun eventuallyRestartBackup() {
                                    // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                                    if (state == KeysBackupState.Unknown) {
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
    }

    override fun canRestoreKeys(): Boolean {
        // Server contains more keys than locally
        val totalNumberOfKeysLocally = getTotalNumbersOfKeys()

        val keysBackupData = cryptoStore.getKeysBackupData()

        val totalNumberOfKeysServer = keysBackupData?.backupLastServerNumberOfKeys ?: -1
        val hashServer = keysBackupData?.backupLastServerHash

        return when {
            totalNumberOfKeysLocally < totalNumberOfKeysServer  -> {
                // Server contains more keys than this device
                true
            }
            totalNumberOfKeysLocally == totalNumberOfKeysServer -> {
                // Same number, compare hash?
                // TODO We have not found any algorithm to determine if a restore is recommended here. Return false for the moment
                false
            }
            else                                                -> false
        }
    }

    override fun getTotalNumbersOfKeys(): Int {
        return cryptoStore.inboundGroupSessionsCount(false)

    }

    override fun getTotalNumbersOfBackedUpKeys(): Int {
        return cryptoStore.inboundGroupSessionsCount(true)
    }

    override fun backupAllGroupSessions(progressListener: ProgressListener?,
                                        callback: MatrixCallback<Unit>?) {
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
                                if (state === KeysBackupState.ReadyToBackUp) {
                                    backupAllGroupSessionsCallback?.onSuccess(Unit)
                                    resetBackupAllGroupSessionsListeners()
                                }
                            }
                        })
                    }
                }

                keysBackupStateManager.addListener(keysBackupStateListener!!)

                backupKeys()
            }
        })
    }

    override fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult,
                                    callback: MatrixCallback<KeysBackupVersionTrust>) {
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
        val myUserId = credentials.userId

        val keysBackupVersionTrust = KeysBackupVersionTrust()
        val authData = keysBackupVersion.getAuthDataAsMegolmBackupAuthData()

        if (keysBackupVersion.algorithm == null
                || authData == null
                || authData.publicKey.isEmpty()
                || authData.signatures.isNullOrEmpty()) {
            Timber.v("getKeysBackupTrust: Key backup is absent or missing required data")
            return keysBackupVersionTrust
        }

        val mySigs = authData.signatures?.get(myUserId)
        if (mySigs.isNullOrEmpty()) {
            Timber.v("getKeysBackupTrust: Ignoring key backup because it lacks any signatures from this user")
            return keysBackupVersionTrust
        }

        for (keyId in mySigs.keys) {
            // XXX: is this how we're supposed to get the device id?
            var deviceId: String? = null
            val components = keyId.split(":")
            if (components.size == 2) {
                deviceId = components[1]
            }

            if (deviceId != null) {
                val device = cryptoStore.getUserDevice(deviceId, myUserId)
                var isSignatureValid = false

                if (device == null) {
                    Timber.v("getKeysBackupTrust: Signature from unknown device $deviceId")
                } else {
                    val fingerprint = device.fingerprint()
                    if (fingerprint != null) {
                        try {
                            olmDevice.verifySignature(fingerprint, authData.signalableJSONDictionary(), mySigs[keyId] as String)
                            isSignatureValid = true
                        } catch (e: OlmException) {
                            Timber.v("getKeysBackupTrust: Bad signature from device " + device.deviceId + " " + e.localizedMessage)
                        }
                    }

                    if (isSignatureValid && device.isVerified) {
                        keysBackupVersionTrust.usable = true
                    }
                }

                val signature = KeysBackupVersionTrustSignature()
                signature.device = device
                signature.valid = isSignatureValid
                signature.deviceId = deviceId
                keysBackupVersionTrust.signatures.add(signature)
            }
        }

        return keysBackupVersionTrust
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
            GlobalScope.launch(coroutineDispatchers.main) {
                val updateKeysBackupVersionBody = withContext(coroutineDispatchers.crypto) {
                    val myUserId = credentials.userId

                    // Get current signatures, or create an empty set
                    val myUserSignatures = authData.signatures?.get(myUserId)?.toMutableMap()
                            ?: HashMap()

                    if (trust) {
                        // Add current device signature
                        val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, authData.signalableJSONDictionary())

                        val deviceSignatures = objectSigner.signObject(canonicalJson)

                        deviceSignatures[myUserId]?.forEach { entry ->
                            myUserSignatures[entry.key] = entry.value
                        }
                    } else {
                        // Remove current device signature
                        myUserSignatures.remove("ed25519:${credentials.deviceId}")
                    }

                    // Create an updated version of KeysVersionResult
                    val updateKeysBackupVersionBody = UpdateKeysBackupVersionBody(keysBackupVersion.version!!)

                    updateKeysBackupVersionBody.algorithm = keysBackupVersion.algorithm

                    val newMegolmBackupAuthData = authData.copy()

                    val newSignatures = newMegolmBackupAuthData.signatures!!.toMutableMap()
                    newSignatures[myUserId] = myUserSignatures

                    newMegolmBackupAuthData.signatures = newSignatures

                    val moshi = MoshiProvider.providesMoshi()
                    val adapter = moshi.adapter(Map::class.java)

                    updateKeysBackupVersionBody.authData = adapter.fromJson(newMegolmBackupAuthData.toJsonString()) as Map<String, Any>?

                    updateKeysBackupVersionBody
                }

                // And send it to the homeserver
                updateKeysBackupVersionTask
                        .configureWith(UpdateKeysBackupVersionTask.Params(keysBackupVersion.version!!, updateKeysBackupVersionBody)) {
                            this.callback = object : MatrixCallback<Unit> {
                                override fun onSuccess(data: Unit) {
                                    // Relaunch the state machine on this updated backup version
                                    val newKeysBackupVersion = KeysVersionResult()

                                    newKeysBackupVersion.version = keysBackupVersion.version
                                    newKeysBackupVersion.algorithm = keysBackupVersion.algorithm
                                    newKeysBackupVersion.count = keysBackupVersion.count
                                    newKeysBackupVersion.hash = keysBackupVersion.hash
                                    newKeysBackupVersion.authData = updateKeysBackupVersionBody.authData

                                    checkAndStartWithKeysBackupVersion(newKeysBackupVersion)

                                    callback.onSuccess(data)
                                }

                                override fun onFailure(failure: Throwable) {
                                    callback.onFailure(failure)
                                }
                            }
                        }
                        .executeBy(taskExecutor)
            }
        }
    }

    override fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult,
                                                       recoveryKey: String,
                                                       callback: MatrixCallback<Unit>) {
        Timber.v("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")

        GlobalScope.launch(coroutineDispatchers.main) {
            val isValid = withContext(coroutineDispatchers.crypto) {
                isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysBackupVersion)
            }

            if (!isValid) {
                Timber.w("trustKeyBackupVersionWithRecoveryKey: Invalid recovery key.")

                callback.onFailure(IllegalArgumentException("Invalid recovery key or password"))
            } else {
                trustKeysBackupVersion(keysBackupVersion, true, callback)
            }
        }
    }

    override fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult,
                                                      password: String,
                                                      callback: MatrixCallback<Unit>) {
        Timber.v("trustKeysBackupVersionWithPassphrase: version ${keysBackupVersion.version}")

        GlobalScope.launch(coroutineDispatchers.main) {
            val recoveryKey = withContext(coroutineDispatchers.crypto) {
                recoveryKeyFromPassword(password, keysBackupVersion, null)
            }

            if (recoveryKey == null) {
                Timber.w("trustKeysBackupVersionWithPassphrase: Key backup is missing required data")

                callback.onFailure(IllegalArgumentException("Missing element"))
            } else {
                // Check trust using the recovery key
                trustKeysBackupVersionWithRecoveryKey(keysBackupVersion, recoveryKey, callback)
            }
        }
    }

    /**
     * Get public key from a Recovery key
     *
     * @param recoveryKey the recovery key
     * @return the corresponding public key, from Olm
     */
    @WorkerThread
    private fun pkPublicKeyFromRecoveryKey(recoveryKey: String): String? {
        // Extract the primary key
        val privateKey = extractCurveKeyFromRecoveryKey(recoveryKey)

        if (privateKey == null) {
            Timber.w("pkPublicKeyFromRecoveryKey: private key is null")

            return null
        }

        // Built the PK decryption with it
        val pkPublicKey: String

        try {
            val decryption = OlmPkDecryption()
            pkPublicKey = decryption.setPrivateKey(privateKey)
        } catch (e: OlmException) {
            return null
        }

        return pkPublicKey
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

    override fun restoreKeysWithRecoveryKey(keysVersionResult: KeysVersionResult,
                                            recoveryKey: String,
                                            roomId: String?,
                                            sessionId: String?,
                                            stepProgressListener: StepProgressListener?,
                                            callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.v("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version}")

        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                Try {
                    // Check if the recovery is valid before going any further
                    if (!isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysVersionResult)) {
                        Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key for this keys version")
                        throw InvalidParameterException("Invalid recovery key")
                    }

                    // Get a PK decryption instance
                    val decryption = pkDecryptionFromRecoveryKey(recoveryKey)
                    if (decryption == null) {
                        // This should not happen anymore
                        Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key. Error")
                        throw InvalidParameterException("Invalid recovery key")
                    }

                    decryption!!
                }
            }.fold(
                    {
                        callback.onFailure(it)
                    },
                    { decryption ->
                        stepProgressListener?.onStepProgress(StepProgressListener.Step.DownloadingKey)

                        // Get backed up keys from the homeserver
                        getKeys(sessionId, roomId, keysVersionResult.version!!, object : MatrixCallback<KeysBackupData> {
                            override fun onSuccess(data: KeysBackupData) {
                                GlobalScope.launch(coroutineDispatchers.main) {
                                    val importRoomKeysResult = withContext(coroutineDispatchers.crypto) {
                                        val sessionsData = ArrayList<MegolmSessionData>()
                                        // Restore that data
                                        var sessionsFromHsCount = 0
                                        for (roomIdLoop in data.roomIdToRoomKeysBackupData.keys) {
                                            for (sessionIdLoop in data.roomIdToRoomKeysBackupData[roomIdLoop]!!.sessionIdToKeyBackupData.keys) {
                                                sessionsFromHsCount++

                                                val keyBackupData = data.roomIdToRoomKeysBackupData[roomIdLoop]!!.sessionIdToKeyBackupData[sessionIdLoop]!!

                                                val sessionData = decryptKeyBackupData(keyBackupData, sessionIdLoop, roomIdLoop, decryption)

                                                sessionData?.let {
                                                    sessionsData.add(it)
                                                }
                                            }
                                        }
                                        Timber.v("restoreKeysWithRecoveryKey: Decrypted " + sessionsData.size + " keys out of "
                                                + sessionsFromHsCount + " from the backup store on the homeserver")

                                        // Do not trigger a backup for them if they come from the backup version we are using
                                        val backUp = keysVersionResult.version != keysBackupVersion?.version
                                        if (backUp) {
                                            Timber.v("restoreKeysWithRecoveryKey: Those keys will be backed up to backup version: "
                                                    + keysBackupVersion?.version)
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

                                        val result = megolmSessionDataImporter.handle(sessionsData, !backUp, uiHandler, progressListener)

                                        // Do not back up the key if it comes from a backup recovery
                                        if (backUp) {
                                            maybeBackupKeys()
                                        }

                                        result
                                    }

                                    callback.onSuccess(importRoomKeysResult)
                                }
                            }

                            override fun onFailure(failure: Throwable) {
                                callback.onFailure(failure)
                            }
                        })
                    }
            )
        }
    }

    override fun restoreKeyBackupWithPassword(keysBackupVersion: KeysVersionResult,
                                              password: String,
                                              roomId: String?,
                                              sessionId: String?,
                                              stepProgressListener: StepProgressListener?,
                                              callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.v("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")

        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
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

                Try {
                    recoveryKeyFromPassword(password, keysBackupVersion, progressListener)
                }
            }.fold(
                    {
                        callback.onFailure(it)
                    },
                    { recoveryKey ->
                        if (recoveryKey == null) {
                            Timber.v("backupKeys: Invalid configuration")
                            callback.onFailure(IllegalStateException("Invalid configuration"))
                        } else {
                            restoreKeysWithRecoveryKey(keysBackupVersion, recoveryKey, roomId, sessionId, stepProgressListener, callback)
                        }
                    }
            )
        }
    }

    /**
     * Same method as [RoomKeysRestClient.getRoomKey] except that it accepts nullable
     * parameters and always returns a KeysBackupData object through the Callback
     */
    private fun getKeys(sessionId: String?,
                        roomId: String?,
                        version: String,
                        callback: MatrixCallback<KeysBackupData>) {
        if (roomId != null && sessionId != null) {
            // Get key for the room and for the session
            getRoomSessionDataTask
                    .configureWith(GetRoomSessionDataTask.Params(roomId, sessionId, version)) {
                        this.callback = object : MatrixCallback<KeyBackupData> {
                            override fun onSuccess(data: KeyBackupData) {
                                // Convert to KeysBackupData
                                val keysBackupData = KeysBackupData()
                                keysBackupData.roomIdToRoomKeysBackupData = HashMap()
                                val roomKeysBackupData = RoomKeysBackupData()
                                roomKeysBackupData.sessionIdToKeyBackupData = HashMap()
                                roomKeysBackupData.sessionIdToKeyBackupData[sessionId] = data
                                keysBackupData.roomIdToRoomKeysBackupData[roomId] = roomKeysBackupData

                                callback.onSuccess(keysBackupData)
                            }

                            override fun onFailure(failure: Throwable) {
                                callback.onFailure(failure)
                            }
                        }
                    }
                    .executeBy(taskExecutor)
        } else if (roomId != null) {
            // Get all keys for the room
            getRoomSessionsDataTask
                    .configureWith(GetRoomSessionsDataTask.Params(roomId, version)) {
                        this.callback = object : MatrixCallback<RoomKeysBackupData> {
                            override fun onSuccess(data: RoomKeysBackupData) {
                                // Convert to KeysBackupData
                                val keysBackupData = KeysBackupData()
                                keysBackupData.roomIdToRoomKeysBackupData = HashMap()
                                keysBackupData.roomIdToRoomKeysBackupData[roomId] = data

                                callback.onSuccess(keysBackupData)
                            }

                            override fun onFailure(failure: Throwable) {
                                callback.onFailure(failure)
                            }
                        }
                    }
                    .executeBy(taskExecutor)
        } else {
            // Get all keys
            getSessionsDataTask
                    .configureWith(GetSessionsDataTask.Params(version)) {
                        this.callback = callback
                    }
                    .executeBy(taskExecutor)
        }
    }

    @VisibleForTesting
    @WorkerThread
    fun pkDecryptionFromRecoveryKey(recoveryKey: String): OlmPkDecryption? {
        // Extract the primary key
        val privateKey = extractCurveKeyFromRecoveryKey(recoveryKey)

        // Built the PK decryption with it
        var decryption: OlmPkDecryption? = null
        if (privateKey != null) {
            try {
                decryption = OlmPkDecryption()
                decryption.setPrivateKey(privateKey)
            } catch (e: OlmException) {
                Timber.e(e, "OlmException")
            }

        }

        return decryption
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
                val delayInMs = random.nextInt(KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS).toLong()

                uiHandler.postDelayed({ backupKeys() }, delayInMs)
            }
            else                                   -> {
                Timber.v("maybeBackupKeys: Skip it because state: $state")
            }
        }
    }

    override fun getVersion(version: String,
                            callback: MatrixCallback<KeysVersionResult?>) {
        getKeysBackupVersionTask
                .configureWith(version) {
                    this.callback = object : MatrixCallback<KeysVersionResult> {
                        override fun onSuccess(data: KeysVersionResult) {
                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            if (failure is Failure.ServerError
                                    && failure.error.code == MatrixError.NOT_FOUND) {
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

    override fun getCurrentVersion(callback: MatrixCallback<KeysVersionResult?>) {
        getKeysBackupLastVersionTask
                .configureWith {
                    this.callback = object : MatrixCallback<KeysVersionResult> {
                        override fun onSuccess(data: KeysVersionResult) {
                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            if (failure is Failure.ServerError
                                    && failure.error.code == MatrixError.NOT_FOUND) {
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

    override fun forceUsingLastVersion(callback: MatrixCallback<Boolean>) {
        getCurrentVersion(object : MatrixCallback<KeysVersionResult?> {
            override fun onSuccess(data: KeysVersionResult?) {
                val localBackupVersion = keysBackupVersion?.version
                val serverBackupVersion = data?.version

                if (serverBackupVersion == null) {
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
                } else {
                    if (localBackupVersion == null) {
                        // backup on the server, and backup is not active
                        callback.onSuccess(false)
                        // Do a check
                        checkAndStartWithKeysBackupVersion(data)
                    } else {
                        // Backup on the server, and we are currently backing up, compare version
                        if (localBackupVersion == serverBackupVersion) {
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

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
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
                    val versionInStore = cryptoStore.getKeyBackupVersion()

                    if (data.usable) {
                        Timber.v("checkAndStartWithKeysBackupVersion: Found usable key backup. version: " + keyBackupVersion.version)
                        // Check the version we used at the previous app run
                        if (versionInStore != null && versionInStore != keyBackupVersion.version) {
                            Timber.v(" -> clean the previously used version $versionInStore")
                            resetKeysBackupData()
                        }

                        Timber.v("   -> enabling key backups")
                        enableKeysBackup(keyBackupVersion)
                    } else {
                        Timber.v("checkAndStartWithKeysBackupVersion: No usable key backup. version: " + keyBackupVersion.version)
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
     * @param keysBackupData the key backup data
     *
     * @return the authentication if found and valid, null in other case
     */
    private fun getMegolmBackupAuthData(keysBackupData: KeysVersionResult): MegolmBackupAuthData? {
        if (keysBackupData.version.isNullOrBlank()
                || keysBackupData.algorithm != MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
                || keysBackupData.authData == null) {
            return null
        }

        val authData = keysBackupData.getAuthDataAsMegolmBackupAuthData()

        if (authData?.signatures == null || authData.publicKey.isBlank()) {
            return null
        }

        return authData
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
    private fun recoveryKeyFromPassword(password: String, keysBackupData: KeysVersionResult, progressListener: ProgressListener?): String? {
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

        // Extract the recovery key from the passphrase
        val data = retrievePrivateKeyWithPassword(password, authData.privateKeySalt!!, authData.privateKeyIterations!!, progressListener)

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
        // Build PK decryption instance with the recovery key
        val publicKey = pkPublicKeyFromRecoveryKey(recoveryKey)

        if (publicKey == null) {
            Timber.w("isValidRecoveryKeyForKeysBackupVersion: public key is null")

            return false
        }

        val authData = getMegolmBackupAuthData(keysBackupData)

        if (authData == null) {
            Timber.w("isValidRecoveryKeyForKeysBackupVersion: Key backup is missing required data")

            return false
        }

        // Compare both
        if (publicKey != authData.publicKey) {
            Timber.w("isValidRecoveryKeyForKeysBackupVersion: Public keys mismatch")

            return false
        }

        // Public keys match!
        return true
    }

    /**
     * Enable backing up of keys.
     * This method will update the state and will start sending keys in nominal case
     *
     * @param keysVersionResult backup information object as returned by [getCurrentVersion].
     */
    private fun enableKeysBackup(keysVersionResult: KeysVersionResult) {
        if (keysVersionResult.authData != null) {
            val retrievedMegolmBackupAuthData = keysVersionResult.getAuthDataAsMegolmBackupAuthData()

            if (retrievedMegolmBackupAuthData != null) {
                keysBackupVersion = keysVersionResult
                cryptoStore.setKeyBackupVersion(keysVersionResult.version)

                onServerDataRetrieved(keysVersionResult.count, keysVersionResult.hash)

                try {
                    backupOlmPkEncryption = OlmPkEncryption().apply {
                        setRecipientKey(retrievedMegolmBackupAuthData.publicKey)
                    }
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
        } else {
            Timber.e("Invalid authentication data")
            keysBackupStateManager.state = KeysBackupState.Disabled
        }
    }

    /**
     * Update the DB with data fetch from the server
     */
    private fun onServerDataRetrieved(count: Int?, hash: String?) {
        cryptoStore.setKeysBackupData(KeysBackupDataEntity()
                .apply {
                    backupLastServerNumberOfKeys = count
                    backupLastServerHash = hash
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
        backupOlmPkEncryption = null

        // Reset backup markers
        cryptoStore.resetBackupMarkers()
    }

    /**
     * Send a chunk of keys to backup
     */
    @UiThread
    private fun backupKeys() {
        Timber.v("backupKeys")

        // Sanity check, as this method can be called after a delay, the state may have change during the delay
        if (!isEnabled || backupOlmPkEncryption == null || keysBackupVersion == null) {
            Timber.v("backupKeys: Invalid configuration")
            backupAllGroupSessionsCallback?.onFailure(IllegalStateException("Invalid configuration"))
            resetBackupAllGroupSessionsListeners()

            return
        }

        if (state === KeysBackupState.BackingUp) {
            // Do nothing if we are already backing up
            Timber.v("backupKeys: Invalid state: $state")
            return
        }

        // Get a chunk of keys to backup
        val olmInboundGroupSessionWrappers = cryptoStore.inboundGroupSessionsToBackup(KEY_BACKUP_SEND_KEYS_MAX_COUNT)

        Timber.v("backupKeys: 1 - " + olmInboundGroupSessionWrappers.size + " sessions to back up")

        if (olmInboundGroupSessionWrappers.isEmpty()) {
            // Backup is up to date
            keysBackupStateManager.state = KeysBackupState.ReadyToBackUp

            backupAllGroupSessionsCallback?.onSuccess(Unit)
            resetBackupAllGroupSessionsListeners()
            return
        }

        keysBackupStateManager.state = KeysBackupState.BackingUp

        GlobalScope.launch(coroutineDispatchers.main) {
            withContext(coroutineDispatchers.crypto) {
                Timber.v("backupKeys: 2 - Encrypting keys")

                // Gather data to send to the homeserver
                // roomId -> sessionId -> MXKeyBackupData
                val keysBackupData = KeysBackupData()
                keysBackupData.roomIdToRoomKeysBackupData = HashMap()

                for (olmInboundGroupSessionWrapper in olmInboundGroupSessionWrappers) {
                    val keyBackupData = encryptGroupSession(olmInboundGroupSessionWrapper)
                    if (keysBackupData.roomIdToRoomKeysBackupData[olmInboundGroupSessionWrapper.roomId] == null) {
                        val roomKeysBackupData = RoomKeysBackupData()
                        roomKeysBackupData.sessionIdToKeyBackupData = HashMap()
                        keysBackupData.roomIdToRoomKeysBackupData[olmInboundGroupSessionWrapper.roomId!!] = roomKeysBackupData
                    }

                    try {
                        keysBackupData.roomIdToRoomKeysBackupData[olmInboundGroupSessionWrapper.roomId]!!
                                .sessionIdToKeyBackupData[olmInboundGroupSessionWrapper.olmInboundGroupSession!!.sessionIdentifier()] = keyBackupData
                    } catch (e: OlmException) {
                        Timber.e(e, "OlmException")
                    }
                }

                Timber.v("backupKeys: 4 - Sending request")

                val sendingRequestCallback = object : MatrixCallback<BackupKeysResult> {
                    override fun onSuccess(data: BackupKeysResult) {
                        uiHandler.post {
                            Timber.v("backupKeys: 5a - Request complete")

                            // Mark keys as backed up
                            cryptoStore.markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers)

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
                                    MatrixError.NOT_FOUND,
                                    MatrixError.WRONG_ROOM_KEYS_VERSION -> {
                                        // Backup has been deleted on the server, or we are not using the last backup version
                                        keysBackupStateManager.state = KeysBackupState.WrongBackUpVersion
                                        backupAllGroupSessionsCallback?.onFailure(failure)
                                        resetBackupAllGroupSessionsListeners()
                                        resetKeysBackupData()
                                        keysBackupVersion = null

                                        // Do not stay in KeysBackupState.WrongBackUpVersion but check what is available on the homeserver
                                        checkAndStartKeysBackup()
                                    }
                                    else                                ->
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

                // Make the request
                storeSessionDataTask
                        .configureWith(StoreSessionsDataTask.Params(keysBackupVersion!!.version!!, keysBackupData)){
                            this.callback = sendingRequestCallback
                        }
                        .executeBy(taskExecutor)
            }
        }
    }

    @VisibleForTesting
    @WorkerThread
    fun encryptGroupSession(olmInboundGroupSessionWrapper: OlmInboundGroupSessionWrapper): KeyBackupData {
        // Gather information for each key
        val device = cryptoStore.deviceWithIdentityKey(olmInboundGroupSessionWrapper.senderKey!!)

        // Build the m.megolm_backup.v1.curve25519-aes-sha2 data as defined at
        // https://github.com/uhoreg/matrix-doc/blob/e2e_backup/proposals/1219-storing-megolm-keys-serverside.md#mmegolm_backupv1curve25519-aes-sha2-key-format
        val sessionData = olmInboundGroupSessionWrapper.exportKeys()
        val sessionBackupData = mapOf(
                "algorithm" to sessionData!!.algorithm,
                "sender_key" to sessionData.senderKey,
                "sender_claimed_keys" to sessionData.senderClaimedKeys,
                "forwarding_curve25519_key_chain" to (sessionData.forwardingCurve25519KeyChain
                        ?: ArrayList<Any>()),
                "session_key" to sessionData.sessionKey)

        var encryptedSessionBackupData: OlmPkMessage? = null

        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(Map::class.java)

        try {
            val json = adapter.toJson(sessionBackupData)

            encryptedSessionBackupData = backupOlmPkEncryption?.encrypt(json)
        } catch (e: OlmException) {
            Timber.e(e, "OlmException")
        }

        // Build backup data for that key
        val keyBackupData = KeyBackupData()
        try {
            keyBackupData.firstMessageIndex = olmInboundGroupSessionWrapper.olmInboundGroupSession!!.firstKnownIndex
        } catch (e: OlmException) {
            Timber.e(e, "OlmException")
        }

        keyBackupData.forwardedCount = olmInboundGroupSessionWrapper.forwardingCurve25519KeyChain!!.size
        keyBackupData.isVerified = device?.isVerified == true

        val data = mapOf(
                "ciphertext" to encryptedSessionBackupData!!.mCipherText,
                "mac" to encryptedSessionBackupData.mMac,
                "ephemeral" to encryptedSessionBackupData.mEphemeralKey)

        keyBackupData.sessionData = data

        return keyBackupData
    }

    @VisibleForTesting
    @WorkerThread
    fun decryptKeyBackupData(keyBackupData: KeyBackupData, sessionId: String, roomId: String, decryption: OlmPkDecryption): MegolmSessionData? {
        var sessionBackupData: MegolmSessionData? = null

        val jsonObject = keyBackupData.sessionData

        val ciphertext = jsonObject?.get("ciphertext")?.toString()
        val mac = jsonObject?.get("mac")?.toString()
        val ephemeralKey = jsonObject?.get("ephemeral")?.toString()

        if (ciphertext != null && mac != null && ephemeralKey != null) {
            val encrypted = OlmPkMessage()
            encrypted.mCipherText = ciphertext
            encrypted.mMac = mac
            encrypted.mEphemeralKey = ephemeralKey

            try {
                val decrypted = decryption.decrypt(encrypted)

                val moshi = MoshiProvider.providesMoshi()
                val adapter = moshi.adapter(MegolmSessionData::class.java)

                sessionBackupData = adapter.fromJson(decrypted)
            } catch (e: OlmException) {
                Timber.e(e, "OlmException")
            }

            if (sessionBackupData != null) {
                sessionBackupData.sessionId = sessionId
                sessionBackupData.roomId = roomId
            }
        }

        return sessionBackupData
    }

    companion object {
        // Maximum delay in ms in {@link maybeBackupKeys}
        private const val KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS = 10000

        // Maximum number of keys to send at a time to the homeserver.
        private const val KEY_BACKUP_SEND_KEYS_MAX_COUNT = 100
    }

/* ==========================================================================================
 * DEBUG INFO
 * ========================================================================================== */

    override fun toString() = "KeysBackup for ${credentials.userId}"
}
