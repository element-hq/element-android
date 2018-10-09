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

package im.vector.matrix.android.internal.legacy.crypto;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXPatterns;
import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;
import im.vector.matrix.android.internal.legacy.data.cryptostore.IMXCryptoStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.KeysQueryResponse;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MXDeviceList {
    private static final String LOG_TAG = MXDeviceList.class.getSimpleName();

    /**
     * State transition diagram for DeviceList.deviceTrackingStatus
     * <p>
     * |
     * stopTrackingDeviceList     V
     * +---------------------> NOT_TRACKED
     * |                            |
     * +<--------------------+      | startTrackingDeviceList
     * |                     |      V
     * |   +-------------> PENDING_DOWNLOAD <--------------------+-+
     * |   |                      ^ |                            | |
     * |   | restart     download | |  start download            | | invalidateUserDeviceList
     * |   | client        failed | |                            | |
     * |   |                      | V                            | |
     * |   +------------ DOWNLOAD_IN_PROGRESS -------------------+ |
     * |                    |       |                              |
     * +<-------------------+       |  download successful         |
     * ^                            V                              |
     * +----------------------- UP_TO_DATE ------------------------+
     **/

    public static final int TRACKING_STATUS_NOT_TRACKED = -1;
    public static final int TRACKING_STATUS_PENDING_DOWNLOAD = 1;
    public static final int TRACKING_STATUS_DOWNLOAD_IN_PROGRESS = 2;
    public static final int TRACKING_STATUS_UP_TO_DATE = 3;
    public static final int TRACKING_STATUS_UNREACHABLE_SERVER = 4;

    // keys in progress
    private final Set<String> mUserKeyDownloadsInProgress = new HashSet<>();

    // HS not ready for retry
    private final Set<String> mNotReadyToRetryHS = new HashSet<>();

    // indexed by UserId
    private final Map<String, String> mPendingDownloadKeysRequestToken = new HashMap<>();

    // download keys queue
    class DownloadKeysPromise {
        // list of remain pending device keys
        final List<String> mPendingUserIdsList;

        // the unfiltered user ids list
        final List<String> mUserIdsList;

        // the request callback
        final ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> mCallback;

        /**
         * Creator
         *
         * @param userIds  the user ids list
         * @param callback the asynchronous callback
         */
        DownloadKeysPromise(List<String> userIds, ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback) {
            mPendingUserIdsList = new ArrayList<>(userIds);
            mUserIdsList = new ArrayList<>(userIds);
            mCallback = callback;
        }
    }

    // pending queues list
    private final List<DownloadKeysPromise> mDownloadKeysQueues = new ArrayList<>();

    private final MXCrypto mxCrypto;

    private final MXSession mxSession;

    private final IMXCryptoStore mCryptoStore;

    // tells if there is a download keys request in progress
    private boolean mIsDownloadingKeys = false;

    /**
     * Constructor
     *
     * @param session the session
     * @param crypto  the crypto session
     */
    public MXDeviceList(MXSession session, MXCrypto crypto) {
        mxSession = session;
        mxCrypto = crypto;
        mCryptoStore = crypto.getCryptoStore();

        boolean isUpdated = false;

        Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();
        for (String userId : deviceTrackingStatuses.keySet()) {
            int status = deviceTrackingStatuses.get(userId);

            if ((TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == status) || (TRACKING_STATUS_UNREACHABLE_SERVER == status)) {
                // if a download was in progress when we got shut down, it isn't any more.
                deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD);
                isUpdated = true;
            }
        }

        if (isUpdated) {
            mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);
        }
    }

    /**
     * Tells if the key downloads should be tried
     *
     * @param userId the userId
     * @return true if the keys download can be retrieved
     */
    private boolean canRetryKeysDownload(String userId) {
        boolean res = false;

        if (!TextUtils.isEmpty(userId) && userId.contains(":")) {
            try {
                synchronized (mNotReadyToRetryHS) {
                    res = !mNotReadyToRetryHS.contains(userId.substring(userId.lastIndexOf(":") + 1));
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## canRetryKeysDownload() failed : " + e.getMessage(), e);
            }
        }

        return res;
    }

    /**
     * Add a download keys promise
     *
     * @param userIds  the user ids list
     * @param callback the asynchronous callback
     * @return the filtered user ids list i.e the one which require a remote request
     */
    private List<String> addDownloadKeysPromise(List<String> userIds, ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback) {
        if (null != userIds) {
            List<String> filteredUserIds = new ArrayList<>();
            List<String> invalidUserIds = new ArrayList<>();

            for (String userId : userIds) {
                if (MXPatterns.isUserId(userId)) {
                    filteredUserIds.add(userId);
                } else {
                    Log.e(LOG_TAG, "## userId " + userId + "is not a valid user id");
                    invalidUserIds.add(userId);
                }
            }

            synchronized (mUserKeyDownloadsInProgress) {
                filteredUserIds.removeAll(mUserKeyDownloadsInProgress);
                mUserKeyDownloadsInProgress.addAll(userIds);
                // got some email addresses instead of matrix ids
                mUserKeyDownloadsInProgress.removeAll(invalidUserIds);
                userIds.removeAll(invalidUserIds);
            }

            mDownloadKeysQueues.add(new DownloadKeysPromise(userIds, callback));

            return filteredUserIds;
        } else {
            return null;
        }
    }

    /**
     * Clear the unavailable server lists
     */
    private void clearUnavailableServersList() {
        synchronized (mNotReadyToRetryHS) {
            mNotReadyToRetryHS.clear();
        }
    }

    /**
     * Mark the cached device list for the given user outdated
     * flag the given user for device-list tracking, if they are not already.
     *
     * @param userIds the user ids list
     */
    public void startTrackingDeviceList(List<String> userIds) {
        if (null != userIds) {
            boolean isUpdated = false;
            Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();

            for (String userId : userIds) {
                if (!deviceTrackingStatuses.containsKey(userId) || (TRACKING_STATUS_NOT_TRACKED == deviceTrackingStatuses.get(userId))) {
                    Log.d(LOG_TAG, "## startTrackingDeviceList() : Now tracking device list for " + userId);
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD);
                    isUpdated = true;
                }
            }

            if (isUpdated) {
                mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);
            }
        }
    }

    /**
     * Update the devices list statuses
     *
     * @param changed the user ids list which have new devices
     * @param left    the user ids list which left a room
     */
    public void handleDeviceListsChanges(List<String> changed, List<String> left) {
        boolean isUpdated = false;
        Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();

        if ((null != changed) && (0 != changed.size())) {
            clearUnavailableServersList();

            for (String userId : changed) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Log.d(LOG_TAG, "## invalidateUserDeviceList() : Marking device list outdated for " + userId);
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD);
                    isUpdated = true;
                }
            }
        }

        if ((null != left) && (0 != left.size())) {
            clearUnavailableServersList();

            for (String userId : left) {
                if (deviceTrackingStatuses.containsKey(userId)) {
                    Log.d(LOG_TAG, "## invalidateUserDeviceList() : No longer tracking device list for " + userId);
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_NOT_TRACKED);
                    isUpdated = true;
                }
            }
        }

        if (isUpdated) {
            mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);
        }
    }

    /**
     * This will flag each user whose devices we are tracking as in need of an
     * + update
     */
    public void invalidateAllDeviceLists() {
        handleDeviceListsChanges(new ArrayList<>(mCryptoStore.getDeviceTrackingStatuses().keySet()), null);
    }

    /**
     * The keys download failed
     *
     * @param userIds the user ids list
     */
    private void onKeysDownloadFailed(final List<String> userIds) {
        if (null != userIds) {
            synchronized (mUserKeyDownloadsInProgress) {
                Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();

                for (String userId : userIds) {
                    mUserKeyDownloadsInProgress.remove(userId);
                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD);
                }

                mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);
            }
        }

        mIsDownloadingKeys = false;
    }

    /**
     * The keys download succeeded.
     *
     * @param userIds  the userIds list
     * @param failures the failure map.
     */
    private void onKeysDownloadSucceed(List<String> userIds, Map<String, Map<String, Object>> failures) {
        if (null != failures) {
            Set<String> keys = failures.keySet();

            for (String k : keys) {
                Map<String, Object> value = failures.get(k);

                if (value.containsKey("status")) {
                    Object statusCodeAsVoid = value.get("status");
                    int statusCode = 0;

                    if (statusCodeAsVoid instanceof Double) {
                        statusCode = ((Double) statusCodeAsVoid).intValue();
                    } else if (statusCodeAsVoid instanceof Integer) {
                        statusCode = ((Integer) statusCodeAsVoid).intValue();
                    }

                    if (statusCode == 503) {
                        synchronized (mNotReadyToRetryHS) {
                            mNotReadyToRetryHS.add(k);
                        }
                    }
                }
            }
        }

        Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();

        if (null != userIds) {
            if (mDownloadKeysQueues.size() > 0) {
                List<DownloadKeysPromise> promisesToRemove = new ArrayList<>();

                for (DownloadKeysPromise promise : mDownloadKeysQueues) {
                    promise.mPendingUserIdsList.removeAll(userIds);

                    if (promise.mPendingUserIdsList.size() == 0) {
                        // private members
                        final MXUsersDevicesMap<MXDeviceInfo> usersDevicesInfoMap = new MXUsersDevicesMap<>();

                        for (String userId : promise.mUserIdsList) {
                            Map<String, MXDeviceInfo> devices = mCryptoStore.getUserDevices(userId);
                            if (null == devices) {
                                if (canRetryKeysDownload(userId)) {
                                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_PENDING_DOWNLOAD);
                                    Log.e(LOG_TAG, "failed to retry the devices of " + userId + " : retry later");
                                } else {
                                    if (deviceTrackingStatuses.containsKey(userId)
                                            && (TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses.get(userId))) {
                                        deviceTrackingStatuses.put(userId, TRACKING_STATUS_UNREACHABLE_SERVER);
                                        Log.e(LOG_TAG, "failed to retry the devices of " + userId + " : the HS is not available");
                                    }
                                }
                            } else {
                                if (deviceTrackingStatuses.containsKey(userId)
                                        && (TRACKING_STATUS_DOWNLOAD_IN_PROGRESS == deviceTrackingStatuses.get(userId))) {
                                    // we didn't get any new invalidations since this download started:
                                    //  this user's device list is now up to date.
                                    deviceTrackingStatuses.put(userId, TRACKING_STATUS_UP_TO_DATE);
                                    Log.d(LOG_TAG, "Device list for " + userId + " now up to date");
                                }

                                // And the response result
                                usersDevicesInfoMap.setObjects(devices, userId);
                            }
                        }

                        if (!mxCrypto.hasBeenReleased()) {
                            final ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback = promise.mCallback;

                            if (null != callback) {
                                mxCrypto.getUIHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess(usersDevicesInfoMap);
                                    }
                                });
                            }
                        }
                        promisesToRemove.add(promise);
                    }
                }
                mDownloadKeysQueues.removeAll(promisesToRemove);
            }

            for (String userId : userIds) {
                mUserKeyDownloadsInProgress.remove(userId);
            }

            mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);
        }

        mIsDownloadingKeys = false;
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
    public void downloadKeys(List<String> userIds, boolean forceDownload, final ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback) {
        Log.d(LOG_TAG, "## downloadKeys() : forceDownload " + forceDownload + " : " + userIds);

        // Map from userid -> deviceid -> DeviceInfo
        final MXUsersDevicesMap<MXDeviceInfo> stored = new MXUsersDevicesMap<>();

        // List of user ids we need to download keys for
        final List<String> downloadUsers = new ArrayList<>();

        if (null != userIds) {
            if (forceDownload) {
                downloadUsers.addAll(userIds);
            } else {
                for (String userId : userIds) {
                    Integer status = mCryptoStore.getDeviceTrackingStatus(userId, TRACKING_STATUS_NOT_TRACKED);

                    // downloading keys ->the keys download won't be triggered twice but the callback requires the dedicated keys
                    // not yet retrieved
                    if (mUserKeyDownloadsInProgress.contains(userId)
                            || ((TRACKING_STATUS_UP_TO_DATE != status) && (TRACKING_STATUS_UNREACHABLE_SERVER != status))) {
                        downloadUsers.add(userId);
                    } else {
                        Map<String, MXDeviceInfo> devices = mCryptoStore.getUserDevices(userId);

                        // should always be true
                        if (null != devices) {
                            stored.setObjects(devices, userId);
                        } else {
                            downloadUsers.add(userId);
                        }
                    }
                }
            }
        }

        if (0 == downloadUsers.size()) {
            Log.d(LOG_TAG, "## downloadKeys() : no new user device");

            if (null != callback) {
                mxCrypto.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(stored);
                    }
                });
            }
        } else {
            Log.d(LOG_TAG, "## downloadKeys() : starts");
            final long t0 = System.currentTimeMillis();

            doKeyDownloadForUsers(downloadUsers, new ApiCallback<MXUsersDevicesMap<MXDeviceInfo>>() {
                public void onSuccess(MXUsersDevicesMap<MXDeviceInfo> usersDevicesInfoMap) {
                    Log.d(LOG_TAG, "## downloadKeys() : doKeyDownloadForUsers succeeds after " + (System.currentTimeMillis() - t0) + " ms");

                    usersDevicesInfoMap.addEntriesFromMap(stored);

                    if (null != callback) {
                        callback.onSuccess(usersDevicesInfoMap);
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.e(LOG_TAG, "## downloadKeys() : doKeyDownloadForUsers onNetworkError " + e.getMessage(), e);
                    if (null != callback) {
                        callback.onNetworkError(e);
                    }
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    Log.e(LOG_TAG, "## downloadKeys() : doKeyDownloadForUsers onMatrixError " + e.getMessage());
                    if (null != callback) {
                        callback.onMatrixError(e);
                    }
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    Log.e(LOG_TAG, "## downloadKeys() : doKeyDownloadForUsers onUnexpectedError " + e.getMessage(), e);
                    if (null != callback) {
                        callback.onUnexpectedError(e);
                    }
                }
            });
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
    private void doKeyDownloadForUsers(final List<String> downloadUsers, final ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback) {
        Log.d(LOG_TAG, "## doKeyDownloadForUsers() : doKeyDownloadForUsers " + downloadUsers);

        // get the user ids which did not already trigger a keys download
        final List<String> filteredUsers = addDownloadKeysPromise(downloadUsers, callback);

        // if there is no new keys request
        if (0 == filteredUsers.size()) {
            // trigger nothing
            return;
        }

        // sanity check
        if ((null == mxSession.getDataHandler()) || (null == mxSession.getDataHandler().getStore())) {
            return;
        }

        mIsDownloadingKeys = true;

        // track the race condition while sending requests
        // we defines a tag for each request
        // and test if the response is the latest request one
        final String downloadToken = filteredUsers.hashCode() + " " + System.currentTimeMillis();

        for (String userId : filteredUsers) {
            mPendingDownloadKeysRequestToken.put(userId, downloadToken);
        }

        mxSession.getCryptoRestClient()
                .downloadKeysForUsers(filteredUsers, mxSession.getDataHandler().getStore().getEventStreamToken(), new ApiCallback<KeysQueryResponse>() {
                    @Override
                    public void onSuccess(final KeysQueryResponse keysQueryResponse) {
                        mxCrypto.getEncryptingThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(LOG_TAG, "## doKeyDownloadForUsers() : Got keys for " + filteredUsers.size() + " users");
                                MXDeviceInfo myDevice = mxCrypto.getMyDevice();
                                IMXCryptoStore cryptoStore = mxCrypto.getCryptoStore();

                                List<String> userIdsList = new ArrayList<>(filteredUsers);

                                for (String userId : userIdsList) {
                                    // test if the response is the latest request one
                                    if (!TextUtils.equals(mPendingDownloadKeysRequestToken.get(userId), downloadToken)) {
                                        Log.e(LOG_TAG, "## doKeyDownloadForUsers() : Another update in the queue for "
                                                + userId + " not marking up-to-date");
                                        filteredUsers.remove(userId);
                                    } else {
                                        Map<String, MXDeviceInfo> devices = keysQueryResponse.deviceKeys.get(userId);

                                        Log.d(LOG_TAG, "## doKeyDownloadForUsers() : Got keys for " + userId + " : " + devices);

                                        if (null != devices) {
                                            Map<String, MXDeviceInfo> mutableDevices = new HashMap<>(devices);
                                            List<String> deviceIds = new ArrayList<>(mutableDevices.keySet());

                                            for (String deviceId : deviceIds) {
                                                // the user has been logged out
                                                if (null == cryptoStore) {
                                                    break;
                                                }

                                                // Get the potential previously store device keys for this device
                                                MXDeviceInfo previouslyStoredDeviceKeys = cryptoStore.getUserDevice(deviceId, userId);
                                                MXDeviceInfo deviceInfo = mutableDevices.get(deviceId);

                                                // in some race conditions (like unit tests)
                                                // the self device must be seen as verified
                                                if (TextUtils.equals(deviceInfo.deviceId, myDevice.deviceId)
                                                        && TextUtils.equals(userId, myDevice.userId)) {
                                                    deviceInfo.mVerified = MXDeviceInfo.DEVICE_VERIFICATION_VERIFIED;
                                                }

                                                // Validate received keys
                                                if (!validateDeviceKeys(deviceInfo, userId, deviceId, previouslyStoredDeviceKeys)) {
                                                    // New device keys are not valid. Do not store them
                                                    mutableDevices.remove(deviceId);

                                                    if (null != previouslyStoredDeviceKeys) {
                                                        // But keep old validated ones if any
                                                        mutableDevices.put(deviceId, previouslyStoredDeviceKeys);
                                                    }
                                                } else if (null != previouslyStoredDeviceKeys) {
                                                    // The verified status is not sync'ed with hs.
                                                    // This is a client side information, valid only for this client.
                                                    // So, transfer its previous value
                                                    mutableDevices.get(deviceId).mVerified = previouslyStoredDeviceKeys.mVerified;
                                                }
                                            }

                                            // Update the store
                                            // Note that devices which aren't in the response will be removed from the stores
                                            cryptoStore.storeUserDevices(userId, mutableDevices);
                                        }

                                        // the response is the latest request one
                                        mPendingDownloadKeysRequestToken.remove(userId);
                                    }
                                }

                                onKeysDownloadSucceed(filteredUsers, keysQueryResponse.failures);
                            }
                        });
                    }

                    private void onFailed() {
                        mxCrypto.getEncryptingThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                List<String> userIdsList = new ArrayList<>(filteredUsers);

                                // test if the response is the latest request one
                                for (String userId : userIdsList) {
                                    if (!TextUtils.equals(mPendingDownloadKeysRequestToken.get(userId), downloadToken)) {
                                        Log.e(LOG_TAG, "## doKeyDownloadForUsers() : Another update in the queue for " + userId + " not marking up-to-date");
                                        filteredUsers.remove(userId);
                                    } else {
                                        // the response is the latest request one
                                        mPendingDownloadKeysRequestToken.remove(userId);
                                    }
                                }

                                onKeysDownloadFailed(filteredUsers);
                            }
                        });
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.e(LOG_TAG, "##doKeyDownloadForUsers() : onNetworkError " + e.getMessage(), e);

                        onFailed();

                        if (null != callback) {
                            callback.onNetworkError(e);
                        }
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        Log.e(LOG_TAG, "##doKeyDownloadForUsers() : onMatrixError " + e.getMessage());

                        onFailed();

                        if (null != callback) {
                            callback.onMatrixError(e);
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        Log.e(LOG_TAG, "##doKeyDownloadForUsers() : onUnexpectedError " + e.getMessage(), e);

                        onFailed();

                        if (null != callback) {
                            callback.onUnexpectedError(e);
                        }
                    }
                });
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
    private boolean validateDeviceKeys(MXDeviceInfo deviceKeys, String userId, String deviceId, MXDeviceInfo previouslyStoredDeviceKeys) {
        if (null == deviceKeys) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : deviceKeys is null from " + userId + ":" + deviceId);
            return false;
        }

        if (null == deviceKeys.keys) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : deviceKeys.keys is null from " + userId + ":" + deviceId);
            return false;
        }

        if (null == deviceKeys.signatures) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : deviceKeys.signatures is null from " + userId + ":" + deviceId);
            return false;
        }

        // Check that the user_id and device_id in the received deviceKeys are correct
        if (!TextUtils.equals(deviceKeys.userId, userId)) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Mismatched user_id " + deviceKeys.userId + " from " + userId + ":" + deviceId);
            return false;
        }

        if (!TextUtils.equals(deviceKeys.deviceId, deviceId)) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Mismatched device_id " + deviceKeys.deviceId + " from " + userId + ":" + deviceId);
            return false;
        }

        String signKeyId = "ed25519:" + deviceKeys.deviceId;
        String signKey = deviceKeys.keys.get(signKeyId);

        if (null == signKey) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " has no ed25519 key");
            return false;
        }

        Map<String, String> signatureMap = deviceKeys.signatures.get(userId);

        if (null == signatureMap) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " has no map for " + userId);
            return false;
        }

        String signature = signatureMap.get(signKeyId);

        if (null == signature) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Device " + userId + ":" + deviceKeys.deviceId + " is not signed");
            return false;
        }

        boolean isVerified = false;
        String errorMessage = null;

        try {
            mxCrypto.getOlmDevice().verifySignature(signKey, deviceKeys.signalableJSONDictionary(), signature);
            isVerified = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

        if (!isVerified) {
            Log.e(LOG_TAG, "## validateDeviceKeys() : Unable to verify signature on device " + userId + ":"
                    + deviceKeys.deviceId + " with error " + errorMessage);
            return false;
        }

        if (null != previouslyStoredDeviceKeys) {
            if (!TextUtils.equals(previouslyStoredDeviceKeys.fingerprint(), signKey)) {
                // This should only happen if the list has been MITMed; we are
                // best off sticking with the original keys.
                //
                // Should we warn the user about it somehow?
                Log.e(LOG_TAG, "## validateDeviceKeys() : WARNING:Ed25519 key for device " + userId + ":"
                        + deviceKeys.deviceId + " has changed : "
                        + previouslyStoredDeviceKeys.fingerprint() + " -> " + signKey);

                Log.e(LOG_TAG, "## validateDeviceKeys() : " + previouslyStoredDeviceKeys + " -> " + deviceKeys);
                Log.e(LOG_TAG, "## validateDeviceKeys() : " + previouslyStoredDeviceKeys.keys + " -> " + deviceKeys.keys);

                return false;
            }
        }

        return true;
    }

    /**
     * Start device queries for any users who sent us an m.new_device recently
     * This method must be called on getEncryptingThreadHandler() thread.
     */
    public void refreshOutdatedDeviceLists() {
        final List<String> users = new ArrayList<>();

        Map<String, Integer> deviceTrackingStatuses = mCryptoStore.getDeviceTrackingStatuses();

        for (String userId : deviceTrackingStatuses.keySet()) {
            if (TRACKING_STATUS_PENDING_DOWNLOAD == deviceTrackingStatuses.get(userId)) {
                users.add(userId);
            }
        }

        if (users.size() == 0) {
            return;
        }

        if (mIsDownloadingKeys) {
            // request already in progress - do nothing. (We will automatically
            // make another request if there are more users with outdated
            // device lists when the current request completes).
            return;
        }

        // update the statuses
        for (String userId : users) {
            Integer status = deviceTrackingStatuses.get(userId);

            if ((null != status) && (TRACKING_STATUS_PENDING_DOWNLOAD == status)) {
                deviceTrackingStatuses.put(userId, TRACKING_STATUS_DOWNLOAD_IN_PROGRESS);
            }
        }

        mCryptoStore.saveDeviceTrackingStatuses(deviceTrackingStatuses);

        doKeyDownloadForUsers(users, new ApiCallback<MXUsersDevicesMap<MXDeviceInfo>>() {
            @Override
            public void onSuccess(final MXUsersDevicesMap<MXDeviceInfo> response) {
                mxCrypto.getEncryptingThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "## refreshOutdatedDeviceLists() : done");
                    }
                });
            }

            private void onError(String error) {
                Log.e(LOG_TAG, "## refreshOutdatedDeviceLists() : ERROR updating device keys for users " + users + " : " + error);
            }

            @Override
            public void onNetworkError(final Exception e) {
                onError(e.getMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                onError(e.getMessage());
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getMessage());
            }
        });
    }
}