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
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibilityContent
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import im.vector.matrix.android.internal.crypto.actions.MegolmSessionDataImporter
import im.vector.matrix.android.internal.crypto.actions.SetDeviceVerificationAction
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmEncryptionFactory
import im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmEncryptionFactory
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXEncryptEventContentResult
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.event.RoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadResponse
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.repository.WarnOnUnknownDeviceRepository
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.DeleteDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.GetDevicesTask
import im.vector.matrix.android.internal.crypto.tasks.GetKeyChangesTask
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.crypto.tasks.SetDeviceNameTask
import im.vector.matrix.android.internal.crypto.tasks.UploadKeysTask
import im.vector.matrix.android.internal.crypto.verification.DefaultSasVerificationService
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import org.matrix.olm.OlmManager
import timber.log.Timber
import java.util.*

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
        private val mMyDeviceInfoHolder: MyDeviceInfoHolder,
        // the crypto store
        private val mCryptoStore: IMXCryptoStore,
        // Olm device
        private val mOlmDevice: MXOlmDevice,
        // Set of parameters used to configure/customize the end-to-end crypto.
        private val mCryptoConfig: MXCryptoConfig = MXCryptoConfig(),
        // Device list manager
        private val deviceListManager: DeviceListManager,
        // The key backup service.
        private val mKeysBackup: KeysBackup,
        //
        private val mObjectSigner: ObjectSigner,
        //
        private val mOneTimeKeysUploader: OneTimeKeysUploader,
        //
        private val roomDecryptorProvider: RoomDecryptorProvider,
        // The SAS verification service.
        private val mSasVerificationService: DefaultSasVerificationService,
        //
        private val mIncomingRoomKeyRequestManager: IncomingRoomKeyRequestManager,
        //
        private val mOutgoingRoomKeyRequestManager: OutgoingRoomKeyRequestManager,
        // Olm Manager
        private val mOlmManager: OlmManager,
        // Actions
        private val mSetDeviceVerificationAction: SetDeviceVerificationAction,
        private val mMegolmSessionDataImporter: MegolmSessionDataImporter,
        private val mEnsureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        // Repository
        private val mWarnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        private val mMXMegolmEncryptionFactory: MXMegolmEncryptionFactory,
        private val mMXOlmEncryptionFactory: MXOlmEncryptionFactory,
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
) : CryptoService {

    // MXEncrypting instance for each room.
    private val mRoomEncryptors: MutableMap<String, IMXEncrypting> = HashMap()

    // the encryption is starting
    private var mIsStarting: Boolean = false

    // tell if the crypto is started
    private var mIsStarted: Boolean = false

    // TODO
    //private val mNetworkListener = object : IMXNetworkEventListener {
    //    override fun onNetworkConnectionUpdate(isConnected: Boolean) {
    //        if (isConnected && !isStarted()) {
    //            Timber.v("Start MXCrypto because a network connection has been retrieved ")
    //            start(false, null)
    //        }
    //    }
    //}

    fun onStateEvent(roomId: String, event: Event) {
        if (event.type == EventType.ENCRYPTION) {
            // TODO Remove onRoomEncryptionEvent(roomId, event)
        } else if (event.type == EventType.STATE_ROOM_MEMBER) {
            onRoomMembershipEvent(roomId, event)
        } else if (event.type == EventType.STATE_HISTORY_VISIBILITY) {
            onRoomHistoryVisibilityEvent(roomId, event)
        }
    }

    fun onLiveEvent(roomId: String, event: Event) {
        if (event.type == EventType.ENCRYPTION) {
            // TODO Remove onRoomEncryptionEvent(roomId, event)
        } else if (event.type == EventType.STATE_ROOM_MEMBER) {
            onRoomMembershipEvent(roomId, event)
        } else if (event.type == EventType.STATE_HISTORY_VISIBILITY) {
            onRoomHistoryVisibilityEvent(roomId, event)
        }
    }

    // initialization callbacks
    private val mInitializationCallbacks = ArrayList<MatrixCallback<Unit>>()

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
        return mMyDeviceInfoHolder.myDevice
    }

    override fun getDevicesList(callback: MatrixCallback<DevicesListResponse>) {
        mGetDevicesTask
                .configureWith(Unit)
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return mCryptoStore.inboundGroupSessionsCount(onlyBackedUp)
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
     */
    fun start(isInitialSync: Boolean) {
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

        uploadDeviceKeys(object : MatrixCallback<KeysUploadResponse> {
            private fun onError() {
                Handler().postDelayed({
                                          if (!isStarted()) {
                                              mIsStarting = false
                                              start(isInitialSync)
                                          }
                                      }, 1000)
            }

            override fun onSuccess(data: KeysUploadResponse) {
                Timber.v("###########################################################")
                Timber.v("uploadDeviceKeys done for " + mCredentials.userId)
                Timber.v("  - device id  : " + mCredentials.deviceId)
                Timber.v("  - ed25519    : " + mOlmDevice.deviceEd25519Key)
                Timber.v("  - curve25519 : " + mOlmDevice.deviceCurve25519Key)
                Timber.v("  - oneTimeKeys: " + mOneTimeKeysUploader.mLastPublishedOneTimeKeys)
                Timber.v("")

                mOneTimeKeysUploader.maybeUploadOneTimeKeys(object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        // TODO
                        //if (null != mNetworkConnectivityReceiver) {
                        //    mNetworkConnectivityReceiver!!.removeEventListener(mNetworkListener)
                        //}

                        mIsStarting = false
                        mIsStarted = true

                        mOutgoingRoomKeyRequestManager.start()

                        mKeysBackup.checkAndStartKeysBackup()

                        if (isInitialSync) {
                            // refresh the devices list for each known room members
                            deviceListManager.invalidateAllDeviceLists()
                            deviceListManager.refreshOutdatedDeviceLists()
                        } else {
                            mIncomingRoomKeyRequestManager.processReceivedRoomKeyRequests()
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "## start failed")
                        onError()
                    }
                })
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## start failed")
                onError()
            }
        })
    }

    /**
     * Close the crypto
     */
    fun close() {
        mOlmDevice.release()

        mCryptoStore.close()

        mOutgoingRoomKeyRequestManager.stop()
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
        if (null != syncResponse.deviceLists) {
            deviceListManager.handleDeviceListsChanges(syncResponse.deviceLists.changed, syncResponse.deviceLists.left)
        }

        if (null != syncResponse.deviceOneTimeKeysCount) {
            val currentCount = syncResponse.deviceOneTimeKeysCount.signedCurve25519 ?: 0
            mOneTimeKeysUploader.updateOneTimeKeyCount(currentCount)
        }

        if (isStarted()) {
            // Make sure we process to-device messages before generating new one-time-keys #2782
            deviceListManager.refreshOutdatedDeviceLists()
        }

        if (!isCatchingUp && isStarted()) {
            mOneTimeKeysUploader.maybeUploadOneTimeKeys()

            mIncomingRoomKeyRequestManager.processReceivedRoomKeyRequests()
        }
    }

    /**
     * Find a device by curve25519 identity key
     *
     * @param senderKey the curve25519 key to match.
     * @param algorithm the encryption algorithm.
     * @return the device info, or null if not found / unsupported algorithm / crypto released
     */
    override fun deviceWithIdentityKey(senderKey: String, algorithm: String): MXDeviceInfo? {
        return if (!TextUtils.equals(algorithm, MXCRYPTO_ALGORITHM_MEGOLM) && !TextUtils.equals(algorithm, MXCRYPTO_ALGORITHM_OLM)) {
            // We only deal in olm keys
            null
        } else mCryptoStore.deviceWithIdentityKey(senderKey)
    }

    /**
     * Provides the device information for a device id and a user Id
     *
     * @param userId   the user id
     * @param deviceId the device id
     */
    override fun getDeviceInfo(userId: String, deviceId: String?): MXDeviceInfo? {
        return if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(deviceId)) {
            mCryptoStore.getUserDevice(deviceId!!, userId)
        } else {
            null
        }
    }

    /**
     * Set the devices as known
     *
     * @param devices  the devices. Note that the mVerified member of the devices in this list will not be updated by this method.
     * @param callback the asynchronous callback
     */
    override fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?) {
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


        callback?.onSuccess(Unit)
    }

    /**
     * Update the blocked/verified state of the given device.
     *
     * @param verificationStatus the new verification status
     * @param deviceId           the unique identifier for the device.
     * @param userId             the owner of the device
     */
    override fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String) {
        mSetDeviceVerificationAction.handle(verificationStatus, deviceId, userId)
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
        // If we already have encryption in this room, we should ignore this event
        // (for now at least. Maybe we should alert the user somehow?)
        val existingAlgorithm = mCryptoStore.getRoomAlgorithm(roomId)

        if (!TextUtils.isEmpty(existingAlgorithm) && !TextUtils.equals(existingAlgorithm, algorithm)) {
            Timber.e("## setEncryptionInRoom() : Ignoring m.room.encryption event which requests a change of config in $roomId")
            return false
        }

        val encryptingClass = MXCryptoAlgorithms.hasEncryptorClassForAlgorithm(algorithm)

        if (!encryptingClass) {
            Timber.e("## setEncryptionInRoom() : Unable to encrypt with " + algorithm!!)
            return false
        }

        mCryptoStore.storeRoomAlgorithm(roomId, algorithm!!)

        val alg: IMXEncrypting = when (algorithm) {
            MXCRYPTO_ALGORITHM_MEGOLM -> mMXMegolmEncryptionFactory.instantiate(roomId)
            else                      -> mMXOlmEncryptionFactory.instantiate(roomId)
        }

        synchronized(mRoomEncryptors) {
            mRoomEncryptors.put(roomId, alg)
        }

        // if encryption was not previously enabled in this room, we will have been
        // ignoring new device events for these users so far. We may well have
        // up-to-date lists for some users, for instance if we were sharing other
        // e2e rooms with them, so there is room for optimisation here, but for now
        // we just invalidate everyone in the room.
        if (null == existingAlgorithm) {
            Timber.v("Enabling encryption in $roomId for the first time; invalidating device lists for all users therein")

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
    override fun isRoomEncrypted(roomId: String): Boolean {
        var res: Boolean

        synchronized(mRoomEncryptors) {
            res = mRoomEncryptors.containsKey(roomId)
        }

        if (!res) {
            res = !mCryptoStore.getRoomAlgorithm(roomId).isNullOrBlank()
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

    // TODO Remove ?
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
        mEnsureOlmSessionsForDevicesAction.handle(devicesByUser, callback)
    }

    fun isEncryptionEnabledForInvitedUser(): Boolean {
        return mCryptoConfig.mEnableEncryptionForInvitedMembers
    }

    override fun getEncryptionAlgorithm(roomId: String): String? {
        return mCryptoStore.getRoomAlgorithm(roomId)
    }

    /**
     * Determine whether we should encrypt messages for invited users in this room.
     * <p>
     * Check here whether the invited members are allowed to read messages in the room history
     * from the point they were invited onwards.
     *
     * @return true if we should encrypt messages for invited users.
     */
    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return mCryptoStore.shouldEncryptForInvitedMembers(roomId)
    }

    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType    the type of the event.
     * @param room         the room the event will be sent.
     * @param callback     the asynchronous callback
     */
    override fun encryptEventContent(eventContent: Content,
                                     eventType: String,
                                     room: Room,
                                     callback: MatrixCallback<MXEncryptEventContentResult>) {
        // wait that the crypto is really started
        if (!isStarted()) {
            Timber.v("## encryptEventContent() : wait after e2e init")

            start(false)

            return
        }

        // Check whether the event content must be encrypted for the invited members.
        val encryptForInvitedMembers = mCryptoConfig.mEnableEncryptionForInvitedMembers && shouldEncryptForInvitedMembers(room.roomId)

        // TODO
        //val userIds = if (encryptForInvitedMembers) {
        //    room.getActiveRoomMemberIds()
        //} else {
        //    room.getJoinedRoomMemberIds()
        //}
        val userIds = emptyList<String>()

        // just as you are sending a secret message?

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
            Timber.v("## encryptEventContent() starts")

            alg!!.encryptEventContent(eventContent, eventType, userIds, object : MatrixCallback<Content> {
                override fun onSuccess(data: Content) {
                    Timber.v("## encryptEventContent() : succeeds after " + (System.currentTimeMillis() - t0) + " ms")

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

            callback.onFailure(Failure.CryptoError(MXCryptoError(MXCryptoError.UNABLE_TO_ENCRYPT_ERROR_CODE,
                                                                 MXCryptoError.UNABLE_TO_ENCRYPT, reason)))
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
        val exceptions = ArrayList<MXDecryptionException>()

        var result: MXEventDecryptionResult? = null
        val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(event.roomId, eventContent["algorithm"] as String)

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
                results.add(result) // TODO simplify
            }
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
        mOlmDevice.resetReplayAttackCheckInTimeline(timelineId)
    }

    /**
     * Handle the 'toDevice' event
     *
     * @param event the event
     */
    fun onToDeviceEvent(event: Event) {
        if (event.type == EventType.ROOM_KEY || event.type == EventType.FORWARDED_ROOM_KEY) {
            onRoomKeyEvent(event)
        } else if (event.type == EventType.ROOM_KEY_REQUEST) {
            mIncomingRoomKeyRequestManager.onRoomKeyRequestEvent(event)
        }
    }

    /**
     * Handle a key event.
     * This method must be called on getDecryptingThreadHandler() thread.
     *
     * @param event the key event.
     */
    private fun onRoomKeyEvent(event: Event) {
        val roomKeyContent = event.content.toModel<RoomKeyContent>()!!

        if (TextUtils.isEmpty(roomKeyContent.roomId) || TextUtils.isEmpty(roomKeyContent.algorithm)) {
            Timber.e("## onRoomKeyEvent() : missing fields")
            return
        }

        val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(roomKeyContent.roomId, roomKeyContent.algorithm)

        if (null == alg) {
            Timber.e("## onRoomKeyEvent() : Unable to handle keys for " + roomKeyContent.algorithm)
            return
        }

        alg.onRoomKeyEvent(event, mKeysBackup)
    }

    /**
     * Handle an m.room.encryption event.
     *
     * @param event the encryption event.
     */
    fun onRoomEncryptionEvent(event: Event, userIds: List<String>) {
        setEncryptionInRoom(event.roomId!!, event.content!!["algorithm"] as String, true, userIds)
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
        event.stateKey?.let { userId ->
            val roomMember: RoomMember? = event.content.toModel()
            val membership = roomMember?.membership
            if (membership == Membership.JOIN) {
                // make sure we are tracking the deviceList for this user.
                deviceListManager.startTrackingDeviceList(Arrays.asList(userId))
            } else if (membership == Membership.INVITE
                       && shouldEncryptForInvitedMembers(roomId)
                       && mCryptoConfig.mEnableEncryptionForInvitedMembers) {
                // track the deviceList for this invited user.
                // Caution: there's a big edge case here in that federated servers do not
                // know what other servers are in the room at the time they've been invited.
                // They therefore will not send device updates if a user logs in whilst
                // their state is invite.
                deviceListManager.startTrackingDeviceList(Arrays.asList(userId))
            }
        }
    }

    private fun onRoomHistoryVisibilityEvent(roomId: String, event: Event) {
        val eventContent = event.content.toModel<RoomHistoryVisibilityContent>()

        eventContent?.historyVisibility?.let {
            mCryptoStore.setShouldEncryptForInvitedMembers(roomId, it != RoomHistoryVisibility.JOINED)
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
        val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, getMyDevice().signalableJSONDictionary())

        getMyDevice().signatures = mObjectSigner.signObject(canonicalJson)

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        mUploadKeysTask
                .configureWith(UploadKeysTask.Params(getMyDevice().toDeviceKeys(), null, getMyDevice().deviceId))
                .executeOn(TaskThread.ENCRYPTION)
                .dispatchTo(callback)
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
            val adapter = MoshiProvider.providesMoshi()
                    .adapter(List::class.java)

            encryptedRoomKeys = MXMegolmExportEncryption
                    .encryptMegolmKeyFile(adapter.toJson(exportedSessions), password, iterationCount)
        } catch (e: Exception) {
            callback.onFailure(e)
            return
        }

        callback.onSuccess(encryptedRoomKeys)
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
        Timber.v("## importRoomKeys starts")

        val t0 = System.currentTimeMillis()
        val roomKeys: String

        try {
            roomKeys = MXMegolmExportEncryption.decryptMegolmKeyFile(roomKeysAsArray, password)
        } catch (e: Exception) {
            callback.onFailure(e)
            return
        }

        val importedSessions: List<MegolmSessionData>

        val t1 = System.currentTimeMillis()

        Timber.v("## importRoomKeys : decryptMegolmKeyFile done in " + (t1 - t0) + " ms")

        try {
            val list = MoshiProvider.providesMoshi()
                    .adapter(List::class.java)
                    .fromJson(roomKeys)
            importedSessions = list as List<MegolmSessionData>
        } catch (e: Exception) {
            Timber.e(e, "## importRoomKeys failed")
            callback.onFailure(e)
            return
        }

        val t2 = System.currentTimeMillis()

        Timber.v("## importRoomKeys : JSON parsing " + (t2 - t1) + " ms")

        mMegolmSessionDataImporter.handle(importedSessions, true, progressListener, callback)
    }

    /**
     * Update the warn status when some unknown devices are detected.
     *
     * @param warn true to warn when some unknown devices are detected.
     */
    override fun setWarnOnUnknownDevices(warn: Boolean) {
        mWarnOnUnknownDevicesRepository.setWarnOnUnknownDevices(warn)
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

                if (unknownDevices.map.isEmpty()) {
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
     */
    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        mCryptoStore.setGlobalBlacklistUnverifiedDevices(block)
    }

    /**
     * Tells whether the client should ever send encrypted messages to unverified devices.
     * The default value is false.
     * This function must be called in the getEncryptingThreadHandler() thread.
     *
     * @return true to unilaterally blacklist all unverified devices.
     */
    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return mCryptoStore.getGlobalBlacklistUnverifiedDevices()
    }

    /**
     * Tells whether the client should encrypt messages only for the verified devices
     * in this room.
     * The default value is false.
     *
     * @param roomId the room id
     * @return true if the client should encrypt messages only for the verified devices.
     */
    // TODO add this info in CryptoRoomEntity?
    override fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean {
        return if (null != roomId) {
            mCryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(roomId)
        } else {
            false
        }
    }

    /**
     * Manages the room black-listing for unverified devices.
     *
     * @param roomId   the room id
     * @param add      true to add the room id to the list, false to remove it.
     */
    private fun setRoomBlacklistUnverifiedDevices(roomId: String, add: Boolean) {
        val roomIds = mCryptoStore.getRoomsListBlacklistUnverifiedDevices().toMutableList()

        if (add) {
            if (!roomIds.contains(roomId)) {
                roomIds.add(roomId)
            }
        } else {
            roomIds.remove(roomId)
        }

        mCryptoStore.setRoomsListBlacklistUnverifiedDevices(roomIds)
    }


    /**
     * Add this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId   the room id
     */
    override fun setRoomBlacklistUnverifiedDevices(roomId: String) {
        setRoomBlacklistUnverifiedDevices(roomId, true)
    }

    /**
     * Remove this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId   the room id
     */
    override fun setRoomUnBlacklistUnverifiedDevices(roomId: String) {
        setRoomBlacklistUnverifiedDevices(roomId, false)
    }

    // TODO Check if this method is still necessary
    /**
     * Cancel any earlier room key request
     *
     * @param requestBody requestBody
     */
    override fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        mOutgoingRoomKeyRequestManager.cancelRoomKeyRequest(requestBody)
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

        val requestBody = RoomKeyRequestBody()

        requestBody.roomId = event.roomId
        requestBody.algorithm = algorithm
        requestBody.senderKey = senderKey
        requestBody.sessionId = sessionId

        mOutgoingRoomKeyRequestManager.resendRoomKeyRequest(requestBody)
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

    /**
     * Provides the list of unknown devices
     *
     * @param devicesInRoom the devices map
     * @return the unknown devices map
     */
    private fun getUnknownDevices(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>): MXUsersDevicesMap<MXDeviceInfo> {
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

    /* ==========================================================================================
     * DEBUG INFO
     * ========================================================================================== */

    override fun toString(): String {
        return "CryptoManager of " + mCredentials.userId + " (" + mCredentials.deviceId + ")"

    }
}
