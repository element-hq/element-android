/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.crypto

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keyshare.RoomKeysRequestListener
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.*
import im.vector.matrix.android.internal.crypto.model.event.RoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedMessage
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadResponse
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.*
import im.vector.matrix.android.internal.crypto.verification.DefaultSasVerificationService
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.convertToUTF8
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmManager
import timber.log.Timber
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * A `MXCrypto` class instance manages the end-to-end crypto for a MXSession instance.
 *
 *
 * Messages posted by the user are automatically redirected to MXCrypto in order to be encrypted
 * before sending.
 * In the other hand, received events goes through MXCrypto for decrypting.
 * MXCrypto maintains all necessary keys and their sharing with other devices required for the crypto.
 * Specially, it tracks all room membership changes events in order to do keys updates.
 */
internal class CryptoManager(
        // The credentials,
        private val mCredentials: Credentials,
        // the crypto store
        private val mCryptoStore: IMXCryptoStore,
        // Olm device
        private val mOlmDevice: MXOlmDevice,
        cryptoConfig: MXCryptoConfig?,
        // Device list manager
        private val deviceListManager: DeviceListManager,
        // The key backup service.
        private val mKeysBackup: KeysBackup,
        //
        private val roomDecryptorProvider: RoomDecryptorProvider,
        // The SAS verification service.
        private val mSasVerificationService: DefaultSasVerificationService,
        //
        private val mIncomingRoomKeyRequestManager: IncomingRoomKeyRequestManager,
        //
        private val mOutgoingRoomKeyRequestManager: MXOutgoingRoomKeyRequestManager,
        // Room service
        private val mRoomService: RoomService,
        // Olm Manager
        private val mOlmManager: OlmManager,
        // Tasks
        private val mClaimOneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask,
        private val mDeleteDeviceTask: DeleteDeviceTask,
        private val mGetDevicesTask: GetDevicesTask,
        private val mGetKeyChangesTask: GetKeyChangesTask,
        private val mSendToDeviceTask: SendToDeviceTask,
        private val mSetDeviceNameTask: SetDeviceNameTask,
        private val mUploadKeysTask: UploadKeysTask,
        // TaskExecutor
        private val mTaskExecutor: TaskExecutor
) : KeysBackup.KeysBackupCryptoListener,
        DefaultSasVerificationService.SasCryptoListener,
        DeviceListManager.DeviceListCryptoListener,
        CryptoService {

    // MXEncrypting instance for each room.
    private val mRoomEncryptors: MutableMap<String, IMXEncrypting>

    // Our device keys
    /**
     * @return my device info
     */
    private val myDevice: MXDeviceInfo

    private var mLastPublishedOneTimeKeys: Map<String, Map<String, String>>? = null

    // the encryption is starting
    private var mIsStarting: Boolean = false

    // tell if the crypto is started
    private var mIsStarted: Boolean = false

    // the crypto background threads
    private var mEncryptingHandlerThread: HandlerThread? = null
    private var mEncryptingHandler: Handler? = null

    private var mDecryptingHandlerThread: HandlerThread? = null
    private var mDecryptingHandler: Handler? = null

    // the UI thread
    private val mUIHandler: Handler

    private var mOneTimeKeyCount: Int? = null

    // TODO
    //private val mNetworkListener = object : IMXNetworkEventListener {
    //    override fun onNetworkConnectionUpdate(isConnected: Boolean) {
    //        if (isConnected && !isStarted()) {
    //            Timber.d("Start MXCrypto because a network connection has been retrieved ")
    //            start(false, null)
    //        }
    //    }
    //}

    fun onLiveEvent(roomId: String, event: Event) {
        if (event.type == EventType.ENCRYPTION) {
            onRoomEncryptionEvent(roomId, event)
        } else if (event.type == EventType.STATE_ROOM_MEMBER) {
            onRoomMembershipEvent(roomId, event)
        }
    }

    // initialization callbacks
    private val mInitializationCallbacks = ArrayList<MatrixCallback<Unit>>()

    // Warn the user if some new devices are detected while encrypting a message.
    private var mWarnOnUnknownDevices = true

    // tell if there is a OTK check in progress
    private var mOneTimeKeyCheckInProgress = false

    // last OTK check timestamp
    private var mLastOneTimeKeyCheck: Long = 0

    // Set of parameters used to configure/customize the end-to-end crypto.
    private var mCryptoConfig: MXCryptoConfig? = null

    /**
     * @return the encrypting thread handler
     */
    // mEncryptingHandlerThread was not yet ready
    // fail to get the handler
    // might happen if the thread is not yet ready
    val encryptingThreadHandler: Handler
        get() {
            if (null == mEncryptingHandler) {
                mEncryptingHandler = Handler(mEncryptingHandlerThread!!.looper)
            }
            return if (null == mEncryptingHandler) {
                mUIHandler
            } else mEncryptingHandler!!
        }

    /**
     * Tells whether the client should ever send encrypted messages to unverified devices.
     * The default value is false.
     * This function must be called in the getEncryptingThreadHandler() thread.
     *
     * @return true to unilaterally blacklist all unverified devices.
     */
    val globalBlacklistUnverifiedDevices: Boolean
        get() = mCryptoStore.getGlobalBlacklistUnverifiedDevices()

    init {
        if (null != cryptoConfig) {
            mCryptoConfig = cryptoConfig
        } else {
            // Consider the default configuration value
            mCryptoConfig = MXCryptoConfig()
        }

        mRoomEncryptors = HashMap() // TODO Merge with declaration

        var deviceId = mCredentials.deviceId
        // deviceId should always be defined
        val refreshDevicesList = !TextUtils.isEmpty(deviceId)

        if (TextUtils.isEmpty(deviceId)) {
            // use the stored one
            deviceId = this.mCryptoStore.getDeviceId()

            // Should not happen anymore
            TODO()
            //mSession.setDeviceId(deviceId)
        }

        if (TextUtils.isEmpty(deviceId)) {
            deviceId = UUID.randomUUID().toString()
            // Should not happen anymore
            TODO()
            //mSession.setDeviceId(deviceId)
            Timber.d("Warning: No device id in MXCredentials. An id was created. Think of storing it")
            this.mCryptoStore.storeDeviceId(deviceId)
        }

        myDevice = MXDeviceInfo(deviceId!!, mCredentials.userId)

        val keys = HashMap<String, String>()

        if (!TextUtils.isEmpty(mOlmDevice.deviceEd25519Key)) {
            keys["ed25519:" + mCredentials.deviceId] = mOlmDevice.deviceEd25519Key!!
        }

        if (!TextUtils.isEmpty(mOlmDevice.deviceCurve25519Key)) {
            keys["curve25519:" + mCredentials.deviceId] = mOlmDevice.deviceCurve25519Key!!
        }

        myDevice.keys = keys

        myDevice.algorithms = MXCryptoAlgorithms.supportedAlgorithms()
        myDevice.mVerified = MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED

        // Add our own deviceinfo to the store
        val endToEndDevicesForUser = this.mCryptoStore.getUserDevices(mCredentials.userId)

        val myDevices: MutableMap<String, MXDeviceInfo>

        if (null != endToEndDevicesForUser) {
            myDevices = HashMap(endToEndDevicesForUser)
        } else {
            myDevices = HashMap()
        }

        myDevices[myDevice.deviceId] = myDevice

        this.mCryptoStore.storeUserDevices(mCredentials.userId, myDevices)

        mEncryptingHandlerThread = HandlerThread("MXCrypto_encrypting_" + mCredentials.userId, Thread.MIN_PRIORITY)
        mEncryptingHandlerThread!!.start()

        mDecryptingHandlerThread = HandlerThread("MXCrypto_decrypting_" + mCredentials.userId, Thread.MIN_PRIORITY)
        mDecryptingHandlerThread!!.start()

        mUIHandler = Handler(Looper.getMainLooper())

        if (refreshDevicesList) {
            // ensure to have the up-to-date devices list
            // got some issues when upgrading from Riot < 0.6.4
            deviceListManager.handleDeviceListsChanges(listOf(mCredentials.userId), null)
        }

        mOutgoingRoomKeyRequestManager.setWorkingHandler(encryptingThreadHandler)
        mIncomingRoomKeyRequestManager.setEncryptingThreadHandler(encryptingThreadHandler)

        mKeysBackup.setCryptoInternalListener(this)
        mSasVerificationService.setCryptoInternalListener(this)
        deviceListManager.setCryptoInternalListener(this)
    }

    override fun setDeviceName(deviceId: String, deviceName: String, callback: MatrixCallback<Unit>) {
        mSetDeviceNameTask
                .configureWith(SetDeviceNameTask.Params(deviceId, deviceName))
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    override fun deleteDevice(deviceId: String, accountPassword: String, callback: MatrixCallback<Unit>) {
        mDeleteDeviceTask
                .configureWith(DeleteDeviceTask.Params(deviceId, accountPassword))
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    override fun getCryptoVersion(context: Context, longFormat: Boolean): String {
        return if (longFormat) mOlmManager.getDetailedVersion(context) else mOlmManager.version
    }

    override fun getMyDevice(): MXDeviceInfo {
        return myDevice
    }

    override fun getDevicesList(callback: MatrixCallback<DevicesListResponse>) {
        mGetDevicesTask
                .configureWith(Unit)
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    /**
     * @return the decrypting thread handler
     */
    fun getDecryptingThreadHandler(): Handler {
        // mDecryptingHandlerThread was not yet ready
        if (null == mDecryptingHandler) {
            mDecryptingHandler = Handler(mDecryptingHandlerThread!!.looper)
        }

        // fail to get the handler
        // might happen if the thread is not yet ready
        return if (null == mDecryptingHandler) {
            mUIHandler
        } else mDecryptingHandler!!
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return mCryptoStore.inboundGroupSessionsCount(onlyBackedUp)
    }

    /**
     * @return true if this instance has been released
     */
    override fun hasBeenReleased(): Boolean {
        return null == mOlmDevice
    }

    /**
     * Provides the tracking status
     *
     * @param userId the user id
     * @return the tracking status
     */
    override fun getDeviceTrackingStatus(userId: String): Int {
        return mCryptoStore.getDeviceTrackingStatus(userId, DeviceListManager.TRACKING_STATUS_NOT_TRACKED)
    }

    /**
     * Tell if the MXCrypto is started
     *
     * @return true if the crypto is started
     */
    fun isStarted(): Boolean {
        return mIsStarted
    }

    /**
     * Tells if the MXCrypto is starting.
     *
     * @return true if the crypto is starting
     */
    fun isStarting(): Boolean {
        return mIsStarting
    }

    /**
     * Start the crypto module.
     * Device keys will be uploaded, then one time keys if there are not enough on the homeserver
     * and, then, if this is the first time, this new device will be announced to all other users
     * devices.
     *
     * @param isInitialSync true if it starts from an initial sync
     * @param aCallback     the asynchronous callback
     */
    fun start(isInitialSync: Boolean, aCallback: MatrixCallback<Unit>?) {
        synchronized(mInitializationCallbacks) {
            if (null != aCallback && mInitializationCallbacks.indexOf(aCallback) < 0) {
                mInitializationCallbacks.add(aCallback)
            }
        }

        if (mIsStarting) {
            return
        }

        // do not start if there is not network connection
        // TODO
        //if (null != mNetworkConnectivityReceiver && !mNetworkConnectivityReceiver!!.isConnected()) {
        //    // wait that a valid network connection is retrieved
        //    mNetworkConnectivityReceiver!!.removeEventListener(mNetworkListener)
        //    mNetworkConnectivityReceiver!!.addEventListener(mNetworkListener)
        //    return
        //}

        mIsStarting = true

        // Open the store
        mCryptoStore.open()

        encryptingThreadHandler.post {
            uploadDeviceKeys(object : MatrixCallback<KeysUploadResponse> {
                private fun onError() {
                    mUIHandler.postDelayed({
                        if (!isStarted()) {
                            mIsStarting = false
                            start(isInitialSync, null)
                        }
                    }, 1000)
                }

                override fun onSuccess(data: KeysUploadResponse) {
                    encryptingThreadHandler.post {
                        if (!hasBeenReleased()) {
                            Timber.d("###########################################################")
                            Timber.d("uploadDeviceKeys done for " + mCredentials.userId)
                            Timber.d("  - device id  : " + mCredentials.deviceId)
                            Timber.d("  - ed25519    : " + mOlmDevice.deviceEd25519Key)
                            Timber.d("  - curve25519 : " + mOlmDevice.deviceCurve25519Key)
                            Timber.d("  - oneTimeKeys: " + mLastPublishedOneTimeKeys)
                            Timber.d("")

                            encryptingThreadHandler.post {
                                maybeUploadOneTimeKeys(object : MatrixCallback<Unit> {
                                    override fun onSuccess(data: Unit) {
                                        encryptingThreadHandler.post {
                                            // TODO
                                            //if (null != mNetworkConnectivityReceiver) {
                                            //    mNetworkConnectivityReceiver!!.removeEventListener(mNetworkListener)
                                            //}

                                            mIsStarting = false
                                            mIsStarted = true

                                            mOutgoingRoomKeyRequestManager.start()

                                            mKeysBackup.checkAndStartKeysBackup()

                                            synchronized(mInitializationCallbacks) {
                                                for (callback in mInitializationCallbacks) {
                                                    mUIHandler.post { callback.onSuccess(Unit) }
                                                }
                                                mInitializationCallbacks.clear()
                                            }

                                            if (isInitialSync) {
                                                encryptingThreadHandler.post {
                                                    // refresh the devices list for each known room members
                                                    deviceListManager.invalidateAllDeviceLists()
                                                    deviceListManager.refreshOutdatedDeviceLists()
                                                }
                                            } else {
                                                encryptingThreadHandler.post {
                                                    mIncomingRoomKeyRequestManager.processReceivedRoomKeyRequests()

                                                }
                                            }
                                        }
                                    }

                                    override fun onFailure(failure: Throwable) {
                                        Timber.e(failure, "## start failed")
                                        onError()
                                    }
                                })
                            }
                        }
                    }
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## start failed")
                    onError()
                }
            })
        }
    }

    /**
     * Close the crypto
     */
    fun close() {
        if (null != mEncryptingHandlerThread) {
            encryptingThreadHandler.post {
                mOlmDevice.release()

                // Do not reset My Device
                // mMyDevice = null;

                mCryptoStore.close()
                // Do not reset Crypto store
                // mCryptoStore = null;

                if (null != mEncryptingHandlerThread) {
                    mEncryptingHandlerThread!!.quit()
                    mEncryptingHandlerThread = null
                }

                mOutgoingRoomKeyRequestManager.stop()
            }

            getDecryptingThreadHandler().post {
                if (null != mDecryptingHandlerThread) {
                    mDecryptingHandlerThread!!.quit()
                    mDecryptingHandlerThread = null
                }
            }
        }
    }

    override fun isCryptoEnabled(): Boolean {
        // TODO Check that this test is correct
        return mOlmDevice != null
    }

    /**
     * @return the Keys backup Service
     */
    override fun getKeysBackupService(): KeysBackupService {
        return mKeysBackup
    }

    /**
     * @return the SasVerificationService
     */
    override fun getSasVerificationService(): SasVerificationService {
        return mSasVerificationService
    }

    /**
     * A sync response has been received
     *
     * @param syncResponse the syncResponse
     * @param fromToken    the start sync token
     * @param isCatchingUp true if there is a catch-up in progress.
     */
    fun onSyncCompleted(syncResponse: SyncResponse, fromToken: String?, isCatchingUp: Boolean) {
        encryptingThreadHandler.post {
            if (null != syncResponse.deviceLists) {
                deviceListManager.handleDeviceListsChanges(syncResponse.deviceLists.changed, syncResponse.deviceLists.left)
            }

            if (null != syncResponse.deviceOneTimeKeysCount) {
                val currentCount = syncResponse.deviceOneTimeKeysCount.signedCurve25519 ?: 0
                updateOneTimeKeyCount(currentCount)
            }

            if (isStarted()) {
                // Make sure we process to-device messages before generating new one-time-keys #2782
                deviceListManager.refreshOutdatedDeviceLists()
            }

            if (!isCatchingUp && isStarted()) {
                maybeUploadOneTimeKeys()

                mIncomingRoomKeyRequestManager.processReceivedRoomKeyRequests()
            }
        }
    }

    /**
     * Get the stored device keys for a user.
     *
     * @param userId   the user to list keys for.
     * @param callback the asynchronous callback
     */
    fun getUserDevices(userId: String, callback: MatrixCallback<List<MXDeviceInfo>>?) {
        encryptingThreadHandler.post {
            val list = getUserDevices(userId)

            if (null != callback) {
                mUIHandler.post { callback.onSuccess(list) }
            }
        }
    }

    /**
     * Stores the current one_time_key count which will be handled later (in a call of
     * _onSyncCompleted). The count is e.g. coming from a /sync response.
     *
     * @param currentCount the new count
     */
    private fun updateOneTimeKeyCount(currentCount: Int) {
        mOneTimeKeyCount = currentCount
    }

    /**
     * Find a device by curve25519 identity key
     *
     * @param senderKey the curve25519 key to match.
     * @param algorithm the encryption algorithm.
     * @return the device info, or null if not found / unsupported algorithm / crypto released
     */
    override fun deviceWithIdentityKey(senderKey: String, algorithm: String): MXDeviceInfo? {
        return if (!hasBeenReleased()) {
            if (!TextUtils.equals(algorithm, MXCRYPTO_ALGORITHM_MEGOLM) && !TextUtils.equals(algorithm, MXCRYPTO_ALGORITHM_OLM)) {
                // We only deal in olm keys
                null
            } else mCryptoStore.deviceWithIdentityKey(senderKey)

            // Find in the crypto store
        } else null

        // The store is released
    }

    /**
     * Provides the device information for a device id and a user Id
     *
     * @param userId   the user id
     * @param deviceId the device id
     * @param callback the asynchronous callback
     */
    override fun getDeviceInfo(userId: String, deviceId: String?, callback: MatrixCallback<MXDeviceInfo?>) {
        getDecryptingThreadHandler().post {
            val di: MXDeviceInfo?

            if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(deviceId)) {
                di = mCryptoStore.getUserDevice(deviceId!!, userId)
            } else {
                di = null
            }

            mUIHandler.post { callback.onSuccess(di) }
        }
    }

    /**
     * Set the devices as known
     *
     * @param devices  the devices. Note that the mVerified member of the devices in this list will not be updated by this method.
     * @param callback the asynchronous callback
     */
    override fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?) {
        if (hasBeenReleased()) {
            return
        }
        encryptingThreadHandler.post {
            // build a devices map
            val devicesIdListByUserId = HashMap<String, List<String>>()

            for (di in devices) {
                var deviceIdsList: MutableList<String>? = devicesIdListByUserId[di.userId]?.toMutableList()

                if (null == deviceIdsList) {
                    deviceIdsList = ArrayList()
                    devicesIdListByUserId[di.userId] = deviceIdsList
                }
                deviceIdsList.add(di.deviceId)
            }

            val userIds = devicesIdListByUserId.keys

            for (userId in userIds) {
                val storedDeviceIDs = mCryptoStore.getUserDevices(userId)

                // sanity checks
                if (null != storedDeviceIDs) {
                    var isUpdated = false
                    val deviceIds = devicesIdListByUserId[userId]

                    for (deviceId in deviceIds!!) {
                        val device = storedDeviceIDs[deviceId]

                        // assume if the device is either verified or blocked
                        // it means that the device is known
                        if (null != device && device.isUnknown) {
                            device.mVerified = MXDeviceInfo.DEVICE_VERIFICATION_UNVERIFIED
                            isUpdated = true
                        }
                    }

                    if (isUpdated) {
                        mCryptoStore.storeUserDevices(userId, storedDeviceIDs)
                    }
                }
            }

            if (null != callback) {
                mUIHandler.post { callback.onSuccess(Unit) }
            }
        }
    }

    /**
     * Update the blocked/verified state of the given device.
     *
     * @param verificationStatus the new verification status
     * @param deviceId           the unique identifier for the device.
     * @param userId             the owner of the device
     * @param callback           the asynchronous callback
     */
    override fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String, callback: MatrixCallback<Unit>) {
        if (hasBeenReleased()) {
            return
        }

        encryptingThreadHandler.post(Runnable {
            val device = mCryptoStore.getUserDevice(deviceId, userId)

            // Sanity check
            if (null == device) {
                Timber.e("## setDeviceVerification() : Unknown device $userId:$deviceId")
                mUIHandler.post { callback.onSuccess(Unit) }
                return@Runnable
            }

            if (device.mVerified != verificationStatus) {
                device.mVerified = verificationStatus
                mCryptoStore.storeUserDevice(userId, device)

                if (userId == mCredentials.userId) {
                    // If one of the user's own devices is being marked as verified / unverified,
                    // check the key backup status, since whether or not we use this depends on
                    // whether it has a signature from a verified device
                    mKeysBackup.checkAndStartKeysBackup()
                }
            }

            mUIHandler.post { callback.onSuccess(Unit) }
        })
    }

    /**
     * Configure a room to use encryption.
     * This method must be called in getEncryptingThreadHandler
     *
     * @param roomId             the room id to enable encryption in.
     * @param algorithm          the encryption config for the room.
     * @param inhibitDeviceQuery true to suppress device list query for users in the room (for now)
     * @param membersId          list of members to start tracking their devices
     * @return true if the operation succeeds.
     */
    private fun setEncryptionInRoom(roomId: String, algorithm: String?, inhibitDeviceQuery: Boolean, membersId: List<String>): Boolean {
        if (hasBeenReleased()) {
            return false
        }

        // If we already have encryption in this room, we should ignore this event
        // (for now at least. Maybe we should alert the user somehow?)
        val existingAlgorithm = mCryptoStore.getRoomAlgorithm(roomId)

        if (!TextUtils.isEmpty(existingAlgorithm) && !TextUtils.equals(existingAlgorithm, algorithm)) {
            Timber.e("## setEncryptionInRoom() : Ignoring m.room.encryption event which requests a change of config in $roomId")
            return false
        }

        val encryptingClass = MXCryptoAlgorithms.encryptorClassForAlgorithm(algorithm)

        if (null == encryptingClass) {
            Timber.e("## setEncryptionInRoom() : Unable to encrypt with " + algorithm!!)
            return false
        }

        mCryptoStore.storeRoomAlgorithm(roomId, algorithm!!)

        val alg: IMXEncrypting

        try {
            val ctor = encryptingClass.constructors[0]
            alg = ctor.newInstance() as IMXEncrypting
        } catch (e: Exception) {
            Timber.e(e, "## setEncryptionInRoom() : fail to load the class")
            return false
        }

        alg.initWithMatrixSession(this,
                mOlmDevice,
                mKeysBackup,
                deviceListManager,
                mCredentials,
                mSendToDeviceTask,
                mTaskExecutor,
                roomId)

        synchronized(mRoomEncryptors) {
            mRoomEncryptors.put(roomId, alg)
        }

        // if encryption was not previously enabled in this room, we will have been
        // ignoring new device events for these users so far. We may well have
        // up-to-date lists for some users, for instance if we were sharing other
        // e2e rooms with them, so there is room for optimisation here, but for now
        // we just invalidate everyone in the room.
        if (null == existingAlgorithm) {
            Timber.d("Enabling encryption in $roomId for the first time; invalidating device lists for all users therein")

            val userIds = ArrayList(membersId)

            deviceListManager.startTrackingDeviceList(userIds)

            if (!inhibitDeviceQuery) {
                deviceListManager.refreshOutdatedDeviceLists()
            }
        }

        return true
    }

    /**
     * Tells if a room is encrypted
     *
     * @param roomId the room id
     * @return true if the room is encrypted
     */
    fun isRoomEncrypted(roomId: String?): Boolean {
        var res = false

        if (null != roomId) {
            synchronized(mRoomEncryptors) {
                res = mRoomEncryptors.containsKey(roomId)

                if (!res) {
                    val room = mRoomService.getRoom(roomId)

                    if (null != room) {
                        res = room.isEncrypted()
                    }
                }
            }
        }

        return res
    }

    /**
     * @return the stored device keys for a user.
     */
    override fun getUserDevices(userId: String): MutableList<MXDeviceInfo> {
        val map = mCryptoStore.getUserDevices(userId)
        return if (null != map) ArrayList(map.values) else ArrayList()
    }

    /**
     * Try to make sure we have established olm sessions for the given users.
     * It must be called in getEncryptingThreadHandler() thread.
     * The callback is called in the UI thread.
     *
     * @param users    a list of user ids.
     * @param callback the asynchronous callback
     */
    fun ensureOlmSessionsForUsers(users: List<String>, callback: MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>>) {
        Timber.d("## ensureOlmSessionsForUsers() : ensureOlmSessionsForUsers $users")

        val devicesByUser = HashMap<String /* userId */, MutableList<MXDeviceInfo>>()

        for (userId in users) {
            devicesByUser[userId] = ArrayList()

            val devices = getUserDevices(userId)

            for (device in devices) {
                val key = device.identityKey()

                if (TextUtils.equals(key, mOlmDevice.deviceCurve25519Key)) {
                    // Don't bother setting up session to ourself
                    continue
                }

                if (device.isVerified) {
                    // Don't bother setting up sessions with blocked users
                    continue
                }

                devicesByUser[userId]!!.add(device)
            }
        }

        ensureOlmSessionsForDevices(devicesByUser, callback)
    }

    /**
     * Try to make sure we have established olm sessions for the given devices.
     * It must be called in getCryptoHandler() thread.
     * The callback is called in the UI thread.
     *
     * @param devicesByUser a map from userid to list of devices.
     * @param callback      the asynchronous callback
     */
    fun ensureOlmSessionsForDevices(devicesByUser: Map<String, List<MXDeviceInfo>>,
                                    callback: MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>>?) {
        val devicesWithoutSession = ArrayList<MXDeviceInfo>()

        val results = MXUsersDevicesMap<MXOlmSessionResult>()

        val userIds = devicesByUser.keys

        for (userId in userIds) {
            val deviceInfos = devicesByUser[userId]

            for (deviceInfo in deviceInfos!!) {
                val deviceId = deviceInfo.deviceId
                val key = deviceInfo.identityKey()

                val sessionId = mOlmDevice.getSessionId(key!!)

                if (TextUtils.isEmpty(sessionId)) {
                    devicesWithoutSession.add(deviceInfo)
                }

                val olmSessionResult = MXOlmSessionResult(deviceInfo, sessionId)
                results.setObject(olmSessionResult, userId, deviceId)
            }
        }

        if (devicesWithoutSession.size == 0) {
            if (null != callback) {
                mUIHandler.post { callback.onSuccess(results) }
            }
            return
        }

        // Prepare the request for claiming one-time keys
        val usersDevicesToClaim = MXUsersDevicesMap<String>()

        val oneTimeKeyAlgorithm = MXKey.KEY_SIGNED_CURVE_25519_TYPE

        for (device in devicesWithoutSession) {
            usersDevicesToClaim.setObject(oneTimeKeyAlgorithm, device.userId, device.deviceId)
        }

        // TODO: this has a race condition - if we try to send another message
        // while we are claiming a key, we will end up claiming two and setting up
        // two sessions.
        //
        // That should eventually resolve itself, but it's poor form.

        Timber.d("## claimOneTimeKeysForUsersDevices() : $usersDevicesToClaim")

        mClaimOneTimeKeysForUsersDeviceTask
                .configureWith(ClaimOneTimeKeysForUsersDeviceTask.Params(usersDevicesToClaim))
                .dispatchTo(object : MatrixCallback<MXUsersDevicesMap<MXKey>> {
                    override fun onSuccess(data: MXUsersDevicesMap<MXKey>) {
                        encryptingThreadHandler.post {
                            try {
                                Timber.d("## claimOneTimeKeysForUsersDevices() : keysClaimResponse.oneTimeKeys: $data")

                                for (userId in userIds) {
                                    val deviceInfos = devicesByUser[userId]

                                    for (deviceInfo in deviceInfos!!) {

                                        var oneTimeKey: MXKey? = null

                                        val deviceIds = data.getUserDeviceIds(userId)

                                        if (null != deviceIds) {
                                            for (deviceId in deviceIds) {
                                                val olmSessionResult = results.getObject(deviceId, userId)

                                                if (null != olmSessionResult!!.mSessionId) {
                                                    // We already have a result for this device
                                                    continue
                                                }

                                                val key = data.getObject(deviceId, userId)

                                                if (TextUtils.equals(key!!.type, oneTimeKeyAlgorithm)) {
                                                    oneTimeKey = key
                                                }

                                                if (null == oneTimeKey) {
                                                    Timber.d("## ensureOlmSessionsForDevices() : No one-time keys " + oneTimeKeyAlgorithm
                                                            + " for device " + userId + " : " + deviceId)
                                                    continue
                                                }

                                                // Update the result for this device in results
                                                olmSessionResult.mSessionId = verifyKeyAndStartSession(oneTimeKey, userId, deviceInfo)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "## ensureOlmSessionsForDevices() " + e.message)
                            }

                            if (!hasBeenReleased()) {
                                if (null != callback) {
                                    mUIHandler.post { callback.onSuccess(results) }
                                }
                            }
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## ensureOlmSessionsForUsers(): claimOneTimeKeysForUsersDevices request failed")

                        callback?.onFailure(failure)
                    }
                })
                .executeBy(mTaskExecutor)
    }

    private fun verifyKeyAndStartSession(oneTimeKey: MXKey, userId: String, deviceInfo: MXDeviceInfo): String? {
        var sessionId: String? = null

        val deviceId = deviceInfo.deviceId
        val signKeyId = "ed25519:$deviceId"
        val signature = oneTimeKey.signatureForUserId(userId, signKeyId)

        if (!TextUtils.isEmpty(signature) && !TextUtils.isEmpty(deviceInfo.fingerprint())) {
            var isVerified = false
            var errorMessage: String? = null

            try {
                mOlmDevice.verifySignature(deviceInfo.fingerprint()!!, oneTimeKey.signalableJSONDictionary(), signature)
                isVerified = true
            } catch (e: Exception) {
                errorMessage = e.message
            }

            // Check one-time key signature
            if (isVerified) {
                sessionId = mOlmDevice.createOutboundSession(deviceInfo.identityKey()!!, oneTimeKey.value)

                if (!TextUtils.isEmpty(sessionId)) {
                    Timber.d("## verifyKeyAndStartSession() : Started new sessionid " + sessionId
                            + " for device " + deviceInfo + "(theirOneTimeKey: " + oneTimeKey.value + ")")
                } else {
                    // Possibly a bad key
                    Timber.e("## verifyKeyAndStartSession() : Error starting session with device $userId:$deviceId")
                }
            } else {
                Timber.e("## verifyKeyAndStartSession() : Unable to verify signature on one-time key for device " + userId
                        + ":" + deviceId + " Error " + errorMessage)
            }
        }

        return sessionId
    }


    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType    the type of the event.
     * @param room         the room the event will be sent.
     * @param callback     the asynchronous callback
     */
    fun encryptEventContent(eventContent: Content,
                            eventType: String,
                            room: Room,
                            callback: MatrixCallback<MXEncryptEventContentResult>) {
        // wait that the crypto is really started
        if (!isStarted()) {
            Timber.d("## encryptEventContent() : wait after e2e init")

            start(false, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    encryptEventContent(eventContent, eventType, room, callback)
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## encryptEventContent() : onNetworkError while waiting to start e2e")

                    callback.onFailure(failure)
                }
            })

            return
        }

        // Check whether the event content must be encrypted for the invited members.
        val encryptForInvitedMembers = mCryptoConfig!!.mEnableEncryptionForInvitedMembers && room.shouldEncryptForInvitedMembers()

        val userIds = if (encryptForInvitedMembers) {
            room.getActiveRoomMemberIds()
        } else {
            room.getJoinedRoomMemberIds()
        }

        // just as you are sending a secret message?

        encryptingThreadHandler.post {
            var alg: IMXEncrypting?

            synchronized(mRoomEncryptors) {
                alg = mRoomEncryptors[room.roomId]
            }

            if (null == alg) {
                val algorithm = room.encryptionAlgorithm()

                if (null != algorithm) {
                    if (setEncryptionInRoom(room.roomId, algorithm, false, userIds)) {
                        synchronized(mRoomEncryptors) {
                            alg = mRoomEncryptors[room.roomId]
                        }
                    }
                }
            }

            if (null != alg) {
                val t0 = System.currentTimeMillis()
                Timber.d("## encryptEventContent() starts")

                alg!!.encryptEventContent(eventContent, eventType, userIds, object : MatrixCallback<Content> {
                    override fun onSuccess(data: Content) {
                        Timber.d("## encryptEventContent() : succeeds after " + (System.currentTimeMillis() - t0) + " ms")

                        callback.onSuccess(MXEncryptEventContentResult(data, EventType.ENCRYPTED))
                    }

                    override fun onFailure(failure: Throwable) {
                        callback.onFailure(failure)
                    }
                })
            } else {
                val algorithm = room.encryptionAlgorithm()
                val reason = String.format(MXCryptoError.UNABLE_TO_ENCRYPT_REASON,
                        algorithm ?: MXCryptoError.NO_MORE_ALGORITHM_REASON)
                Timber.e("## encryptEventContent() : $reason")

                mUIHandler.post {
                    callback.onFailure(Failure.CryptoError(MXCryptoError(MXCryptoError.UNABLE_TO_ENCRYPT_ERROR_CODE,
                            MXCryptoError.UNABLE_TO_ENCRYPT, reason)))
                }
            }
        }
    }

    /**
     * Decrypt an event
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or null in case of error
     */
    @Throws(MXDecryptionException::class)
    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult? {
        val eventContent = event.content //wireEventContent?

        if (null == eventContent) {
            Timber.e("## decryptEvent : empty event content")
            return null
        }

        val results = ArrayList<MXEventDecryptionResult>()
        val lock = CountDownLatch(1)
        val exceptions = ArrayList<MXDecryptionException>()

        getDecryptingThreadHandler().post {
            var result: MXEventDecryptionResult? = null
            val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(this, event.roomId, eventContent["algorithm"] as String)

            if (null == alg) {
                val reason = String.format(MXCryptoError.UNABLE_TO_DECRYPT_REASON, event.eventId, eventContent["algorithm"] as String)
                Timber.e("## decryptEvent() : $reason")
                exceptions.add(MXDecryptionException(MXCryptoError(MXCryptoError.UNABLE_TO_DECRYPT_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_DECRYPT, reason)))
            } else {
                try {
                    result = alg.decryptEvent(event, timeline)
                } catch (decryptionException: MXDecryptionException) {
                    exceptions.add(decryptionException)
                }

                if (null != result) {
                    results.add(result)
                }
            }
            lock.countDown()
        }

        try {
            lock.await()
        } catch (e: Exception) {
            Timber.e(e, "## decryptEvent() : failed")
        }

        if (!exceptions.isEmpty()) {
            throw exceptions[0]
        }

        return if (!results.isEmpty()) {
            results[0]
        } else null

    }

    /**
     * Reset replay attack data for the given timeline.
     *
     * @param timelineId the timeline id
     */
    fun resetReplayAttackCheckInTimeline(timelineId: String) {
        getDecryptingThreadHandler().post { mOlmDevice.resetReplayAttackCheckInTimeline(timelineId) }
    }

    /**
     * Encrypt an event payload for a list of devices.
     * This method must be called from the getCryptoHandler() thread.
     *
     * @param payloadFields fields to include in the encrypted payload.
     * @param deviceInfos   list of device infos to encrypt for.
     * @return the content for an m.room.encrypted event.
     */
    fun encryptMessage(payloadFields: Map<String, Any>, deviceInfos: List<MXDeviceInfo>): EncryptedMessage {
        if (hasBeenReleased()) {
            // Empty object
            return EncryptedMessage()
        }

        val deviceInfoParticipantKey = HashMap<String, MXDeviceInfo>()
        val participantKeys = ArrayList<String>()

        for (di in deviceInfos) {
            participantKeys.add(di.identityKey()!!)
            deviceInfoParticipantKey[di.identityKey()!!] = di
        }

        val payloadJson = HashMap(payloadFields)

        payloadJson["sender"] = mCredentials.userId
        payloadJson["sender_device"] = mCredentials.deviceId

        // Include the Ed25519 key so that the recipient knows what
        // device this message came from.
        // We don't need to include the curve25519 key since the
        // recipient will already know this from the olm headers.
        // When combined with the device keys retrieved from the
        // homeserver signed by the ed25519 key this proves that
        // the curve25519 key and the ed25519 key are owned by
        // the same device.
        val keysMap = HashMap<String, String>()
        keysMap["ed25519"] = mOlmDevice.deviceEd25519Key!!
        payloadJson["keys"] = keysMap

        val ciphertext = HashMap<String, Any>()

        for (deviceKey in participantKeys) {
            val sessionId = mOlmDevice.getSessionId(deviceKey)

            if (!TextUtils.isEmpty(sessionId)) {
                Timber.d("Using sessionid $sessionId for device $deviceKey")
                val deviceInfo = deviceInfoParticipantKey[deviceKey]

                payloadJson["recipient"] = deviceInfo!!.userId

                val recipientsKeysMap = HashMap<String, String>()
                recipientsKeysMap["ed25519"] = deviceInfo.fingerprint()!!
                payloadJson["recipient_keys"] = recipientsKeysMap

                // FIXME We have to canonicalize the JSON
                //JsonUtility.canonicalize(JsonUtility.getGson(false).toJsonTree(payloadJson)).toString()

                val payloadString = convertToUTF8(MoshiProvider.getCanonicalJson(Map::class.java, payloadJson))
                ciphertext[deviceKey] = mOlmDevice.encryptMessage(deviceKey, sessionId!!, payloadString!!)!!
            }
        }

        val res = EncryptedMessage()

        res.algorithm = MXCRYPTO_ALGORITHM_OLM
        res.senderKey = mOlmDevice.deviceCurve25519Key
        res.cipherText = ciphertext

        return res
    }

    /**
     * Sign Object
     *
     * Example:
     * <pre>
     *     {
     *         "[MY_USER_ID]": {
     *             "ed25519:[MY_DEVICE_ID]": "sign(str)"
     *         }
     *     }
     * </pre>
     *
     * @param strToSign the String to sign and to include in the Map
     * @return a Map (see example)
     */
    override fun signObject(strToSign: String): Map<String, Map<String, String>> {
        val result = HashMap<String, Map<String, String>>()

        val content = HashMap<String, String>()

        content["ed25519:" + myDevice.deviceId] = mOlmDevice.signMessage(strToSign)!!

        result[myDevice.userId] = content

        return result
    }

    /**
     * Handle the 'toDevice' event
     *
     * @param event the event
     */
    fun onToDeviceEvent(event: Event) {
        if (event.type == EventType.ROOM_KEY || event.type == EventType.FORWARDED_ROOM_KEY) {
            getDecryptingThreadHandler().post {
                onRoomKeyEvent(event)
            }
        } else if (event.type == EventType.ROOM_KEY_REQUEST) {
            encryptingThreadHandler.post {
                mIncomingRoomKeyRequestManager.onRoomKeyRequestEvent(event)
            }
        }
    }

    /**
     * Handle a key event.
     * This method must be called on getDecryptingThreadHandler() thread.
     *
     * @param event the key event.
     */
    private fun onRoomKeyEvent(event: Event?) {
        // sanity check
        if (null == event) {
            Timber.e("## onRoomKeyEvent() : null event")
            return
        }

        val roomKeyContent = event.content.toModel<RoomKeyContent>()!!

        if (TextUtils.isEmpty(roomKeyContent.roomId) || TextUtils.isEmpty(roomKeyContent.algorithm)) {
            Timber.e("## onRoomKeyEvent() : missing fields")
            return
        }

        val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(this, roomKeyContent.roomId, roomKeyContent.algorithm)

        if (null == alg) {
            Timber.e("## onRoomKeyEvent() : Unable to handle keys for " + roomKeyContent.algorithm!!)
            return
        }

        alg.onRoomKeyEvent(event)
    }

    /**
     * Handle an m.room.encryption event.
     *
     * @param event the encryption event.
     */
    private fun onRoomEncryptionEvent(roomId: String, event: Event) {
        // TODO Parse the event
        val eventContent = event.content // wireEventContent

        val room = mRoomService.getRoom(roomId)!!

        // Check whether the event content must be encrypted for the invited members.
        val encryptForInvitedMembers = mCryptoConfig!!.mEnableEncryptionForInvitedMembers && room.shouldEncryptForInvitedMembers()

        val userIds = if (encryptForInvitedMembers) {
            room.getActiveRoomMemberIds()
        } else {
            room.getJoinedRoomMemberIds()
        }

        encryptingThreadHandler.post { setEncryptionInRoom(roomId, eventContent!!["algorithm"] as String, true, userIds) }
    }

    /**
     * Handle a change in the membership state of a member of a room.
     *
     * @param event the membership event causing the change
     */
    private fun onRoomMembershipEvent(roomId: String, event: Event) {
        val alg: IMXEncrypting?

        synchronized(mRoomEncryptors) {
            alg = mRoomEncryptors[roomId]
        }

        if (null == alg) {
            // No encrypting in this room
            return
        }

        val userId = event.stateKey!!
        val room = mRoomService.getRoom(roomId)

        val roomMember = room?.getRoomMember(userId)

        if (null != roomMember) {
            val membership = roomMember.membership

            encryptingThreadHandler.post {
                if (membership == Membership.JOIN) {
                    // make sure we are tracking the deviceList for this user.
                    deviceListManager.startTrackingDeviceList(Arrays.asList(userId))
                } else if (membership == Membership.INVITE
                        && room.shouldEncryptForInvitedMembers()
                        && mCryptoConfig!!.mEnableEncryptionForInvitedMembers) {
                    // track the deviceList for this invited user.
                    // Caution: there's a big edge case here in that federated servers do not
                    // know what other servers are in the room at the time they've been invited.
                    // They therefore will not send device updates if a user logs in whilst
                    // their state is invite.
                    deviceListManager.startTrackingDeviceList(Arrays.asList(userId))
                }
            }
        }
    }

    /**
     * Upload my user's device keys.
     * This method must called on getEncryptingThreadHandler() thread.
     * The callback will called on UI thread.
     *
     * @param callback the asynchronous callback
     */
    private fun uploadDeviceKeys(callback: MatrixCallback<KeysUploadResponse>) {
        // Prepare the device keys data to send
        // Sign it
        val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, myDevice.signalableJSONDictionary())

        myDevice.signatures = signObject(canonicalJson)

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        mUploadKeysTask
                .configureWith(UploadKeysTask.Params(myDevice.toDeviceKeys(), null, myDevice.deviceId))
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    /**
     * OTK upload loop
     *
     * @param keyCount the number of key to generate
     * @param keyLimit the limit
     * @param callback the asynchronous callback
     */
    private fun uploadLoop(keyCount: Int, keyLimit: Int, callback: MatrixCallback<Unit>) {
        if (keyLimit <= keyCount) {
            // If we don't need to generate any more keys then we are done.
            mUIHandler.post { callback.onSuccess(Unit) }
            return
        }

        val keysThisLoop = Math.min(keyLimit - keyCount, ONE_TIME_KEY_GENERATION_MAX_NUMBER)

        mOlmDevice.generateOneTimeKeys(keysThisLoop)

        uploadOneTimeKeys(object : MatrixCallback<KeysUploadResponse> {
            override fun onSuccess(data: KeysUploadResponse) {
                encryptingThreadHandler.post {
                    if (data.hasOneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)) {
                        uploadLoop(data.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE), keyLimit, callback)
                    } else {
                        Timber.e("## uploadLoop() : response for uploading keys does not contain one_time_key_counts.signed_curve25519")
                        mUIHandler.post {
                            callback.onFailure(
                                    Exception("response for uploading keys does not contain one_time_key_counts.signed_curve25519"))
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
     * Check if the OTK must be uploaded.
     *
     * @param callback the asynchronous callback
     */
    private fun maybeUploadOneTimeKeys(callback: MatrixCallback<Unit>? = null) {
        if (mOneTimeKeyCheckInProgress) {
            mUIHandler.post {
                callback?.onSuccess(Unit)
            }
            return
        }

        if (System.currentTimeMillis() - mLastOneTimeKeyCheck < ONE_TIME_KEY_UPLOAD_PERIOD) {
            // we've done a key upload recently.
            mUIHandler.post {
                callback?.onSuccess(Unit)
            }
            return
        }

        mLastOneTimeKeyCheck = System.currentTimeMillis()

        mOneTimeKeyCheckInProgress = true

        // We then check how many keys we can store in the Account object.
        val maxOneTimeKeys = mOlmDevice.getMaxNumberOfOneTimeKeys()

        // Try to keep at most half that number on the server. This leaves the
        // rest of the slots free to hold keys that have been claimed from the
        // server but we haven't received a message for.
        // If we run out of slots when generating new keys then olm will
        // discard the oldest private keys first. This will eventually clean
        // out stale private keys that won't receive a message.
        val keyLimit = Math.floor(maxOneTimeKeys / 2.0).toInt()

        if (null != mOneTimeKeyCount) {
            uploadOTK(mOneTimeKeyCount!!, keyLimit, callback)
        } else {
            // ask the server how many keys we have
            mUploadKeysTask
                    .configureWith(UploadKeysTask.Params(null, null, myDevice.deviceId))
                    .dispatchTo(object : MatrixCallback<KeysUploadResponse> {

                        override fun onSuccess(data: KeysUploadResponse) {
                            encryptingThreadHandler.post {
                                if (!hasBeenReleased()) {
                                    // We need to keep a pool of one time public keys on the server so that
                                    // other devices can start conversations with us. But we can only store
                                    // a finite number of private keys in the olm Account object.
                                    // To complicate things further then can be a delay between a device
                                    // claiming a public one time key from the server and it sending us a
                                    // message. We need to keep the corresponding private key locally until
                                    // we receive the message.
                                    // But that message might never arrive leaving us stuck with duff
                                    // private keys clogging up our local storage.
                                    // So we need some kind of enginering compromise to balance all of
                                    // these factors. // TODO Why we do not set mOneTimeKeyCount here?
                                    val keyCount = data.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)
                                    uploadOTK(keyCount, keyLimit, callback)
                                }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "## uploadKeys() : failed")

                            mOneTimeKeyCount = null
                            mOneTimeKeyCheckInProgress = false

                            mUIHandler.post {
                                callback?.onFailure(failure)
                            }
                        }
                    })
                    .executeBy(mTaskExecutor)
        }
    }

    /**
     * Upload some the OTKs.
     *
     * @param keyCount the key count
     * @param keyLimit the limit
     * @param callback the asynchronous callback
     */
    private fun uploadOTK(keyCount: Int, keyLimit: Int, callback: MatrixCallback<Unit>?) {
        uploadLoop(keyCount, keyLimit, object : MatrixCallback<Unit> {
            private fun uploadKeysDone(errorMessage: String?) {
                if (null != errorMessage) {
                    Timber.e("## maybeUploadOneTimeKeys() : failed $errorMessage")
                }
                mOneTimeKeyCount = null
                mOneTimeKeyCheckInProgress = false
            }

            override fun onSuccess(data: Unit) {
                Timber.d("## maybeUploadOneTimeKeys() : succeeded")
                uploadKeysDone(null)

                mUIHandler.post {
                    callback?.onSuccess(Unit)
                }
            }

            override fun onFailure(failure: Throwable) {
                uploadKeysDone(failure.message)

                mUIHandler.post {
                    callback?.onFailure(failure)
                }
            }
        })

    }

    /**
     * Upload my user's one time keys.
     * This method must called on getEncryptingThreadHandler() thread.
     * The callback will called on UI thread.
     *
     * @param callback the asynchronous callback
     */
    private fun uploadOneTimeKeys(callback: MatrixCallback<KeysUploadResponse>?) {
        val oneTimeKeys = mOlmDevice.getOneTimeKeys()
        val oneTimeJson = HashMap<String, Any>()

        val curve25519Map = oneTimeKeys!![OlmAccount.JSON_KEY_ONE_TIME_KEY]

        if (null != curve25519Map) {
            for (key_id in curve25519Map.keys) {
                val k = HashMap<String, Any>()
                k["key"] = curve25519Map[key_id]!!

                // the key is also signed
                val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, k)

                k["signatures"] = signObject(canonicalJson)

                oneTimeJson["signed_curve25519:$key_id"] = k
            }
        }

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        mUploadKeysTask
                .configureWith(UploadKeysTask.Params(null, oneTimeJson, myDevice.deviceId))
                .dispatchTo(object : MatrixCallback<KeysUploadResponse> {
                    override fun onSuccess(data: KeysUploadResponse) {
                        encryptingThreadHandler.post {
                            if (!hasBeenReleased()) {
                                mLastPublishedOneTimeKeys = oneTimeKeys
                                mOlmDevice.markKeysAsPublished()

                                if (null != callback) {
                                    mUIHandler.post { callback.onSuccess(data) }
                                }
                            }
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        if (null != callback) {
                            mUIHandler.post { callback.onFailure(failure) }
                        }

                    }
                })
                .executeBy(mTaskExecutor)
    }


    /**
     * Export the crypto keys
     *
     * @param password the password
     * @param callback the exported keys
     */
    override fun exportRoomKeys(password: String, callback: MatrixCallback<ByteArray>) {
        exportRoomKeys(password, MXMegolmExportEncryption.DEFAULT_ITERATION_COUNT, callback)
    }

    /**
     * Export the crypto keys
     *
     * @param password         the password
     * @param anIterationCount the encryption iteration count (0 means no encryption)
     * @param callback         the exported keys
     */
    fun exportRoomKeys(password: String, anIterationCount: Int, callback: MatrixCallback<ByteArray>) {
        val iterationCount = Math.max(0, anIterationCount)

        getDecryptingThreadHandler().post(Runnable {
            val exportedSessions = ArrayList<MegolmSessionData>()

            val inboundGroupSessions = mCryptoStore.getInboundGroupSessions()

            for (session in inboundGroupSessions) {
                val megolmSessionData = session.exportKeys()

                if (null != megolmSessionData) {
                    exportedSessions.add(megolmSessionData)
                }
            }

            val encryptedRoomKeys: ByteArray

            try {
                val moshi = MoshiProvider.providesMoshi()
                val adapter = moshi.adapter(List::class.java)

                encryptedRoomKeys = MXMegolmExportEncryption
                        .encryptMegolmKeyFile(adapter.toJson(exportedSessions), password, iterationCount)
            } catch (e: Exception) {
                callback.onFailure(e)
                return@Runnable
            }

            mUIHandler.post { callback.onSuccess(encryptedRoomKeys) }
        })
    }

    /**
     * Import the room keys
     *
     * @param roomKeysAsArray  the room keys as array.
     * @param password         the password
     * @param progressListener the progress listener
     * @param callback         the asynchronous callback.
     */
    override fun importRoomKeys(roomKeysAsArray: ByteArray,
                                password: String,
                                progressListener: ProgressListener?,
                                callback: MatrixCallback<ImportRoomKeysResult>) {
        getDecryptingThreadHandler().post(Runnable {
            Timber.d("## importRoomKeys starts")

            val t0 = System.currentTimeMillis()
            val roomKeys: String

            try {
                roomKeys = MXMegolmExportEncryption.decryptMegolmKeyFile(roomKeysAsArray, password)
            } catch (e: Exception) {
                mUIHandler.post { callback.onFailure(e) }
                return@Runnable
            }

            val importedSessions: List<MegolmSessionData>

            val t1 = System.currentTimeMillis()

            Timber.d("## importRoomKeys : decryptMegolmKeyFile done in " + (t1 - t0) + " ms")

            try {
                val moshi = MoshiProvider.providesMoshi()
                val adapter = moshi.adapter(List::class.java)
                val list = adapter.fromJson(roomKeys)
                importedSessions = list as List<MegolmSessionData>
            } catch (e: Exception) {
                Timber.e(e, "## importRoomKeys failed")
                mUIHandler.post { callback.onFailure(e) }
                return@Runnable
            }

            val t2 = System.currentTimeMillis()

            Timber.d("## importRoomKeys : JSON parsing " + (t2 - t1) + " ms")

            importMegolmSessionsData(importedSessions, true, progressListener, callback)
        })
    }

    /**
     * Import a list of megolm session keys.
     *
     * @param megolmSessionsData megolm sessions.
     * @param backUpKeys         true to back up them to the homeserver.
     * @param progressListener   the progress listener
     * @param callback
     */
    override fun importMegolmSessionsData(megolmSessionsData: List<MegolmSessionData>,
                                          backUpKeys: Boolean,
                                          progressListener: ProgressListener?,
                                          callback: MatrixCallback<ImportRoomKeysResult>) {
        getDecryptingThreadHandler().post {
            val t0 = System.currentTimeMillis()

            val totalNumbersOfKeys = megolmSessionsData.size
            var cpt = 0
            var lastProgress = 0
            var totalNumbersOfImportedKeys = 0

            if (progressListener != null) {
                mUIHandler.post { progressListener.onProgress(0, 100) }
            }

            val sessions = mOlmDevice.importInboundGroupSessions(megolmSessionsData)

            for (megolmSessionData in megolmSessionsData) {
                cpt++


                val decrypting = roomDecryptorProvider.getOrCreateRoomDecryptor(this, megolmSessionData.roomId, megolmSessionData.algorithm)

                if (null != decrypting) {
                    try {
                        val sessionId = megolmSessionData.sessionId
                        Timber.d("## importRoomKeys retrieve mSenderKey " + megolmSessionData.senderKey + " sessionId " + sessionId)

                        totalNumbersOfImportedKeys++

                        // cancel any outstanding room key requests for this session
                        val roomKeyRequestBody = RoomKeyRequestBody()

                        roomKeyRequestBody.algorithm = megolmSessionData.algorithm
                        roomKeyRequestBody.roomId = megolmSessionData.roomId
                        roomKeyRequestBody.senderKey = megolmSessionData.senderKey
                        roomKeyRequestBody.sessionId = megolmSessionData.sessionId

                        cancelRoomKeyRequest(roomKeyRequestBody)

                        // Have another go at decrypting events sent with this session
                        decrypting.onNewSession(megolmSessionData.senderKey!!, sessionId!!)
                    } catch (e: Exception) {
                        Timber.e(e, "## importRoomKeys() : onNewSession failed")
                    }
                }

                if (progressListener != null) {
                    val progress = 100 * cpt / totalNumbersOfKeys

                    if (lastProgress != progress) {
                        lastProgress = progress

                        mUIHandler.post { progressListener.onProgress(progress, 100) }
                    }
                }
            }

            // Do not back up the key if it comes from a backup recovery
            if (backUpKeys) {
                mKeysBackup.maybeBackupKeys()
            } else {
                mCryptoStore.markBackupDoneForInboundGroupSessions(sessions)
            }

            val t1 = System.currentTimeMillis()

            Timber.d("## importMegolmSessionsData : sessions import " + (t1 - t0) + " ms (" + megolmSessionsData.size + " sessions)")

            val finalTotalNumbersOfImportedKeys = totalNumbersOfImportedKeys

            mUIHandler.post { callback.onSuccess(ImportRoomKeysResult(totalNumbersOfKeys, finalTotalNumbersOfImportedKeys)) }
        }
    }

    /**
     * Tells if the encryption must fail if some unknown devices are detected.
     *
     * @return true to warn when some unknown devices are detected.
     */
    fun warnOnUnknownDevices(): Boolean {
        return mWarnOnUnknownDevices
    }

    /**
     * Update the warn status when some unknown devices are detected.
     *
     * @param warn true to warn when some unknown devices are detected.
     */
    override fun setWarnOnUnknownDevices(warn: Boolean) {
        mWarnOnUnknownDevices = warn
    }

    /**
     * Check if the user ids list have some unknown devices.
     * A success means there is no unknown devices.
     * If there are some unknown devices, a MXCryptoError.UNKNOWN_DEVICES_CODE exception is triggered.
     *
     * @param userIds  the user ids list
     * @param callback the asynchronous callback.
     */
    fun checkUnknownDevices(userIds: List<String>, callback: MatrixCallback<Unit>) {
        // force the refresh to ensure that the devices list is up-to-date
        deviceListManager.downloadKeys(userIds, true, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                val unknownDevices = getUnknownDevices(data)

                if (unknownDevices.map.size == 0) {
                    callback.onSuccess(Unit)
                } else {
                    // trigger an an unknown devices exception
                    callback.onFailure(
                            Failure.CryptoError(MXCryptoError(MXCryptoError.UNKNOWN_DEVICES_CODE,
                                    MXCryptoError.UNABLE_TO_ENCRYPT, MXCryptoError.UNKNOWN_DEVICES_REASON, unknownDevices)))
                }
            }

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
    }

    /**
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block    true to unilaterally blacklist all
     * @param callback the asynchronous callback.
     */
    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean, callback: MatrixCallback<Unit>?) {
        encryptingThreadHandler.post {
            mCryptoStore.setGlobalBlacklistUnverifiedDevices(block)
            mUIHandler.post {
                callback?.onSuccess(Unit)
            }
        }
    }

    /**
     * Tells whether the client should ever send encrypted messages to unverified devices.
     * The default value is false.
     * messages to unverified devices.
     *
     * @param callback the asynchronous callback
     */
    override fun getGlobalBlacklistUnverifiedDevices(callback: MatrixCallback<Boolean>?) {
        encryptingThreadHandler.post {
            if (null != callback) {
                val status = globalBlacklistUnverifiedDevices

                mUIHandler.post { callback.onSuccess(status) }
            }
        }
    }

    /**
     * Tells whether the client should encrypt messages only for the verified devices
     * in this room.
     * The default value is false.
     * This function must be called in the getEncryptingThreadHandler() thread.
     *
     * @param roomId the room id
     * @return true if the client should encrypt messages only for the verified devices.
     */
    fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean {
        return if (null != roomId) {
            mCryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(roomId)
        } else {
            false
        }
    }

    /**
     * Tells whether the client should encrypt messages only for the verified devices
     * in this room.
     * The default value is false.
     * This function must be called in the getEncryptingThreadHandler() thread.
     *
     * @param roomId   the room id
     * @param callback the asynchronous callback
     */
    override fun isRoomBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Boolean>?) {
        encryptingThreadHandler.post {
            val status = isRoomBlacklistUnverifiedDevices(roomId)

            mUIHandler.post {
                callback?.onSuccess(status)
            }
        }
    }

    /**
     * Manages the room black-listing for unverified devices.
     *
     * @param roomId   the room id
     * @param add      true to add the room id to the list, false to remove it.
     * @param callback the asynchronous callback
     */
    private fun setRoomBlacklistUnverifiedDevices(roomId: String, add: Boolean, callback: MatrixCallback<Unit>?) {
        val room = mRoomService.getRoom(roomId)

        // sanity check
        if (null == room) {
            mUIHandler.post { callback!!.onSuccess(Unit) }

            return
        }

        encryptingThreadHandler.post {
            val roomIds = mCryptoStore.getRoomsListBlacklistUnverifiedDevices().toMutableList()

            if (add) {
                if (!roomIds.contains(roomId)) {
                    roomIds.add(roomId)
                }
            } else {
                roomIds.remove(roomId)
            }

            mCryptoStore.setRoomsListBlacklistUnverifiedDevices(roomIds)

            mUIHandler.post {
                callback?.onSuccess(Unit)
            }
        }
    }


    /**
     * Add this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId   the room id
     * @param callback the asynchronous callback
     */
    override fun setRoomBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Unit>) {
        setRoomBlacklistUnverifiedDevices(roomId, true, callback)
    }

    /**
     * Remove this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId   the room id
     * @param callback the asynchronous callback
     */
    override fun setRoomUnBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Unit>) {
        setRoomBlacklistUnverifiedDevices(roomId, false, callback)
    }

    /**
     * Send a request for some room keys, if we have not already done so.
     *
     * @param requestBody requestBody
     * @param recipients  recipients
     */
    fun requestRoomKey(requestBody: RoomKeyRequestBody, recipients: List<Map<String, String>>) {
        encryptingThreadHandler.post { mOutgoingRoomKeyRequestManager.sendRoomKeyRequest(requestBody, recipients) }
    }

    /**
     * Cancel any earlier room key request
     *
     * @param requestBody requestBody
     */
    override fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        encryptingThreadHandler.post { mOutgoingRoomKeyRequestManager.cancelRoomKeyRequest(requestBody) }
    }

    /**
     * Re request the encryption keys required to decrypt an event.
     *
     * @param event the event to decrypt again.
     */
    override fun reRequestRoomKeyForEvent(event: Event) {
        val wireContent = event.content!! // Wireeventcontent?

        val algorithm = wireContent["algorithm"].toString()
        val senderKey = wireContent["sender_key"].toString()
        val sessionId = wireContent["session_id"].toString()

        encryptingThreadHandler.post {
            val requestBody = RoomKeyRequestBody()

            requestBody.roomId = event.roomId
            requestBody.algorithm = algorithm
            requestBody.senderKey = senderKey
            requestBody.sessionId = sessionId

            mOutgoingRoomKeyRequestManager.resendRoomKeyRequest(requestBody)
        }
    }

    /**
     * Add a RoomKeysRequestListener listener.
     *
     * @param listener listener
     */
    override fun addRoomKeysRequestListener(listener: RoomKeysRequestListener) {
        mIncomingRoomKeyRequestManager.addRoomKeysRequestListener(listener)
    }

    /**
     * Add a RoomKeysRequestListener listener.
     *
     * @param listener listener
     */
    fun removeRoomKeysRequestListener(listener: RoomKeysRequestListener) {
        mIncomingRoomKeyRequestManager.removeRoomKeysRequestListener(listener)
    }

    /* ==========================================================================================
     * DEBUG INFO
     * ========================================================================================== */

    override fun toString(): String {
        return myDevice.userId + " (" + myDevice.deviceId + ")"

    }

    companion object {
        // max number of keys to upload at once
        // Creating keys can be an expensive operation so we limit the
        // number we generate in one go to avoid blocking the application
        // for too long.
        private const val ONE_TIME_KEY_GENERATION_MAX_NUMBER = 5

        // frequency with which to check & upload one-time keys
        private const val ONE_TIME_KEY_UPLOAD_PERIOD = (60 * 1000).toLong() // one minute

        /**
         * Provides the list of unknown devices
         *
         * @param devicesInRoom the devices map
         * @return the unknown devices map
         */
        fun getUnknownDevices(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>): MXUsersDevicesMap<MXDeviceInfo> {
            val unknownDevices = MXUsersDevicesMap<MXDeviceInfo>()

            val userIds = devicesInRoom.userIds
            for (userId in userIds) {
                val deviceIds = devicesInRoom.getUserDeviceIds(userId)
                for (deviceId in deviceIds!!) {
                    val deviceInfo = devicesInRoom.getObject(deviceId, userId)

                    if (deviceInfo!!.isUnknown) {
                        unknownDevices.setObject(deviceInfo, userId, deviceId)
                    }
                }
            }

            return unknownDevices
        }
    }
}
/**
 * Check if the OTK must be uploaded.
 */