/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.crypto.algorithms.megolm;

import android.text.TextUtils;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.crypto.MXCrypto;
import im.vector.matrix.android.internal.legacy.crypto.MXCryptoAlgorithms;
import im.vector.matrix.android.internal.legacy.crypto.MXCryptoError;
import im.vector.matrix.android.internal.legacy.crypto.MXOlmDevice;
import im.vector.matrix.android.internal.legacy.crypto.algorithms.IMXEncrypting;
import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.crypto.data.MXOlmSessionResult;
import im.vector.matrix.android.internal.legacy.crypto.data.MXQueuedEncryption;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

public class MXMegolmEncryption implements IMXEncrypting {
    private static final String LOG_TAG = MXMegolmEncryption.class.getSimpleName();

    private MXSession mSession;
    private MXCrypto mCrypto;

    // The id of the room we will be sending to.
    private String mRoomId;

    private String mDeviceId;

    // OutboundSessionInfo. Null if we haven't yet started setting one up. Note
    // that even if this is non-null, it may not be ready for use (in which
    // case outboundSession.shareOperation will be non-null.)
    private MXOutboundSessionInfo mOutboundSession;

    // true when there is an HTTP operation in progress
    private boolean mShareOperationIsProgress;

    private final List<MXQueuedEncryption> mPendingEncryptions = new ArrayList<>();

    // Session rotation periods
    private int mSessionRotationPeriodMsgs;
    private int mSessionRotationPeriodMs;

    @Override
    public void initWithMatrixSession(MXSession matrixSession, String roomId) {
        mSession = matrixSession;
        mCrypto = matrixSession.getCrypto();

        mRoomId = roomId;
        mDeviceId = matrixSession.getCredentials().getDeviceId();

        // Default rotation periods
        // TODO: Make it configurable via parameters
        mSessionRotationPeriodMsgs = 100;
        mSessionRotationPeriodMs = 7 * 24 * 3600 * 1000;
    }

    /**
     * @return a snapshot of the pending encryptions
     */
    private List<MXQueuedEncryption> getPendingEncryptions() {
        List<MXQueuedEncryption> list = new ArrayList<>();

        synchronized (mPendingEncryptions) {
            list.addAll(mPendingEncryptions);
        }

        return list;
    }

