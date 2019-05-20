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
import im.vector.matrix.android.internal.crypto.*
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
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXOlmInboundGroupSession2
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import org.matrix.olm.OlmException
import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption
import org.matrix.olm.OlmPkMessage
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.*
import kotlin.collections.HashMap

/**
 * A KeysBackup class instance manage incremental backup of e2e keys (megolm keys)
 * to the user's homeserver.
 */
internal class KeysBackup(
        private val mCredentials: Credentials,
        private val mCryptoStore: IMXCryptoStore,
        private val mOlmDevice: MXOlmDevice,
        private val mObjectSigner: ObjectSigner,
        // Actions
        private val mMegolmSessionDataImporter: MegolmSessionDataImporter,
        // Tasks
        private val mCreateKeysBackupVersionTask: CreateKeysBackupVersionTask,
        private val mDeleteBackupTask: DeleteBackupTask,
        private val mDeleteRoomSessionDataTask: DeleteRoomSessionDataTask,
        private val mDeleteRoomSessionsDataTask: DeleteRoomSessionsDataTask,
        private val mDeleteSessionDataTask: DeleteSessionsDataTask,
        private val mGetKeysBackupLastVersionTask: GetKeysBackupLastVersionTask,
        private val mGetKeysBackupVersionTask: GetKeysBackupVersionTask,
        private val mGetRoomSessionDataTask: GetRoomSessionDataTask,
        private val mGetRoomSessionsDataTask: GetRoomSessionsDataTask,
        private val mGetSessionsDataTask: GetSessionsDataTask,
        private val mStoreRoomSessionDataTask: StoreRoomSessionDataTask,
        private val mStoreSessionsDataTask: StoreRoomSessionsDataTask,
        private val mStoreSessionDataTask: StoreSessionsDataTask,
        private val mUpdateKeysBackupVersionTask: UpdateKeysBackupVersionTask,
        // Task executor
        private val mTaskExecutor: TaskExecutor
) : KeysBackupService {

    private val mUIHandler = Handler(Looper.getMainLooper())

    private val mKeysBackupStateManager = KeysBackupStateManager(mUIHandler)

    // The backup version
    override var mKeysBackupVersion: KeysVersionResult? = null
        private set

    // The backup key being used.
    private var mBackupKey: OlmPkEncryption? = null

    private val mRandom = Random()

    private var backupAllGroupSessionsCallback: MatrixCallback<Unit>? = null

    private var mKeysBackupStateListener: KeysBackupService.KeysBackupStateListener? = null

    override val isEnabled: Boolean
        get() = mKeysBackupStateManager.isEnabled

    override val isStucked: Boolean
        get() = mKeysBackupStateManager.isStucked

    override val state: KeysBackupState
        get() = mKeysBackupStateManager.state

    override val currentBackupVersion: String?
        get() = mKeysBackupVersion?.version

    override fun addListener(listener: KeysBackupService.KeysBackupStateListener) {
        mKeysBackupStateManager.addListener(listener)
    }

    override fun removeListener(listener: KeysBackupService.KeysBackupStateListener) {
        mKeysBackupStateManager.removeListener(listener)
    }

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
     * @param callback Asynchronous callback
     */
    override fun prepareKeysBackupVersion(password: String?,
                                          progressListener: ProgressListener?,
                                          callback: MatrixCallback<MegolmBackupCreationInfo>) {
        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            try {
                val olmPkDecryption = OlmPkDecryption()
                val megolmBackupAuthData = MegolmBackupAuthData()

                if (password != null) {
                    // Generate a private key from the password
                    val backgroundProgressListener = if (progressListener == null) {
                        null
                    } else {
                        object : ProgressListener {
                            override fun onProgress(progress: Int, total: Int) {
                                mUIHandler.post {
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

                val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, megolmBackupAuthData.signalableJSONDictionary())

                megolmBackupAuthData.signatures = mObjectSigner.signObject(canonicalJson)


                val megolmBackupCreationInfo = MegolmBackupCreationInfo()
                megolmBackupCreationInfo.algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
                megolmBackupCreationInfo.authData = megolmBackupAuthData
                megolmBackupCreationInfo.recoveryKey = computeRecoveryKey(olmPkDecryption.privateKey())

                mUIHandler.post { callback.onSuccess(megolmBackupCreationInfo) }
            } catch (e: OlmException) {
                Timber.e(e, "OlmException")

                mUIHandler.post { callback.onFailure(e) }
            }
        }
    }

    /**
     * Create a new keys backup version and enable it, using the information return from [prepareKeysBackupVersion].
     *
     * @param keysBackupCreationInfo the info object from [prepareKeysBackupVersion].
     * @param callback               Asynchronous callback
     */
    override fun createKeysBackupVersion(keysBackupCreationInfo: MegolmBackupCreationInfo,
                                         callback: MatrixCallback<KeysVersion>) {
        val createKeysBackupVersionBody = CreateKeysBackupVersionBody()
        createKeysBackupVersionBody.algorithm = keysBackupCreationInfo.algorithm
        createKeysBackupVersionBody.authData = MoshiProvider.providesMoshi().adapter(Map::class.java)
                .fromJson(keysBackupCreationInfo.authData?.toJsonString()) as Map<String, Any>?

        mKeysBackupStateManager.state = KeysBackupState.Enabling

        mCreateKeysBackupVersionTask
                .configureWith(createKeysBackupVersionBody)
                .dispatchTo(object : MatrixCallback<KeysVersion> {
                    override fun onSuccess(info: KeysVersion) {
                        // Reset backup markers.
                        mCryptoStore.resetBackupMarkers()

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
                        mKeysBackupStateManager.state = KeysBackupState.Disabled
                        callback.onFailure(failure)
                    }
                })
                .executeBy(mTaskExecutor)
    }

    /**
     * Delete a keys backup version. It will delete all backed up keys on the server, and the backup itself.
     * If we are backing up to this version. Backup will be stopped.
     *
     * @param version  the backup version to delete.
     * @param callback Asynchronous callback
     */
    override fun deleteBackup(version: String, callback: MatrixCallback<Unit>?) {
        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            // If we're currently backing up to this backup... stop.
            // (We start using it automatically in createKeysBackupVersion so this is symmetrical).
            if (mKeysBackupVersion != null && version == mKeysBackupVersion!!.version) {
                resetKeysBackupData()
                mKeysBackupVersion = null
                mKeysBackupStateManager.state = KeysBackupState.Unknown
            }

            mDeleteBackupTask.configureWith(DeleteBackupTask.Params(version))
                    .dispatchTo(object : MatrixCallback<Unit> {
                        private fun eventuallyRestartBackup() {
                            // Do not stay in KeysBackupState.Unknown but check what is available on the homeserver
                            if (state == KeysBackupState.Unknown) {
                                checkAndStartKeysBackup()
                            }
                        }

                        override fun onSuccess(data: Unit) {
                            eventuallyRestartBackup()

                            mUIHandler.post { callback?.onSuccess(Unit) }
                        }

                        override fun onFailure(failure: Throwable) {
                            eventuallyRestartBackup()

                            mUIHandler.post { callback?.onFailure(failure) }
                        }
                    })
                    .executeBy(mTaskExecutor)
        }
    }

    /**
     * Ask if the backup on the server contains keys that we may do not have locally.
     * This should be called when entering in the state READY_TO_BACKUP
     */
    override fun canRestoreKeys(): Boolean {
        // Server contains more keys than locally
        val totalNumberOfKeysLocally = getTotalNumbersOfKeys()

        val keysBackupData = mCryptoStore.getKeysBackupData()

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

    /**
     * Facility method to get the total number of locally stored keys
     */
    override fun getTotalNumbersOfKeys(): Int {
        return mCryptoStore.inboundGroupSessionsCount(false)

    }

    /**
     * Facility method to get the number of backed up keys
     */
    override fun getTotalNumbersOfBackedUpKeys(): Int {
        return mCryptoStore.inboundGroupSessionsCount(true)
    }

    /**
     * Start to back up keys immediately.
     *
     * @param progressListener the callback to follow the progress
     * @param callback the main callback
     */
    override fun backupAllGroupSessions(progressListener: ProgressListener?,
                                        callback: MatrixCallback<Unit>?) {
        // Get a status right now
        getBackupProgress(object : ProgressListener {
            override fun onProgress(progress: Int, total: Int) {
                // Reset previous listeners if any
                resetBackupAllGroupSessionsListeners()
                Timber.d("backupAllGroupSessions: backupProgress: $progress/$total")
                try {
                    progressListener?.onProgress(progress, total)
                } catch (e: Exception) {
                    Timber.e(e, "backupAllGroupSessions: onProgress failure")
                }

                if (progress == total) {
                    Timber.d("backupAllGroupSessions: complete")
                    callback?.onSuccess(Unit)
                    return
                }

                backupAllGroupSessionsCallback = callback

                // Listen to `state` change to determine when to call onBackupProgress and onComplete
                mKeysBackupStateListener = object : KeysBackupService.KeysBackupStateListener {
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

                mKeysBackupStateManager.addListener(mKeysBackupStateListener!!)

                backupKeys()
            }
        })
    }

    /**
     * Check trust on a key backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param callback block called when the operations completes.
     */
    override fun getKeysBackupTrust(keysBackupVersion: KeysVersionResult,
                                    callback: MatrixCallback<KeysBackupVersionTrust>) {
        // TODO Validate with François that this is correct
        object : Task<KeysVersionResult, KeysBackupVersionTrust> {
            override fun execute(params: KeysVersionResult): Try<KeysBackupVersionTrust> {
                return Try {
                    getKeysBackupTrustBg(params)
                }
            }
        }
                .configureWith(keysBackupVersion)
                .dispatchTo(callback)
                .executeOn(TaskThread.COMPUTATION)
                .executeBy(mTaskExecutor)
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
        val myUserId = mCredentials.userId

        val keysBackupVersionTrust = KeysBackupVersionTrust()
        val authData = keysBackupVersion.getAuthDataAsMegolmBackupAuthData()

        if (keysBackupVersion.algorithm == null
                || authData == null
                || authData.publicKey.isEmpty()
                || authData.signatures.isNullOrEmpty()) {
            Timber.d("getKeysBackupTrust: Key backup is absent or missing required data")
            return keysBackupVersionTrust
        }

        val mySigs: Map<String, *> = authData.signatures!![myUserId] as Map<String, *>
        if (mySigs.isEmpty()) {
            Timber.d("getKeysBackupTrust: Ignoring key backup because it lacks any signatures from this user")
            return keysBackupVersionTrust
        }

        for (keyId in mySigs.keys) {
            // XXX: is this how we're supposed to get the device id?
            var deviceId: String? = null
            val components = keyId.split(":")
            if (components.size == 2) {
                deviceId = components[1]
            }

            var device: MXDeviceInfo? = null
            if (deviceId != null) {
                device = mCryptoStore.getUserDevice(deviceId, myUserId)

                var isSignatureValid = false

                if (device == null) {
                    Timber.d("getKeysBackupTrust: Signature from unknown device $deviceId")
                } else {
                    try {
                        mOlmDevice.verifySignature(device.fingerprint()!!, authData.signalableJSONDictionary(), mySigs[keyId] as String)
                        isSignatureValid = true
                    } catch (e: OlmException) {
                        Timber.d("getKeysBackupTrust: Bad signature from device " + device.deviceId + " " + e.localizedMessage)
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

    /**
     * Set trust on a keys backup version.
     * It adds (or removes) the signature of the current device to the authentication part of the keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param trust the trust to set to the keys backup.
     * @param callback block called when the operations completes.
     */
    override fun trustKeysBackupVersion(keysBackupVersion: KeysVersionResult,
                                        trust: Boolean,
                                        callback: MatrixCallback<Unit>) {
        Timber.d("trustKeyBackupVersion: $trust, version ${keysBackupVersion.version}")

        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            val myUserId = mCredentials.userId

            // Get auth data to update it
            val authData = getMegolmBackupAuthData(keysBackupVersion)

            if (authData == null) {
                Timber.w("trustKeyBackupVersion:trust: Key backup is missing required data")

                mUIHandler.post {
                    callback.onFailure(IllegalArgumentException("Missing element"))
                }

                return@post
            }

            // Get current signatures, or create an empty set
            val myUserSignatures = (authData.signatures!![myUserId]?.toMutableMap() ?: HashMap())

            if (trust) {
                // Add current device signature
                val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, authData.signalableJSONDictionary())

                val deviceSignatures = mObjectSigner.signObject(canonicalJson)

                deviceSignatures[myUserId]?.forEach { entry ->
                    myUserSignatures[entry.key] = entry.value
                }
            } else {
                // Remove current device signature
                myUserSignatures.remove("ed25519:${mCredentials.deviceId}")
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

            // And send it to the homeserver
            mUpdateKeysBackupVersionTask
                    .configureWith(UpdateKeysBackupVersionTask.Params(keysBackupVersion.version!!, updateKeysBackupVersionBody))
                    .dispatchTo(object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            // Relaunch the state machine on this updated backup version
                            val newKeysBackupVersion = KeysVersionResult()

                            newKeysBackupVersion.version = keysBackupVersion.version
                            newKeysBackupVersion.algorithm = keysBackupVersion.algorithm
                            newKeysBackupVersion.count = keysBackupVersion.count
                            newKeysBackupVersion.hash = keysBackupVersion.hash
                            newKeysBackupVersion.authData = updateKeysBackupVersionBody.authData

                            checkAndStartWithKeysBackupVersion(newKeysBackupVersion)

                            mUIHandler.post {
                                callback.onSuccess(data)
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            mUIHandler.post {
                                callback.onFailure(failure)
                            }
                        }
                    })
                    .executeBy(mTaskExecutor)
        }
    }

    /**
     * Set trust on a keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param recoveryKey the recovery key to challenge with the key backup public key.
     * @param callback block called when the operations completes.
     */
    override fun trustKeysBackupVersionWithRecoveryKey(keysBackupVersion: KeysVersionResult,
                                                       recoveryKey: String,
                                                       callback: MatrixCallback<Unit>) {
        Timber.d("trustKeysBackupVersionWithRecoveryKey: version ${keysBackupVersion.version}")

        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            if (!isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysBackupVersion)) {
                Timber.w("trustKeyBackupVersionWithRecoveryKey: Invalid recovery key.")

                mUIHandler.post {
                    callback.onFailure(IllegalArgumentException("Invalid recovery key or password"))
                }
                return@post
            }

            trustKeysBackupVersion(keysBackupVersion, true, callback)
        }
    }

    /**
     * Set trust on a keys backup version.
     *
     * @param keysBackupVersion the backup version to check.
     * @param password the pass phrase to challenge with the keyBackupVersion public key.
     * @param callback block called when the operations completes.
     */
    override fun trustKeysBackupVersionWithPassphrase(keysBackupVersion: KeysVersionResult,
                                                      password: String,
                                                      callback: MatrixCallback<Unit>) {
        Timber.d("trustKeysBackupVersionWithPassphrase: version ${keysBackupVersion.version}")

        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            val recoveryKey = recoveryKeyFromPassword(password, keysBackupVersion, null)

            if (recoveryKey == null) {
                Timber.w("trustKeysBackupVersionWithPassphrase: Key backup is missing required data")

                mUIHandler.post {
                    callback.onFailure(IllegalArgumentException("Missing element"))
                }

                return@post
            }

            // Check trust using the recovery key
            trustKeysBackupVersionWithRecoveryKey(keysBackupVersion, recoveryKey, callback)
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

        mKeysBackupStateListener?.let {
            mKeysBackupStateManager.removeListener(it)
        }

        mKeysBackupStateListener = null
    }

    /**
     * Return the current progress of the backup
     */
    override fun getBackupProgress(progressListener: ProgressListener) {
        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            val backedUpKeys = mCryptoStore.inboundGroupSessionsCount(true)
            val total = mCryptoStore.inboundGroupSessionsCount(false)

            mUIHandler.post { progressListener.onProgress(backedUpKeys, total) }
        }
    }

    /**
     * Restore a backup with a recovery key from a given backup version stored on the homeserver.
     *
     * @param keysVersionResult    the backup version to restore from.
     * @param recoveryKey          the recovery key to decrypt the retrieved backup.
     * @param roomId               the id of the room to get backup data from.
     * @param sessionId            the id of the session to restore.
     * @param stepProgressListener the step progress listener
     * @param callback             Callback. It provides the number of found keys and the number of successfully imported keys.
     */
    override fun restoreKeysWithRecoveryKey(keysVersionResult: KeysVersionResult,
                                            recoveryKey: String,
                                            roomId: String?,
                                            sessionId: String?,
                                            stepProgressListener: StepProgressListener?,
                                            callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.d("restoreKeysWithRecoveryKey: From backup version: ${keysVersionResult.version}")

        CryptoAsyncHelper.getDecryptBackgroundHandler().post(Runnable {
            // Check if the recovery is valid before going any further
            if (!isValidRecoveryKeyForKeysBackupVersion(recoveryKey, keysVersionResult)) {
                Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key for this keys version")
                mUIHandler.post { callback.onFailure(InvalidParameterException("Invalid recovery key")) }
                return@Runnable
            }

            // Get a PK decryption instance
            val decryption = pkDecryptionFromRecoveryKey(recoveryKey)
            if (decryption == null) {
                // This should not happen anymore
                Timber.e("restoreKeysWithRecoveryKey: Invalid recovery key. Error")
                mUIHandler.post { callback.onFailure(InvalidParameterException("Invalid recovery key")) }
                return@Runnable
            }

            if (stepProgressListener != null) {
                mUIHandler.post { stepProgressListener.onStepProgress(StepProgressListener.Step.DownloadingKey) }
            }

            // Get backed up keys from the homeserver
            getKeys(sessionId, roomId, keysVersionResult.version!!, object : MatrixCallback<KeysBackupData> {
                override fun onSuccess(data: KeysBackupData) {
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
                    Timber.d("restoreKeysWithRecoveryKey: Decrypted " + sessionsData.size + " keys out of "
                            + sessionsFromHsCount + " from the backup store on the homeserver")

                    // Do not trigger a backup for them if they come from the backup version we are using
                    val backUp = keysVersionResult.version != mKeysBackupVersion?.version
                    if (backUp) {
                        Timber.d("restoreKeysWithRecoveryKey: Those keys will be backed up to backup version: " + mKeysBackupVersion?.version)
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

                    mMegolmSessionDataImporter.handle(sessionsData, !backUp, progressListener, object : MatrixCallback<ImportRoomKeysResult> {
                        override fun onSuccess(data: ImportRoomKeysResult) {
                            // Do not back up the key if it comes from a backup recovery
                            if (backUp) {
                                maybeBackupKeys()
                            }

                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }
                    })
                }

                override fun onFailure(failure: Throwable) {
                    mUIHandler.post { callback.onFailure(failure) }
                }
            })
        })
    }

    /**
     * Restore a backup with a password from a given backup version stored on the homeserver.
     *
     * @param keysBackupVersion the backup version to restore from.
     * @param password the password to decrypt the retrieved backup.
     * @param roomId the id of the room to get backup data from.
     * @param sessionId the id of the session to restore.
     * @param stepProgressListener the step progress listener
     * @param callback Callback. It provides the number of found keys and the number of successfully imported keys.
     */
    override fun restoreKeyBackupWithPassword(keysBackupVersion: KeysVersionResult,
                                              password: String,
                                              roomId: String?,
                                              sessionId: String?,
                                              stepProgressListener: StepProgressListener?,
                                              callback: MatrixCallback<ImportRoomKeysResult>) {
        Timber.d("[MXKeyBackup] restoreKeyBackup with password: From backup version: ${keysBackupVersion.version}")

        CryptoAsyncHelper.getDecryptBackgroundHandler().post {
            val progressListener = if (stepProgressListener != null) {
                object : ProgressListener {
                    override fun onProgress(progress: Int, total: Int) {
                        mUIHandler.post {
                            stepProgressListener.onStepProgress(StepProgressListener.Step.ComputingKey(progress, total))
                        }
                    }
                }
            } else {
                null
            }

            val recoveryKey = recoveryKeyFromPassword(password, keysBackupVersion, progressListener)

            if (recoveryKey == null) {
                mUIHandler.post {
                    Timber.d("backupKeys: Invalid configuration")
                    callback.onFailure(IllegalStateException("Invalid configuration"))
                }

                return@post
            }

            restoreKeysWithRecoveryKey(keysBackupVersion, recoveryKey, roomId, sessionId, stepProgressListener, callback)
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
            mGetRoomSessionDataTask
                    .configureWith(GetRoomSessionDataTask.Params(roomId, sessionId, version))
                    .dispatchTo(object : MatrixCallback<KeyBackupData> {
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
                    })
                    .executeBy(mTaskExecutor)
        } else if (roomId != null) {
            // Get all keys for the room
            mGetRoomSessionsDataTask
                    .configureWith(GetRoomSessionsDataTask.Params(roomId, version))
                    .dispatchTo(object : MatrixCallback<RoomKeysBackupData> {
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
                    })
                    .executeBy(mTaskExecutor)
        } else {
            // Get all keys
            mGetSessionsDataTask
                    .configureWith(GetSessionsDataTask.Params(version))
                    .dispatchTo(callback)
                    .executeBy(mTaskExecutor)
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
                mKeysBackupStateManager.state = KeysBackupState.WillBackUp

                // Wait between 0 and 10 seconds, to avoid backup requests from
                // different clients hitting the server all at the same time when a
                // new key is sent
                val delayInMs = mRandom.nextInt(KEY_BACKUP_WAITING_TIME_TO_SEND_KEY_BACKUP_MILLIS).toLong()

                mUIHandler.postDelayed({ backupKeys() }, delayInMs)
            }
            else                                   -> {
                Timber.d("maybeBackupKeys: Skip it because state: $state")
            }
        }
    }

    /**
     * Get information about a backup version defined on the homeserver.
     *
     * It can be different than mKeysBackupVersion.
     * @param version the backup version
     * @param callback
     */
    override fun getVersion(version: String,
                            callback: MatrixCallback<KeysVersionResult?>) {
        mGetKeysBackupVersionTask
                .configureWith(version)
                .dispatchTo(object : MatrixCallback<KeysVersionResult> {
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
                })
                .executeBy(mTaskExecutor)
    }

    /**
     * Retrieve the current version of the backup from the home server
     *
     * It can be different than mKeysBackupVersion.
     * @param callback onSuccess(null) will be called if there is no backup on the server
     */
    override fun getCurrentVersion(callback: MatrixCallback<KeysVersionResult?>) {
        mGetKeysBackupLastVersionTask
                .configureWith(Unit)
                .dispatchTo(object : MatrixCallback<KeysVersionResult> {
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
                })
                .executeBy(mTaskExecutor)
    }

    /**
     * This method fetches the last backup version on the server, then compare to the currently backup version use.
     * If versions are not the same, the current backup is deleted (on server or locally), then the backup may be started again, using the last version.
     *
     * @param callback true if backup is already using the last version, and false if it is not the case
     */
    override fun forceUsingLastVersion(callback: MatrixCallback<Boolean>) {
        getCurrentVersion(object : MatrixCallback<KeysVersionResult?> {
            override fun onSuccess(data: KeysVersionResult?) {
                val localBackupVersion = mKeysBackupVersion?.version
                val serverBackupVersion = data?.version

                if (serverBackupVersion == null) {
                    if (localBackupVersion == null) {
                        // No backup on the server, and backup is not active
                        callback.onSuccess(true)
                    } else {
                        // No backup on the server, and we are currently backing up, so stop backing up
                        callback.onSuccess(false)
                        resetKeysBackupData()
                        mKeysBackupVersion = null
                        mKeysBackupStateManager.state = KeysBackupState.Disabled
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

    /**
     * Check the server for an active key backup.
     *
     * If one is present and has a valid signature from one of the user's verified
     * devices, start backing up to it.
     */
    override fun checkAndStartKeysBackup() {
        if (!isStucked) {
            // Try to start or restart the backup only if it is in unknown or bad state
            Timber.w("checkAndStartKeysBackup: invalid state: $state")

            return
        }

        mKeysBackupVersion = null
        mKeysBackupStateManager.state = KeysBackupState.CheckingBackUpOnHomeserver

        getCurrentVersion(object : MatrixCallback<KeysVersionResult?> {
            override fun onSuccess(data: KeysVersionResult?) {
                checkAndStartWithKeysBackupVersion(data)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "checkAndStartKeysBackup: Failed to get current version")
                mKeysBackupStateManager.state = KeysBackupState.Unknown
            }
        })
    }

    private fun checkAndStartWithKeysBackupVersion(keyBackupVersion: KeysVersionResult?) {
        Timber.d("checkAndStartWithKeyBackupVersion: ${keyBackupVersion?.version}")

        mKeysBackupVersion = keyBackupVersion

        if (keyBackupVersion == null) {
            Timber.d("checkAndStartWithKeysBackupVersion: Found no key backup version on the homeserver")
            resetKeysBackupData()
            mKeysBackupStateManager.state = KeysBackupState.Disabled
        } else {
            getKeysBackupTrust(keyBackupVersion, object : MatrixCallback<KeysBackupVersionTrust> {

                override fun onSuccess(data: KeysBackupVersionTrust) {
                    val versionInStore = mCryptoStore.getKeyBackupVersion()

                    if (data.usable) {
                        Timber.d("checkAndStartWithKeysBackupVersion: Found usable key backup. version: " + keyBackupVersion.version)
                        // Check the version we used at the previous app run
                        if (versionInStore != null && versionInStore != keyBackupVersion.version) {
                            Timber.d(" -> clean the previously used version $versionInStore")
                            resetKeysBackupData()
                        }

                        Timber.d("   -> enabling key backups")
                        enableKeysBackup(keyBackupVersion)
                    } else {
                        Timber.d("checkAndStartWithKeysBackupVersion: No usable key backup. version: " + keyBackupVersion.version)
                        if (versionInStore != null) {
                            Timber.d("   -> disabling key backup")
                            resetKeysBackupData()
                        }

                        mKeysBackupStateManager.state = KeysBackupState.NotTrusted
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

        if (authData.signatures == null
                || authData.publicKey.isBlank()) {
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
                mKeysBackupVersion = keysVersionResult
                mCryptoStore.setKeyBackupVersion(keysVersionResult.version)

                onServerDataRetrieved(keysVersionResult.count, keysVersionResult.hash)

                try {
                    mBackupKey = OlmPkEncryption().apply {
                        setRecipientKey(retrievedMegolmBackupAuthData.publicKey)
                    }
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                    mKeysBackupStateManager.state = KeysBackupState.Disabled
                    return
                }

                mKeysBackupStateManager.state = KeysBackupState.ReadyToBackUp

                maybeBackupKeys()
            } else {
                Timber.e("Invalid authentication data")
                mKeysBackupStateManager.state = KeysBackupState.Disabled
            }
        } else {
            Timber.e("Invalid authentication data")
            mKeysBackupStateManager.state = KeysBackupState.Disabled
        }
    }

    /**
     * Update the DB with data fetch from the server
     */
    private fun onServerDataRetrieved(count: Int?, hash: String?) {
        mCryptoStore.setKeysBackupData(KeysBackupDataEntity()
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

        mCryptoStore.setKeyBackupVersion(null)
        mCryptoStore.setKeysBackupData(null)
        mBackupKey = null

        // Reset backup markers
        mCryptoStore.resetBackupMarkers()
    }

    /**
     * Send a chunk of keys to backup
     */
    @UiThread
    private fun backupKeys() {
        Timber.d("backupKeys")

        // Sanity check, as this method can be called after a delay, the state may have change during the delay
        if (!isEnabled || mBackupKey == null || mKeysBackupVersion == null) {
            Timber.d("backupKeys: Invalid configuration")
            backupAllGroupSessionsCallback?.onFailure(IllegalStateException("Invalid configuration"))
            resetBackupAllGroupSessionsListeners()

            return
        }

        if (state === KeysBackupState.BackingUp) {
            // Do nothing if we are already backing up
            Timber.d("backupKeys: Invalid state: $state")
            return
        }

        // Get a chunk of keys to backup
        val sessions = mCryptoStore.inboundGroupSessionsToBackup(KEY_BACKUP_SEND_KEYS_MAX_COUNT)

        Timber.d("backupKeys: 1 - " + sessions.size + " sessions to back up")

        if (sessions.isEmpty()) {
            // Backup is up to date
            mKeysBackupStateManager.state = KeysBackupState.ReadyToBackUp

            backupAllGroupSessionsCallback?.onSuccess(Unit)
            resetBackupAllGroupSessionsListeners()
            return
        }

        mKeysBackupStateManager.state = KeysBackupState.BackingUp

        CryptoAsyncHelper.getEncryptBackgroundHandler().post {
            Timber.d("backupKeys: 2 - Encrypting keys")

            // Gather data to send to the homeserver
            // roomId -> sessionId -> MXKeyBackupData
            val keysBackupData = KeysBackupData()
            keysBackupData.roomIdToRoomKeysBackupData = HashMap()

            for (session in sessions) {
                val keyBackupData = encryptGroupSession(session)
                if (keysBackupData.roomIdToRoomKeysBackupData[session.mRoomId] == null) {
                    val roomKeysBackupData = RoomKeysBackupData()
                    roomKeysBackupData.sessionIdToKeyBackupData = HashMap()
                    keysBackupData.roomIdToRoomKeysBackupData[session.mRoomId!!] = roomKeysBackupData
                }

                try {
                    keysBackupData.roomIdToRoomKeysBackupData[session.mRoomId]!!.sessionIdToKeyBackupData[session.mSession!!.sessionIdentifier()] = keyBackupData
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                }
            }

            Timber.d("backupKeys: 4 - Sending request")

            // Make the request
            mStoreSessionDataTask
                    .configureWith(StoreSessionsDataTask.Params(mKeysBackupVersion!!.version!!, keysBackupData))
                    .dispatchTo(object : MatrixCallback<BackupKeysResult> {
                        override fun onSuccess(data: BackupKeysResult) {
                            mUIHandler.post {
                                Timber.d("backupKeys: 5a - Request complete")

                                // Mark keys as backed up
                                mCryptoStore.markBackupDoneForInboundGroupSessions(sessions)

                                if (sessions.size < KEY_BACKUP_SEND_KEYS_MAX_COUNT) {
                                    Timber.d("backupKeys: All keys have been backed up")
                                    onServerDataRetrieved(data.count, data.hash)

                                    // Note: Changing state will trigger the call to backupAllGroupSessionsCallback.onSuccess()
                                    mKeysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                } else {
                                    Timber.d("backupKeys: Continue to back up keys")
                                    mKeysBackupStateManager.state = KeysBackupState.WillBackUp

                                    backupKeys()
                                }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            if (failure is Failure.ServerError) {
                                mUIHandler.post {
                                    Timber.e(failure, "backupKeys: backupKeys failed.")

                                    when (failure.error.code) {
                                        MatrixError.NOT_FOUND,
                                        MatrixError.WRONG_ROOM_KEYS_VERSION -> {
                                            // Backup has been deleted on the server, or we are not using the last backup version
                                            mKeysBackupStateManager.state = KeysBackupState.WrongBackUpVersion
                                            backupAllGroupSessionsCallback?.onFailure(failure)
                                            resetBackupAllGroupSessionsListeners()
                                            resetKeysBackupData()
                                            mKeysBackupVersion = null

                                            // Do not stay in KeysBackupState.WrongBackUpVersion but check what is available on the homeserver
                                            checkAndStartKeysBackup()
                                        }
                                        else                                -> // Come back to the ready state so that we will retry on the next received key
                                            mKeysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                    }
                                }
                            } else {
                                mUIHandler.post {
                                    backupAllGroupSessionsCallback?.onFailure(failure)
                                    resetBackupAllGroupSessionsListeners()

                                    Timber.e("backupKeys: backupKeys failed.")

                                    // Retry a bit later
                                    mKeysBackupStateManager.state = KeysBackupState.ReadyToBackUp
                                    maybeBackupKeys()
                                }
                            }
                        }
                    })
                    .executeBy(mTaskExecutor)
        }
    }

    @VisibleForTesting
    @WorkerThread
    fun encryptGroupSession(session: MXOlmInboundGroupSession2): KeyBackupData {
        // Gather information for each key
        val device = mCryptoStore.deviceWithIdentityKey(session.mSenderKey!!)

        // Build the m.megolm_backup.v1.curve25519-aes-sha2 data as defined at
        // https://github.com/uhoreg/matrix-doc/blob/e2e_backup/proposals/1219-storing-megolm-keys-serverside.md#mmegolm_backupv1curve25519-aes-sha2-key-format
        val sessionData = session.exportKeys()
        val sessionBackupData = mapOf(
                "algorithm" to sessionData!!.algorithm,
                "sender_key" to sessionData.senderKey,
                "sender_claimed_keys" to sessionData.senderClaimedKeys,
                "forwarding_curve25519_key_chain" to (sessionData.forwardingCurve25519KeyChain ?: ArrayList<Any>()),
                "session_key" to sessionData.sessionKey)

        var encryptedSessionBackupData: OlmPkMessage? = null

        val moshi = MoshiProvider.providesMoshi()
        val adapter = moshi.adapter(Map::class.java)

        try {
            val json = adapter.toJson(sessionBackupData)

            encryptedSessionBackupData = mBackupKey?.encrypt(json)
        } catch (e: OlmException) {
            Timber.e(e, "OlmException")
        }

        // Build backup data for that key
        val keyBackupData = KeyBackupData()
        try {
            keyBackupData.firstMessageIndex = session.mSession!!.firstKnownIndex
        } catch (e: OlmException) {
            Timber.e(e, "OlmException")
        }

        keyBackupData.forwardedCount = session.mForwardingCurve25519KeyChain!!.size
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

    override fun toString() = "KeysBackup for ${mCredentials.userId}"
}
