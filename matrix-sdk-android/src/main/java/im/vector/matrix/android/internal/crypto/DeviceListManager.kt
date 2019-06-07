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
import arrow.core.Try
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.util.onError
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.DownloadKeysForUsersTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import timber.log.Timber
import java.util.*

// Legacy name: MXDeviceList
internal class DeviceListManager(private val cryptoStore: IMXCryptoStore,
                                 private val olmDevice: MXOlmDevice,
                                 private val syncTokenStore: SyncTokenStore,
                                 private val credentials: Credentials,
                                 private val downloadKeysForUsersTask: DownloadKeysForUsersTask) {

    // HS not ready for retry
    private val notReadyToRetryHS = HashSet<String>()

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
                    deviceTrackingStatuses[userId] = TRACKING_STATUS_PENDING_DOWNLOAD
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
            for (userId in changed) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Timber.v("## invalidateUserDeviceList() : Marking device list outdated for $userId")
                    deviceTrackingStatuses[userId] = TRACKING_STATUS_PENDING_DOWNLOAD
                    isUpdated = true
                }
            }
        }

        if (left?.isNotEmpty() == true) {
            for (userId in left) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Timber.v("## invalidateUserDeviceList() : No longer tracking device list for $userId")
                    deviceTrackingStatuses[userId] = TRACKING_STATUS_NOT_TRACKED
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
    private fun onKeysDownloadFailed(userIds: List<String>) {
        val deviceTrackingStatuses = cryptoStore.getDeviceTrackingStatuses().toMutableMap()
        for (userId in userIds) {
            deviceTrackingStatuses[userId] = TRACKING_STATUS_PENDING_DOWNLOAD
        }
        cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
    }

    /**
     * The keys download succeeded.
     *
     * @param userIds  the userIds list
     * @param failures the failure map.
     */
    private fun onKeysDownloadSucceed(userIds: List<String>, failures: Map<String, Map<String, Any>>?): MXUsersDevicesMap<MXDeviceInfo> {
        if (failures != null) {
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
        val usersDevicesInfoMap = MXUsersDevicesMap<MXDeviceInfo>()
        for (userId in userIds) {
            val devices = cryptoStore.getUserDevices(userId)
            if (null == devices) {
                if (canRetryKeysDownload(userId)) {
                    deviceTrackingStatuses[userId] = TRACKING_STATUS_PENDING_DOWNLOAD
                    Timber.e("failed to retry the devices of $userId : retry later")
                } else {
                    if (deviceTrackingStatuses.containsKey(userId) && TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses[userId]) {
                        deviceTrackingStatuses[userId] = TRACKING_STATUS_UNREACHABLE_SERVER
                        Timber.e("failed to retry the devices of $userId : the HS is not available")
                    }
                }
            } else {
                if (deviceTrackingStatuses.containsKey(userId) && TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses[userId]) {
                    // we didn't get any new invalidations since this download started:
                    //  this user's device list is now up to date.
                    deviceTrackingStatuses[userId] = TRACKING_STATUS_UP_TO_DATE
                    Timber.v("Device list for $userId now up to date")
                }
                // And the response result
                usersDevicesInfoMap.setObjects(devices, userId)
            }
        }
        cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
        return usersDevicesInfoMap
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
    suspend fun downloadKeys(userIds: List<String>?, forceDownload: Boolean): Try<MXUsersDevicesMap<MXDeviceInfo>> {
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
                    if (TRACKING_STATUS_UP_TO_DATE != status && TRACKING_STATUS_UNREACHABLE_SERVER != status) {
                        downloadUsers.add(userId)
                    } else {
                        val devices = cryptoStore.getUserDevices(userId)
                        // should always be true
                        if (devices != null) {
                            stored.setObjects(devices, userId)
                        } else {
                            downloadUsers.add(userId)
                        }
                    }
                }
            }
        }
        return if (downloadUsers.isEmpty()) {
            Timber.v("## downloadKeys() : no new user device")
            Try.just(stored)
        } else {
            Timber.v("## downloadKeys() : starts")
            val t0 = System.currentTimeMillis()
            doKeyDownloadForUsers(downloadUsers)
                    .map {
                        Timber.v("## downloadKeys() : doKeyDownloadForUsers succeeds after " + (System.currentTimeMillis() - t0) + " ms")
                        it.addEntriesFromMap(stored)
                        it
                    }
        }
    }

    /**
     * Download the devices keys for a set of users.
     *
     * @param downloadUsers the user ids list
     */
    private suspend fun doKeyDownloadForUsers(downloadUsers: MutableList<String>): Try<MXUsersDevicesMap<MXDeviceInfo>> {
        Timber.v("## doKeyDownloadForUsers() : doKeyDownloadForUsers $downloadUsers")
        // get the user ids which did not already trigger a keys download
        val filteredUsers = downloadUsers.filter { MatrixPatterns.isUserId(it) }
        if (filteredUsers.isEmpty()) {
            // trigger nothing
            return Try.just(MXUsersDevicesMap())
        }
        val params = DownloadKeysForUsersTask.Params(filteredUsers, syncTokenStore.getLastToken())
        return downloadKeysForUsersTask.execute(params)
                .map { response ->
                    Timber.v("## doKeyDownloadForUsers() : Got keys for " + filteredUsers.size + " users")
                    for (userId in filteredUsers) {
                        val devices = response.deviceKeys?.get(userId)
                        Timber.v("## doKeyDownloadForUsers() : Got keys for $userId : $devices")
                        if (devices != null) {
                            val mutableDevices = HashMap(devices)
                            val deviceIds = ArrayList(mutableDevices.keys)
                            for (deviceId in deviceIds) {
                                // Get the potential previously store device keys for this device
                                val previouslyStoredDeviceKeys = cryptoStore.getUserDevice(deviceId, userId)
                                val deviceInfo = mutableDevices[deviceId]

                                // in some race conditions (like unit tests)
                                // the self device must be seen as verified
                                if (TextUtils.equals(deviceInfo!!.deviceId, credentials.deviceId) && TextUtils.equals(userId, credentials.userId)) {
                                    deviceInfo.verified = MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED
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
                                    mutableDevices[deviceId]!!.verified = previouslyStoredDeviceKeys.verified
                                }
                            }
                            // Update the store
                            // Note that devices which aren't in the response will be removed from the stores
                            cryptoStore.storeUserDevices(userId, mutableDevices)
                        }
                    }
                    onKeysDownloadSucceed(filteredUsers, response.failures)
                }
                .onError {
                    Timber.e(it, "##doKeyDownloadForUsers(): error")
                    onKeysDownloadFailed(filteredUsers)
                }
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
    suspend fun refreshOutdatedDeviceLists() {
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

        // update the statuses
        for (userId in users) {
            val status = deviceTrackingStatuses[userId]
            if (null != status && TRACKING_STATUS_PENDING_DOWNLOAD == status) {
                deviceTrackingStatuses.put(userId, TRACKING_STATUS_DOWNLOAD_IN_PROGRESS)
            }
        }

        cryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses)
        doKeyDownloadForUsers(users)
                .fold(
                        {
                            Timber.e(it, "## refreshOutdatedDeviceLists() : ERROR updating device keys for users $users")
                        },
                        {
                            Timber.v("## refreshOutdatedDeviceLists() : done")
                        }
                )
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