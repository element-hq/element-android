/*
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

import android.text.TextUtils
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.KeysQueryResponse
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.DownloadKeysForUsersTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import java.util.*

// Legacy name: MXDeviceList
internal class DeviceListManager(private val cryptoStore: IMXCryptoStore,
                                 private val olmDevice: MXOlmDevice,
                                 private val syncTokenStore: SyncTokenStore,
                                 private val credentials: Credentials,
                                 private val downloadKeysForUsersTask: DownloadKeysForUsersTask,
                                 private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                 private val taskExecutor: TaskExecutor) {

    // keys in progress
    private val userKeyDownloadsInProgress = HashSet<String>()

    // HS not ready for retry
    private val notReadyToRetryHS = HashSet<String>()

    // indexed by UserId
    private val pendingDownloadKeysRequestToken = HashMap<String, String>()

    // pending queues list
    private val downloadKeysQueues = ArrayList<DownloadKeysPromise>()

    // tells if there is a download keys request in progress
    private var isDownloadingKeys = false

    /**
     * Creator
     *
     * @param userIds  the user ids list
     * @param callback the asynchronous callback
     */
    internal inner class DownloadKeysPromise(userIds: List<String>,
                                             val callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>?) {
        // list of remain pending device keys
        val mPendingUserIdsList: MutableList<String>

        // the unfiltered user ids list
        val mUserIdsList: List<String>

        init {
            mPendingUserIdsList = ArrayList(userIds)
            mUserIdsList = ArrayList(userIds)
        }
    }

    init {
        var isUpdated = false

        val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()
        for (userId in deviceTrackingStatuses.keys) {
            val status = deviceTrackingStatuses[userId]!!

            if (TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == status || TRACKING_STATUS_UNREACHABLE_SERVER == status) {
                // if a download was in progress when we got shut down, it isn't any more.
                deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD)
                isUpdated = true
            }
        }

        if (isUpdated) {
            cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
        }
    }

    /**
     * Tells if the key downloads should be tried
     *
     * @param userId the userId
     * @return true if the keys download can be retrieved
     */
    private fun canRetryKeysDownload(userId: String): Boolean {
        var res = false

        if (!TextUtils.isEmpty(userId) && userId.contains(":")) {
            try {
                synchronized(notReadyToRetryHS) {
                    res = !notReadyToRetryHS.contains(userId.substring(userId.lastIndexOf(":") + 1))
                }
            } catch (e: Exception) {
                Timber.e(e, "## canRetryKeysDownload() failed")
            }

        }

        return res
    }

    /**
     * Add a download keys promise
     *
     * @param userIds  the user ids list
     * @param callback the asynchronous callback
     * @return the filtered user ids list i.e the one which require a remote request
     */
    private fun addDownloadKeysPromise(userIds: MutableList<String>?, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>?): MutableList<String>? {
        if (null != userIds) {
            val filteredUserIds = ArrayList<String>()
            val invalidUserIds = ArrayList<String>()

            for (userId in userIds) {
                if (MatrixPatterns.isUserId(userId)) {
                    filteredUserIds.add(userId)
                } else {
                    Timber.e("## userId " + userId + "is not a valid user id")
                    invalidUserIds.add(userId)
                }
            }

            synchronized(userKeyDownloadsInProgress) {
                filteredUserIds.removeAll(userKeyDownloadsInProgress)
                userKeyDownloadsInProgress.addAll(userIds)
                // got some email addresses instead of matrix ids
                userKeyDownloadsInProgress.removeAll(invalidUserIds)
                userIds.removeAll(invalidUserIds)
            }

            downloadKeysQueues.add(DownloadKeysPromise(userIds, callback))

            return filteredUserIds
        } else {
            return null
        }
    }

    /**
     * Clear the unavailable server lists
     */
    private fun clearUnavailableServersList() {
        synchronized(notReadyToRetryHS) {
            notReadyToRetryHS.clear()
        }
    }

    /**
     * Mark the cached device list for the given user outdated
     * flag the given user for device-list tracking, if they are not already.
     *
     * @param userIds the user ids list
     */
    fun startTrackingDeviceList(userIds: List<String>?) {
        if (null != userIds) {
            var isUpdated = false
            val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()

            for (userId in userIds) {
                if (!deviceTrackingStatuses.containsKey(userId) || TRACKING_STATUS_NOT_TRACKED == deviceTrackingStatuses[userId]) {
                    Timber.v("## startTrackingDeviceList() : Now tracking device list for $userId")
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD)
                    isUpdated = true
                }
            }

            if (isUpdated) {
                cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
            }
        }
    }

    /**
     * Update the devices list statuses
     *
     * @param changed the user ids list which have new devices
     * @param left    the user ids list which left a room
     */
    fun handleDeviceListsChanges(changed: List<String>?, left: List<String>?) {
        var isUpdated = false
        val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()

        if (changed?.isNotEmpty() == true) {
            clearUnavailableServersList()

            for (userId in changed) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Timber.v("## invalidateUserDeviceList() : Marking device list outdated for $userId")
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD)
                    isUpdated = true
                }
            }
        }

        if (left?.isNotEmpty() == true) {
            clearUnavailableServersList()

            for (userId in left) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Timber.v("## invalidateUserDeviceList() : No longer tracking device list for $userId")
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_NOT_TRACKED)
                    isUpdated = true
                }
            }
        }

        if (isUpdated) {
            cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
        }
    }

    /**
     * This will flag each user whose devices we are tracking as in need of an
     * + update
     */
    fun invalidateAllDeviceLists() {
        handleDeviceListsChanges(ArrayList(cryptoStore.getDeviceTrackingStatuses().keys), null)
    }

    /**
     * The keys download failed
     *
     * @param userIds the user ids list
     */
    private fun onKeysDownloadFailed(userIds: List<String>?) {
        if (null != userIds) {
            synchronized(userKeyDownloadsInProgress) {
                val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()

                for (userId in userIds) {
                    userKeyDownloadsInProgress.remove(userId)
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD)
                }

                cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
            }
        }

        isDownloadingKeys = false
    }

    /**
     * The keys download succeeded.
     *
     * @param userIds  the userIds list
     * @param failures the failure map.
     */
    private fun onKeysDownloadSucceed(userIds: List<String>?, failures: Map<String, Map<String, Any>>?) {
        if (null != failures) {
            val keys = failures.keys

            for (k in keys) {
                val value = failures[k]

                if (value!!.containsKey("status")) {
                    val statusCodeAsVoid = value["status"]
                    var statusCode = 0

                    if (statusCodeAsVoid is Double) {
                        statusCode = statusCodeAsVoid.toInt()
                    } else if (statusCodeAsVoid is Int) {
                        statusCode = statusCodeAsVoid.toInt()
                    }

                    if (statusCode == 503) {
                        synchronized(notReadyToRetryHS) {
                            notReadyToRetryHS.add(k)
                        }
                    }
                }
            }
        }

        val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()

        if (null != userIds) {
            if (downloadKeysQueues.size > 0) {
                val promisesToRemove = ArrayList<DownloadKeysPromise>()

                for (promise in downloadKeysQueues) {
                    promise.mPendingUserIdsList.removeAll(userIds)

                    if (promise.mPendingUserIdsList.size == 0) {
                        // private members
                        val usersDevicesInfoMap = MXUsersDevicesMap<MXDeviceInfo>()

                        for (userId in promise.mUserIdsList) {
                            val devices = cryptoStore.getUserDevices(userId)
                            if (null == devices) {
                                if (canRetryKeysDownload(userId)) {
                                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD)
                                    Timber.e("failed to retry the devices of $userId : retry later")
                                } else {
                                    if (deviceTrackingStatuses.containsKey(userId) && TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses[userId]) {
                                        deviceTrackingStatuses.put(userId, TRACKING_STATUS_UNREACHABLE_SERVER)
                                        Timber.e("failed to retry the devices of $userId : the HS is not available")
                                    }
                                }
                            } else {
                                if (deviceTrackingStatuses.containsKey(userId) && TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses[userId]) {
                                    // we didn't get any new invalidations since this download started:
                                    //  this user's device list is now up to date.
                                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_UP_TO_DATE)
                                    Timber.v("Device list for $userId now up to date")
                                }

                                // And the response result
                                usersDevicesInfoMap.setObjects(devices, userId)
                            }
                        }

                        val callback = promise.callback

                        if (null != callback) {
                            CryptoAsyncHelper.getUiHandler().post { callback.onSuccess(usersDevicesInfoMap) }
                        }

                        promisesToRemove.add(promise)
                    }
                }
                downloadKeysQueues.removeAll(promisesToRemove)
            }

            for (userId in userIds) {
                userKeyDownloadsInProgress.remove(userId)
            }

            cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
        }

        isDownloadingKeys = false
    }

    /**
     * Download the device keys for a list of users and stores the keys in the MXStore.
     * It must be called in getEncryptingThreadHandler() thread.
     * The callback is called in the UI thread.
     *
     * @param userIds       The users to fetch.
     * @param forceDownload Always download the keys even if cached.
     * @param callback      the asynchronous callback
     */
    fun downloadKeys(userIds: List<String>?, forceDownload: Boolean, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>?) {
        Timber.v("## downloadKeys() : forceDownload $forceDownload : $userIds")

        // Map from userid -> deviceid -> DeviceInfo
        val stored = MXUsersDevicesMap<MXDeviceInfo>()

        // List of user ids we need to download keys for
        val downloadUsers = ArrayList<String>()

        if (null != userIds) {
            if (forceDownload) {
                downloadUsers.addAll(userIds)
            } else {
                for (userId in userIds) {
                    val status = cryptoStore.getDeviceTrackingStatus(userId, TRACKING_STATUS_NOT_TRACKED)

                    // downloading keys ->the keys download won't be triggered twice but the callback requires the dedicated keys
                    // not yet retrieved
                    if (userKeyDownloadsInProgress.contains(userId) || TRACKING_STATUS_UP_TO_DATE != status && TRACKING_STATUS_UNREACHABLE_SERVER != status) {
                        downloadUsers.add(userId)
                    } else {
                        val devices = cryptoStore.getUserDevices(userId)

                        // should always be true
                        if (null != devices) {
                            stored.setObjects(devices, userId)
                        } else {
                            downloadUsers.add(userId)
                        }
                    }
                }
            }
        }

        if (0 == downloadUsers.size) {
            Timber.v("## downloadKeys() : no new user device")

            if (null != callback) {
                CryptoAsyncHelper.getUiHandler().post { callback.onSuccess(stored) }
            }
        } else {
            Timber.v("## downloadKeys() : starts")
            val t0 = System.currentTimeMillis()

            doKeyDownloadForUsers(downloadUsers, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
                override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                    Timber.v("## downloadKeys() : doKeyDownloadForUsers succeeds after " + (System.currentTimeMillis() - t0) + " ms")

                    data.addEntriesFromMap(stored)

                    callback?.onSuccess(data)
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## downloadKeys() : doKeyDownloadForUsers onFailure")
                    callback?.onFailure(failure)
                }
            })
        }
    }

    /**
     * Download the devices keys for a set of users.
     * It must be called in getEncryptingThreadHandler() thread.
     * The callback is called in the UI thread.
     *
     * @param downloadUsers the user ids list
     * @param callback      the asynchronous callback
     */
    private fun doKeyDownloadForUsers(downloadUsers: MutableList<String>, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>?) {
        Timber.v("## doKeyDownloadForUsers() : doKeyDownloadForUsers $downloadUsers")

        // get the user ids which did not already trigger a keys download
        val filteredUsers = addDownloadKeysPromise(downloadUsers, callback)

        // if there is no new keys request
        if (0 == filteredUsers!!.size) {
            // trigger nothing
            return
        }

        // sanity check
        //if (null == mxSession.dataHandler || null == mxSession.dataHandler.store) {
        //    return
        //}

        isDownloadingKeys = true

        // track the race condition while sending requests
        // we defines a tag for each request
        // and test if the response is the latest request one
        val downloadToken = filteredUsers.hashCode().toString() + " " + System.currentTimeMillis()

        for (userId in filteredUsers) {
            pendingDownloadKeysRequestToken[userId] = downloadToken
        }

        downloadKeysForUsersTask
                .configureWith(DownloadKeysForUsersTask.Params(filteredUsers, syncTokenStore.getLastToken()))
                .executeOn(TaskThread.ENCRYPTION)
                .dispatchTo(object : MatrixCallback<KeysQueryResponse> {
                    override fun onSuccess(data: KeysQueryResponse) {
                        CryptoAsyncHelper.getEncryptBackgroundHandler().post {
                            Timber.v("## doKeyDownloadForUsers() : Got keys for " + filteredUsers.size + " users")
                            val userIdsList = ArrayList(filteredUsers)

                            for (userId in userIdsList) {
                                // test if the response is the latest request one
                                if (!TextUtils.equals(pendingDownloadKeysRequestToken[userId], downloadToken)) {
                                    Timber.e("## doKeyDownloadForUsers() : Another update in the queue for "
                                            + userId + " not marking up-to-date")
                                    filteredUsers.remove(userId)
                                } else {
                                    val devices = data.deviceKeys!![userId]

                                    Timber.v("## doKeyDownloadForUsers() : Got keys for $userId : $devices")

                                    if (null != devices) {
                                        val mutableDevices = HashMap(devices)
                                        val deviceIds = ArrayList(mutableDevices.keys)

                                        for (deviceId in deviceIds) {
                                            // the user has been logged out
                                            // TODO
                                            //if (null == cryptoStore) {
                                            //    break
                                            //}

                                            // Get the potential previously store device keys for this device
                                            val previouslyStoredDeviceKeys = cryptoStore.getUserDevice(deviceId, userId)
                                            val deviceInfo = mutableDevices[deviceId]

                                            // in some race conditions (like unit tests)
                                            // the self device must be seen as verified
                                            if (TextUtils.equals(deviceInfo!!.deviceId, credentials.deviceId) && TextUtils.equals(userId, credentials.userId)) {
                                                deviceInfo.mVerified = MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED
                                            }

                                            // Validate received keys
                                            if (!validateDeviceKeys(deviceInfo, userId, deviceId, previouslyStoredDeviceKeys)) {
                                                // New device keys are not valid. Do not store them
                                                mutableDevices.remove(deviceId)

                                                if (null != previouslyStoredDeviceKeys) {
                                                    // But keep old validated ones if any
                                                    mutableDevices[deviceId] = previouslyStoredDeviceKeys
                                                }
                                            } else if (null != previouslyStoredDeviceKeys) {
                                                // The verified status is not sync'ed with hs.
                                                // This is a client side information, valid only for this client.
                                                // So, transfer its previous value
                                                mutableDevices[deviceId]!!.mVerified = previouslyStoredDeviceKeys.mVerified
                                            }
                                        }

                                        // Update the store
                                        // Note that devices which aren't in the response will be removed from the stores
                                        cryptoStore.storeUserDevices(userId, mutableDevices)
                                    }

                                    // the response is the latest request one
                                    pendingDownloadKeysRequestToken.remove(userId)
                                }
                            }

                            onKeysDownloadSucceed(filteredUsers, data.failures)
                        }
                    }

                    private fun onFailed() {
                        CryptoAsyncHelper.getEncryptBackgroundHandler().post {
                            val userIdsList = ArrayList(filteredUsers)

                            // test if the response is the latest request one
                            for (userId in userIdsList) {
                                if (!TextUtils.equals(pendingDownloadKeysRequestToken[userId], downloadToken)) {
                                    Timber.e("## doKeyDownloadForUsers() : Another update in the queue for $userId not marking up-to-date")
                                    filteredUsers.remove(userId)
                                } else {
                                    // the response is the latest request one
                                    pendingDownloadKeysRequestToken.remove(userId)
                                }
                            }

                            onKeysDownloadFailed(filteredUsers)
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        Timber.e(failure, "##doKeyDownloadForUsers() : onNetworkError")

                        onFailed()

                        callback?.onFailure(failure)
                    }
                })
                .executeBy(taskExecutor)
    }

    /**
     * Validate device keys.
     * This method must called on getEncryptingThreadHandler() thread.
     *
     * @param deviceKeys                 the device keys to validate.
     * @param userId                     the id of the user of the device.
     * @param deviceId                   the id of the device.
     * @param previouslyStoredDeviceKeys the device keys we received before for this device
     * @return true if succeeds
     */
    private fun validateDeviceKeys(deviceKeys: MXDeviceInfo?, userId: String, deviceId: String, previouslyStoredDeviceKeys: MXDeviceInfo?): Boolean {
        if (null == deviceKeys) {
            Timber.e("## validateDeviceKeys() : deviceKeys is null from $userId:$deviceId")
            return false
        }

        if (null == deviceKeys.keys) {
            Timber.e("## validateDeviceKeys() : deviceKeys.keys is null from $userId:$deviceId")
            return false
        }

        if (null == deviceKeys.signatures) {
            Timber.e("## validateDeviceKeys() : deviceKeys.signatures is null from $userId:$deviceId")
            return false
        }

        // Check that the user_id and device_id in the received deviceKeys are correct
        if (!TextUtils.equals(deviceKeys.userId, userId)) {
            Timber.e("## validateDeviceKeys() : Mismatched user_id " + deviceKeys.userId + " from " + userId + ":" + deviceId)
            return false
        }

        if (!TextUtils.equals(deviceKeys.deviceId, deviceId)) {
            Timber.e("## validateDeviceKeys() : Mismatched device_id " + deviceKeys.deviceId + " from " + userId + ":" + deviceId)
            return false
        }

        val signKeyId = "ed25519:" + deviceKeys.deviceId
        val signKey = deviceKeys.keys!![signKeyId]

        if (null == signKey) {
            Timber.e("## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " has no ed25519 key")
            return false
        }

        val signatureMap = deviceKeys.signatures!![userId]

        if (null == signatureMap) {
            Timber.e("## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " has no map for " + userId)
            return false
        }

        val signature = signatureMap[signKeyId]

        if (null == signature) {
            Timber.e("## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " is not signed")
            return false
        }

        var isVerified = false
        var errorMessage: String? = null

        try {
            olmDevice.verifySignature(signKey, deviceKeys.signalableJSONDictionary(), signature)
            isVerified = true
        } catch (e: Exception) {
            errorMessage = e.message
        }

        if (!isVerified) {
            Timber.e("## validateDeviceKeys() : Unable to verify signature on device " + userId + ":"
                    + deviceKeys.deviceId + " with error " + errorMessage)
            return false
        }

        if (null != previouslyStoredDeviceKeys) {
            if (!TextUtils.equals(previouslyStoredDeviceKeys.fingerprint(), signKey)) {
                // This should only happen if the list has been MITMed; we are
                // best off sticking with the original keys.
                //
                // Should we warn the user about it somehow?
                Timber.e("## validateDeviceKeys() : WARNING:Ed25519 key for device " + userId + ":"
                        + deviceKeys.deviceId + " has changed : "
                        + previouslyStoredDeviceKeys.fingerprint() + " -> " + signKey)

                Timber.e("## validateDeviceKeys() : $previouslyStoredDeviceKeys -> $deviceKeys")
                Timber.e("## validateDeviceKeys() : " + previouslyStoredDeviceKeys.keys + " -> " + deviceKeys.keys)

                return false
            }
        }

        return true
    }

    /**
     * Start device queries for any users who sent us an m.new_device recently
     * This method must be called on getEncryptingThreadHandler() thread.
     */
    fun refreshOutdatedDeviceLists() {
        val users = ArrayList<String>()

        val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()

        for (userId in deviceTrackingStatuses.keys) {
            if (TRACKING_STATUS_PENDING_DOWNLOAD == deviceTrackingStatuses[userId]) {
                users.add(userId)
            }
        }

        if (users.size == 0) {
            return
        }

        if (isDownloadingKeys) {
            // request already in progress - do nothing. (We will automatically
            // make another request if there are more users with outdated
            // device lists when the current request completes).
            return
        }

        // update the statuses
        for (userId in users) {
            val status = deviceTrackingStatuses[userId]

            if (null != status && TRACKING_STATUS_PENDING_DOWNLOAD == status) {
                deviceTrackingStatuses.put(userId, TRACKING_STATUS_DOWNLOAD_IN_PROGRESS)
            }
        }

        cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)

        doKeyDownloadForUsers(users, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                CryptoAsyncHelper.getEncryptBackgroundHandler().post { Timber.v("## refreshOutdatedDeviceLists() : done") }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## refreshOutdatedDeviceLists() : ERROR updating device keys for users $users")
            }
        })
    }

    companion object {

        /**
         * State transition diagram for DeviceList.deviceTrackingStatus
         * <pre>
         *
         *                                   |
         *        stopTrackingDeviceList     V
         *      +---------------------> NOT_TRACKED
         *      |                            |
         *      +<--------------------+      | startTrackingDeviceList
         *      |                     |      V
         *      |   +-------------> PENDING_DOWNLOAD <--------------------+-+
         *      |   |                      ^ |                            | |
         *      |   | restart     download | |  start download            | | invalidateUserDeviceList
         *      |   | client        failed | |                            | |
         *      |   |                      | V                            | |
         *      |   +------------ DOWNLOAD_IN_PROGRESS -------------------+ |
         *      |                    |       |                              |
         *      +<-------------------+       |  download successful         |
         *      ^                            V                              |
         *      +----------------------- UP_TO_DATE ------------------------+
         *
         * </pre>
         */

        const val TRACKING_STATUS_NOT_TRACKED = -1
        const val TRACKING_STATUS_PENDING_DOWNLOAD = 1
        const val TRACKING_STATUS_DOWNLOAD_IN_PROGRESS = 2
        const val TRACKING_STATUS_UP_TO_DATE = 3
        const val TRACKING_STATUS_UNREACHABLE_SERVER = 4
    }
}