    @Override
    public void encryptEventContent(final JsonElement eventContent,
                                    final String eventType,
                                    final List<String> userIds,
                                    final ApiCallback<JsonElement> callback) {
        // Queue the encryption request
        // It will be processed when everything is set up
        MXQueuedEncryption queuedEncryption = new MXQueuedEncryption();

        queuedEncryption.mEventContent = eventContent;
        queuedEncryption.mEventType = eventType;
        queuedEncryption.mApiCallback = callback;

        synchronized (mPendingEncryptions) {
            mPendingEncryptions.add(queuedEncryption);
        }

        final long t0 = System.currentTimeMillis();
        Log.d(LOG_TAG, "## encryptEventContent () starts");

        getDevicesInRoom(userIds, new ApiCallback<MXUsersDevicesMap<MXDeviceInfo>>() {

            /**
             * A network error has been received while encrypting
             * @param e the exception
             */
            private void dispatchNetworkError(Exception e) {
                Log.e(LOG_TAG, "## encryptEventContent() : onNetworkError " + e.getMessage(), e);
                List<MXQueuedEncryption> queuedEncryptions = getPendingEncryptions();

                for (MXQueuedEncryption queuedEncryption : queuedEncryptions) {
                    queuedEncryption.mApiCallback.onNetworkError(e);
                }

                synchronized (mPendingEncryptions) {
                    mPendingEncryptions.removeAll(queuedEncryptions);
                }
            }

            /**
             * A matrix error has been received while encrypting
             * @param e the exception
             */
            private void dispatchMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## encryptEventContent() : onMatrixError " + e.getMessage());

                List<MXQueuedEncryption> queuedEncryptions = getPendingEncryptions();

                for (MXQueuedEncryption queuedEncryption : queuedEncryptions) {
                    queuedEncryption.mApiCallback.onMatrixError(e);
                }

                synchronized (mPendingEncryptions) {
                    mPendingEncryptions.removeAll(queuedEncryptions);
                }
            }

            /**
             * An unexpected error has been received while encrypting
             * @param e the exception
             */
            private void dispatchUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## onUnexpectedError() : onMatrixError " + e.getMessage(), e);

                List<MXQueuedEncryption> queuedEncryptions = getPendingEncryptions();

                for (MXQueuedEncryption queuedEncryption : queuedEncryptions) {
                    queuedEncryption.mApiCallback.onUnexpectedError(e);
                }

                synchronized (mPendingEncryptions) {
                    mPendingEncryptions.removeAll(queuedEncryptions);
                }
            }

            @Override
            public void onSuccess(MXUsersDevicesMap<MXDeviceInfo> devicesInRoom) {
                ensureOutboundSession(devicesInRoom, new ApiCallback<MXOutboundSessionInfo>() {
                    @Override
                    public void onSuccess(final MXOutboundSessionInfo session) {
                        mCrypto.getEncryptingThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(LOG_TAG, "## encryptEventContent () processPendingEncryptions after " + (System.currentTimeMillis() - t0) + "ms");
                                processPendingEncryptions(session);
                            }
                        });
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        dispatchNetworkError(e);
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        dispatchMatrixError(e);
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        dispatchUnexpectedError(e);
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                dispatchNetworkError(e);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                dispatchMatrixError(e);
            }

            @Override
            public void onUnexpectedError(Exception e) {
                dispatchUnexpectedError(e);
            }
        });


    }

    /**
     * Prepare a new session.
     *
     * @return the session description
     */
    private MXOutboundSessionInfo prepareNewSessionInRoom() {
        MXOlmDevice olmDevice = mCrypto.getOlmDevice();
        final String sessionId = olmDevice.createOutboundGroupSession();

        Map<String, String> keysClaimedMap = new HashMap<>();
        keysClaimedMap.put("ed25519", olmDevice.getDeviceEd25519Key());

        olmDevice.addInboundGroupSession(sessionId, olmDevice.getSessionKey(sessionId), mRoomId, olmDevice.getDeviceCurve25519Key(),
                new ArrayList<String>(), keysClaimedMap, false);

        return new MXOutboundSessionInfo(sessionId);
    }

    /**
     * Ensure the outbound session
     *
     * @param devicesInRoom the devices list
     * @param callback      the asynchronous callback.
     */
    private void ensureOutboundSession(MXUsersDevicesMap<MXDeviceInfo> devicesInRoom, final ApiCallback<MXOutboundSessionInfo> callback) {
        MXOutboundSessionInfo session = mOutboundSession;

        if ((null == session)
                // Need to make a brand new session?
                || session.needsRotation(mSessionRotationPeriodMsgs, mSessionRotationPeriodMs)
                // Determine if we have shared with anyone we shouldn't have
                || session.sharedWithTooManyDevices(devicesInRoom)) {
            mOutboundSession = session = prepareNewSessionInRoom();
        }

        if (mShareOperationIsProgress) {
            Log.d(LOG_TAG, "## ensureOutboundSessionInRoom() : already in progress");
            // Key share already in progress
            return;
        }

        final MXOutboundSessionInfo fSession = session;

        Map<String, /* userId */List<MXDeviceInfo>> shareMap = new HashMap<>();

        List<String> userIds = devicesInRoom.getUserIds();

        for (String userId : userIds) {
            List<String> deviceIds = devicesInRoom.getUserDeviceIds(userId);

            for (String deviceId : deviceIds) {
                MXDeviceInfo deviceInfo = devicesInRoom.getObject(deviceId, userId);

                if (null == fSession.mSharedWithDevices.getObject(deviceId, userId)) {
                    if (!shareMap.containsKey(userId)) {
                        shareMap.put(userId, new ArrayList<MXDeviceInfo>());
                    }

                    shareMap.get(userId).add(deviceInfo);
                }
            }
        }

        shareKey(fSession, shareMap, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void anything) {
                mShareOperationIsProgress = false;
                if (null != callback) {
                    callback.onSuccess(fSession);
                }
            }

            @Override
            public void onNetworkError(final Exception e) {
                Log.e(LOG_TAG, "## ensureOutboundSessionInRoom() : shareKey onNetworkError " + e.getMessage(), e);

                if (null != callback) {
                    callback.onNetworkError(e);
                }
                mShareOperationIsProgress = false;
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                Log.e(LOG_TAG, "## ensureOutboundSessionInRoom() : shareKey onMatrixError " + e.getMessage());

                if (null != callback) {
                    callback.onMatrixError(e);
                }
                mShareOperationIsProgress = false;
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                Log.e(LOG_TAG, "## ensureOutboundSessionInRoom() : shareKey onUnexpectedError " + e.getMessage(), e);

                if (null != callback) {
                    callback.onUnexpectedError(e);
                }
                mShareOperationIsProgress = false;
            }
        });

    }

    /**
     * Share the device key to a list of users
     *
     * @param session        the session info
     * @param devicesByUsers the devices map
     * @param callback       the asynchronous callback
     */
    private void shareKey(final MXOutboundSessionInfo session,
                          final Map<String, List<MXDeviceInfo>> devicesByUsers,
                          final ApiCallback<Void> callback) {
        // nothing to send, the task is done
        if (0 == devicesByUsers.size()) {
            Log.d(LOG_TAG, "## shareKey() : nothing more to do");

            if (null != callback) {
                mCrypto.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(null);
                    }
                });
            }

            return;
        }

        // reduce the map size to avoid request timeout when there are too devices (Users size  * devices per user)
        Map<String, List<MXDeviceInfo>> subMap = new HashMap<>();

        final List<String> userIds = new ArrayList<>();
        int devicesCount = 0;

        for (String userId : devicesByUsers.keySet()) {
            List<MXDeviceInfo> devicesList = devicesByUsers.get(userId);

            userIds.add(userId);
            subMap.put(userId, devicesList);

            devicesCount += devicesList.size();

            if (devicesCount > 100) {
                break;
            }
        }

        Log.d(LOG_TAG, "## shareKey() ; userId " + userIds);
        shareUserDevicesKey(session, subMap, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                mCrypto.getEncryptingThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        for (String userId : userIds) {
                            devicesByUsers.remove(userId);
                        }
                        shareKey(session, devicesByUsers, callback);
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## shareKey() ; userIds " + userIds + " failed " + e.getMessage(), e);
                if (null != callback) {
                    callback.onNetworkError(e);
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## shareKey() ; userIds " + userIds + " failed " + e.getMessage());
                if (null != callback) {
                    callback.onMatrixError(e);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## shareKey() ; userIds " + userIds + " failed " + e.getMessage(), e);
                if (null != callback) {
                    callback.onUnexpectedError(e);
                }
            }
        });
    }

    /**
     * Share the device keys of a an user
     *
     * @param session       the session info
     * @param devicesByUser the devices map
     * @param callback      the asynchronous callback
     */
    private void shareUserDevicesKey(final MXOutboundSessionInfo session,
                                     final Map<String, List<MXDeviceInfo>> devicesByUser,
                                     final ApiCallback<Void> callback) {
        final String sessionKey = mCrypto.getOlmDevice().getSessionKey(session.mSessionId);
        final int chainIndex = mCrypto.getOlmDevice().getMessageIndex(session.mSessionId);

        Map<String, Object> submap = new HashMap<>();
        submap.put("algorithm", MXCryptoAlgorithms.MXCRYPTO_ALGORITHM_MEGOLM);
        submap.put("room_id", mRoomId);
        submap.put("session_id", session.mSessionId);
        submap.put("session_key", sessionKey);
        submap.put("chain_index", chainIndex);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("type", Event.EVENT_TYPE_ROOM_KEY);
        payload.put("content", submap);

        final long t0 = System.currentTimeMillis();
        Log.d(LOG_TAG, "## shareUserDevicesKey() : starts");

        mCrypto.ensureOlmSessionsForDevices(devicesByUser, new ApiCallback<MXUsersDevicesMap<MXOlmSessionResult>>() {
            @Override
            public void onSuccess(final MXUsersDevicesMap<MXOlmSessionResult> results) {
                mCrypto.getEncryptingThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "## shareUserDevicesKey() : ensureOlmSessionsForDevices succeeds after " + (System.currentTimeMillis() - t0) + " ms");
                        MXUsersDevicesMap<Map<String, Object>> contentMap = new MXUsersDevicesMap<>();

                        boolean haveTargets = false;
                        List<String> userIds = results.getUserIds();

                        for (String userId : userIds) {
                            List<MXDeviceInfo> devicesToShareWith = devicesByUser.get(userId);

                            for (MXDeviceInfo deviceInfo : devicesToShareWith) {
                                String deviceID = deviceInfo.deviceId;

                                MXOlmSessionResult sessionResult = results.getObject(deviceID, userId);

                                if ((null == sessionResult) || (null == sessionResult.mSessionId)) {
                                    // no session with this device, probably because there
                                    // were no one-time keys.
                                    //
                                    // we could send them a to_device message anyway, as a
                                    // signal that they have missed out on the key sharing
                                    // message because of the lack of keys, but there's not
                                    // much point in that really; it will mostly serve to clog
                                    // up to_device inboxes.
                                    //
                                    // ensureOlmSessionsForUsers has already done the logging,
                                    // so just skip it.
                                    continue;
                                }

                                Log.d(LOG_TAG, "## shareUserDevicesKey() : Sharing keys with device " + userId + ":" + deviceID);
                                //noinspection ArraysAsListWithZeroOrOneArgument,ArraysAsListWithZeroOrOneArgument
                                contentMap.setObject(mCrypto.encryptMessage(payload, Arrays.asList(sessionResult.mDevice)), userId, deviceID);
                                haveTargets = true;
                            }
                        }

                        if (haveTargets && !mCrypto.hasBeenReleased()) {
                            final long t0 = System.currentTimeMillis();
                            Log.d(LOG_TAG, "## shareUserDevicesKey() : has target");

                            mSession.getCryptoRestClient().sendToDevice(Event.EVENT_TYPE_MESSAGE_ENCRYPTED, contentMap, new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void info) {
                                    mCrypto.getEncryptingThreadHandler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(LOG_TAG, "## shareUserDevicesKey() : sendToDevice succeeds after "
                                                    + (System.currentTimeMillis() - t0) + " ms");

                                            // Add the devices we have shared with to session.sharedWithDevices.
                                            // we deliberately iterate over devicesByUser (ie, the devices we
                                            // attempted to share with) rather than the contentMap (those we did
                                            // share with), because we don't want to try to claim a one-time-key
                                            // for dead devices on every message.
                                            for (String userId : devicesByUser.keySet()) {
                                                List<MXDeviceInfo> devicesToShareWith = devicesByUser.get(userId);

                                                for (MXDeviceInfo deviceInfo : devicesToShareWith) {
                                                    session.mSharedWithDevices.setObject(chainIndex, userId, deviceInfo.deviceId);
                                                }
                                            }

                                            mCrypto.getUIHandler().post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (null != callback) {
                                                        callback.onSuccess(null);
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }

                                @Override
                                public void onNetworkError(Exception e) {
                                    Log.e(LOG_TAG, "## shareUserDevicesKey() : sendToDevice onNetworkError " + e.getMessage(), e);

                                    if (null != callback) {
                                        callback.onNetworkError(e);
                                    }
                                }

                                @Override
                                public void onMatrixError(MatrixError e) {
                                    Log.e(LOG_TAG, "## shareUserDevicesKey() : sendToDevice onMatrixError " + e.getMessage());

                                    if (null != callback) {
                                        callback.onMatrixError(e);
                                    }
                                }

                                @Override
                                public void onUnexpectedError(Exception e) {
                                    Log.e(LOG_TAG, "## shareUserDevicesKey() : sendToDevice onUnexpectedError " + e.getMessage(), e);

                                    if (null != callback) {
                                        callback.onUnexpectedError(e);
                                    }
                                }
                            });
                        } else {
                            Log.d(LOG_TAG, "## shareUserDevicesKey() : no need to sharekey");

                            if (null != callback) {
                                mCrypto.getUIHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess(null);
                                    }
                                });
                            }
                        }
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## shareUserDevicesKey() : ensureOlmSessionsForDevices failed " + e.getMessage(), e);

                if (null != callback) {
                    callback.onNetworkError(e);
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## shareUserDevicesKey() : ensureOlmSessionsForDevices failed " + e.getMessage());

                if (null != callback) {
                    callback.onMatrixError(e);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## shareUserDevicesKey() : ensureOlmSessionsForDevices failed " + e.getMessage(), e);

                if (null != callback) {
                    callback.onUnexpectedError(e);
                }
            }
        });
    }

    /**
     * process the pending encryptions
     */
    private void processPendingEncryptions(MXOutboundSessionInfo session) {
        if (null != session) {
            List<MXQueuedEncryption> queuedEncryptions = getPendingEncryptions();

            // Everything is in place, encrypt all pending events
            for (MXQueuedEncryption queuedEncryption : queuedEncryptions) {
                Map<String, Object> payloadJson = new HashMap<>();

                payloadJson.put("room_id", mRoomId);
                payloadJson.put("type", queuedEncryption.mEventType);
                payloadJson.put("content", queuedEncryption.mEventContent);

                String payloadString = JsonUtils.convertToUTF8(JsonUtils.canonicalize(JsonUtils.getGson(false).toJsonTree(payloadJson)).toString());
                String ciphertext = mCrypto.getOlmDevice().encryptGroupMessage(session.mSessionId, payloadString);

                final Map<String, Object> map = new HashMap<>();
                map.put("algorithm", MXCryptoAlgorithms.MXCRYPTO_ALGORITHM_MEGOLM);
                map.put("sender_key", mCrypto.getOlmDevice().getDeviceCurve25519Key());
                map.put("ciphertext", ciphertext);
                map.put("session_id", session.mSessionId);

                // Include our device ID so that recipients can send us a
                // m.new_device message if they don't have our session key.
                map.put("device_id", mDeviceId);

                final MXQueuedEncryption fQueuedEncryption = queuedEncryption;
                mCrypto.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        fQueuedEncryption.mApiCallback.onSuccess(JsonUtils.getGson(false).toJsonTree(map));
                    }
                });

                session.mUseCount++;
            }

            synchronized (mPendingEncryptions) {
                mPendingEncryptions.removeAll(queuedEncryptions);
            }
        }
    }

    /**
     * Get the list of devices which can encrypt data to.
     * This method must be called in getDecryptingThreadHandler() thread.
     *
     * @param userIds  the user ids whose devices must be checked.
     * @param callback the asynchronous callback
     */
    private void getDevicesInRoom(final List<String> userIds, final ApiCallback<MXUsersDevicesMap<MXDeviceInfo>> callback) {
        // We are happy to use a cached version here: we assume that if we already
        // have a list of the user's devices, then we already share an e2e room
        // with them, which means that they will have announced any new devices via
        // an m.new_device.
        mCrypto.getDeviceList().downloadKeys(userIds, false, new SimpleApiCallback<MXUsersDevicesMap<MXDeviceInfo>>(callback) {
            @Override
            public void onSuccess(final MXUsersDevicesMap<MXDeviceInfo> devices) {
                mCrypto.getEncryptingThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        boolean encryptToVerifiedDevicesOnly = mCrypto.getGlobalBlacklistUnverifiedDevices()
                                || mCrypto.isRoomBlacklistUnverifiedDevices(mRoomId);

                        final MXUsersDevicesMap<MXDeviceInfo> devicesInRoom = new MXUsersDevicesMap<>();
                        final MXUsersDevicesMap<MXDeviceInfo> unknownDevices = new MXUsersDevicesMap<>();

                        List<String> userIds = devices.getUserIds();

                        for (String userId : userIds) {
                            List<String> deviceIds = devices.getUserDeviceIds(userId);

                            for (String deviceId : deviceIds) {
                                MXDeviceInfo deviceInfo = devices.getObject(deviceId, userId);

                                if (mCrypto.warnOnUnknownDevices() && deviceInfo.isUnknown()) {
                                    // The device is not yet known by the user
                                    unknownDevices.setObject(deviceInfo, userId, deviceId);
                                    continue;
                                }

                                if (deviceInfo.isBlocked()) {
                                    // Remove any blocked devices
                                    continue;
                                }

                                if (!deviceInfo.isVerified() && encryptToVerifiedDevicesOnly) {
                                    continue;
                                }

                                if (TextUtils.equals(deviceInfo.identityKey(), mCrypto.getOlmDevice().getDeviceCurve25519Key())) {
                                    // Don't bother sending to ourself
                                    continue;
                                }

                                devicesInRoom.setObject(deviceInfo, userId, deviceId);
                            }
                        }

                        mCrypto.getUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                // Check if any of these devices are not yet known to the user.
                                // if so, warn the user so they can verify or ignore.
                                if (0 != unknownDevices.getMap().size()) {
                                    callback.onMatrixError(new MXCryptoError(MXCryptoError.UNKNOWN_DEVICES_CODE,
                                            MXCryptoError.UNABLE_TO_ENCRYPT, MXCryptoError.UNKNOWN_DEVICES_REASON, unknownDevices));
                                } else {
                                    callback.onSuccess(devicesInRoom);
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
