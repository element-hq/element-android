/*
 * Copyright 2016 OpenMarket Ltd
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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.crypto.IncomingRoomKeyRequest;
import im.vector.matrix.android.internal.legacy.crypto.MXCryptoError;
import im.vector.matrix.android.internal.legacy.crypto.MXDecryptionException;
import im.vector.matrix.android.internal.legacy.crypto.MXEventDecryptionResult;
import im.vector.matrix.android.internal.legacy.crypto.MXOlmDevice;
import im.vector.matrix.android.internal.legacy.crypto.algorithms.IMXDecrypting;
import im.vector.matrix.android.internal.legacy.crypto.algorithms.MXDecryptionResult;
import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.crypto.data.MXOlmInboundGroupSession2;
import im.vector.matrix.android.internal.legacy.crypto.data.MXOlmSessionResult;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedEventContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.ForwardedRoomKeyContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyRequestBody;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MXMegolmDecryption implements IMXDecrypting {
    private static final String LOG_TAG = MXMegolmDecryption.class.getSimpleName();

    /**
     * The olm device interface
     */
    private MXOlmDevice mOlmDevice;

    // the matrix session
    private MXSession mSession;

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
    private Map<String, /* senderKey|sessionId */
            Map<String /* timelineId */, List<Event>>> mPendingEvents;

    /**
     * Init the object fields
     *
     * @param matrixSession the matrix session
     */
    @Override
    public void initWithMatrixSession(MXSession matrixSession) {
        mSession = matrixSession;
        mOlmDevice = matrixSession.getCrypto().getOlmDevice();
        mPendingEvents = new HashMap<>();

    }

    @Override
    @Nullable
    public MXEventDecryptionResult decryptEvent(Event event, String timeline) throws MXDecryptionException {
        return decryptEvent(event, timeline, true);
    }

    @Nullable
    private MXEventDecryptionResult decryptEvent(Event event, String timeline, boolean requestKeysOnFail) throws MXDecryptionException {
        // sanity check
        if (null == event) {
            Log.e(LOG_TAG, "## decryptEvent() : null event");
            return null;
        }

        EncryptedEventContent encryptedEventContent = JsonUtils.toEncryptedEventContent(event.getWireContent().getAsJsonObject());

        String senderKey = encryptedEventContent.sender_key;
        String ciphertext = encryptedEventContent.ciphertext;
        String sessionId = encryptedEventContent.session_id;

        if (TextUtils.isEmpty(senderKey) || TextUtils.isEmpty(sessionId) || TextUtils.isEmpty(ciphertext)) {
            throw new MXDecryptionException(new MXCryptoError(MXCryptoError.MISSING_FIELDS_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_FIELDS_REASON));
        }

        MXEventDecryptionResult eventDecryptionResult = null;
        MXCryptoError cryptoError = null;
        MXDecryptionResult decryptGroupMessageResult = null;

        try {
            decryptGroupMessageResult = mOlmDevice.decryptGroupMessage(ciphertext, event.roomId, timeline, sessionId, senderKey);
        } catch (MXDecryptionException e) {
            cryptoError = e.getCryptoError();
        }

        // the decryption succeeds
        if ((null != decryptGroupMessageResult) && (null != decryptGroupMessageResult.mPayload) && (null == cryptoError)) {
            eventDecryptionResult = new MXEventDecryptionResult();

            eventDecryptionResult.mClearEvent = decryptGroupMessageResult.mPayload;
            eventDecryptionResult.mSenderCurve25519Key = decryptGroupMessageResult.mSenderKey;

            if (null != decryptGroupMessageResult.mKeysClaimed) {
                eventDecryptionResult.mClaimedEd25519Key = decryptGroupMessageResult.mKeysClaimed.get("ed25519");
            }

            eventDecryptionResult.mForwardingCurve25519KeyChain = decryptGroupMessageResult.mForwardingCurve25519KeyChain;
        } else if (null != cryptoError) {
            if (cryptoError.isOlmError()) {
                if (TextUtils.equals("UNKNOWN_MESSAGE_INDEX", cryptoError.error)) {
                    addEventToPendingList(event, timeline);

                    if (requestKeysOnFail) {
                        requestKeysForEvent(event);
                    }
                }

                String reason = String.format(MXCryptoError.OLM_REASON, cryptoError.error);
                String detailedReason = String.format(MXCryptoError.DETAILLED_OLM_REASON, ciphertext, cryptoError.error);

                throw new MXDecryptionException(new MXCryptoError(
                        MXCryptoError.OLM_ERROR_CODE,
                        reason,
                        detailedReason));
            } else if (TextUtils.equals(cryptoError.errcode, MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE)) {
                addEventToPendingList(event, timeline);
                if (requestKeysOnFail) {
                    requestKeysForEvent(event);
                }
            }

            throw new MXDecryptionException(cryptoError);
        }

        return eventDecryptionResult;
    }

    /**
     * Helper for the real decryptEvent and for _retryDecryption. If
     * requestKeysOnFail is true, we'll send an m.room_key_request when we fail
     * to decrypt the event due to missing megolm keys.
     *
     * @param event the event
     */
    private void requestKeysForEvent(Event event) {
        String sender = event.getSender();
        EncryptedEventContent wireContent = JsonUtils.toEncryptedEventContent(event.getWireContent());

        List<Map<String, String>> recipients = new ArrayList<>();

        Map<String, String> selfMap = new HashMap<>();
        selfMap.put("userId", mSession.getMyUserId());
        selfMap.put("deviceId", "*");
        recipients.add(selfMap);

        if (!TextUtils.equals(sender, mSession.getMyUserId())) {
            Map<String, String> senderMap = new HashMap<>();
            senderMap.put("userId", sender);
            senderMap.put("deviceId", wireContent.device_id);
            recipients.add(senderMap);
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("room_id", event.roomId);
        requestBody.put("algorithm", wireContent.algorithm);
        requestBody.put("sender_key", wireContent.sender_key);
        requestBody.put("session_id", wireContent.session_id);

        mSession.getCrypto().requestRoomKey(requestBody, recipients);
    }

    /**
     * Add an event to the list of those we couldn't decrypt the first time we
     * saw them.
     *
     * @param event      the event to try to decrypt later
     * @param timelineId the timeline identifier
     */
    private void addEventToPendingList(Event event, String timelineId) {
        EncryptedEventContent encryptedEventContent = JsonUtils.toEncryptedEventContent(event.getWireContent().getAsJsonObject());

        String senderKey = encryptedEventContent.sender_key;
        String sessionId = encryptedEventContent.session_id;

        String k = senderKey + "|" + sessionId;

        // avoid undefined timelineId
        if (TextUtils.isEmpty(timelineId)) {
            timelineId = "";
        }

        if (!mPendingEvents.containsKey(k)) {
            mPendingEvents.put(k, new HashMap<String, List<Event>>());
        }

        if (!mPendingEvents.get(k).containsKey(timelineId)) {
            mPendingEvents.get(k).put(timelineId, new ArrayList<Event>());
        }

        if (mPendingEvents.get(k).get(timelineId).indexOf(event) < 0) {
            Log.d(LOG_TAG, "## addEventToPendingList() : add Event " + event.eventId + " in room id " + event.roomId);
            mPendingEvents.get(k).get(timelineId).add(event);
        }
    }

    /**
     * Handle a key event.
     *
     * @param roomKeyEvent the key event.
     */
    @Override
    public void onRoomKeyEvent(Event roomKeyEvent) {
        boolean exportFormat = false;
        RoomKeyContent roomKeyContent = JsonUtils.toRoomKeyContent(roomKeyEvent.getContentAsJsonObject());

        String roomId = roomKeyContent.room_id;
        String sessionId = roomKeyContent.session_id;
        String sessionKey = roomKeyContent.session_key;
        String senderKey = roomKeyEvent.senderKey();
        Map<String, String> keysClaimed = new HashMap<>();
        List<String> forwarding_curve25519_key_chain = null;

        if (TextUtils.isEmpty(roomId) || TextUtils.isEmpty(sessionId) || TextUtils.isEmpty(sessionKey)) {
            Log.e(LOG_TAG, "## onRoomKeyEvent() :  Key event is missing fields");
            return;
        }

        if (TextUtils.equals(roomKeyEvent.getType(), Event.EVENT_TYPE_FORWARDED_ROOM_KEY)) {
            Log.d(LOG_TAG, "## onRoomKeyEvent(), forward adding key : roomId " + roomId + " sessionId " + sessionId
                    + " sessionKey " + sessionKey); // from " + event);
            ForwardedRoomKeyContent forwardedRoomKeyContent = JsonUtils.toForwardedRoomKeyContent(roomKeyEvent.getContentAsJsonObject());

            if (null == forwardedRoomKeyContent.forwarding_curve25519_key_chain) {
                forwarding_curve25519_key_chain = new ArrayList<>();
            } else {
                forwarding_curve25519_key_chain = new ArrayList<>(forwardedRoomKeyContent.forwarding_curve25519_key_chain);
            }

            forwarding_curve25519_key_chain.add(senderKey);

            exportFormat = true;
            senderKey = forwardedRoomKeyContent.sender_key;
            if (null == senderKey) {
                Log.e(LOG_TAG, "## onRoomKeyEvent() : forwarded_room_key event is missing sender_key field");
                return;
            }

            String ed25519Key = forwardedRoomKeyContent.sender_claimed_ed25519_key;

            if (null == ed25519Key) {
                Log.e(LOG_TAG, "## forwarded_room_key_event is missing sender_claimed_ed25519_key field");
                return;
            }

            keysClaimed.put("ed25519", ed25519Key);
        } else {
            Log.d(LOG_TAG, "## onRoomKeyEvent(), Adding key : roomId " + roomId + " sessionId " + sessionId
                    + " sessionKey " + sessionKey); // from " + event);

            if (null == senderKey) {
                Log.e(LOG_TAG, "## onRoomKeyEvent() : key event has no sender key (not encrypted?)");
                return;
            }

            // inherit the claimed ed25519 key from the setup message
            keysClaimed = roomKeyEvent.getKeysClaimed();
        }

        mOlmDevice.addInboundGroupSession(sessionId, sessionKey, roomId, senderKey, forwarding_curve25519_key_chain, keysClaimed, exportFormat);

        Map<String, String> content = new HashMap<>();
        content.put("algorithm", roomKeyContent.algorithm);
        content.put("room_id", roomKeyContent.room_id);
        content.put("session_id", roomKeyContent.session_id);
        content.put("sender_key", senderKey);
        mSession.getCrypto().cancelRoomKeyRequest(content);

        onNewSession(senderKey, sessionId);
    }

    /**
     * Check if the some messages can be decrypted with a new session
     *
     * @param senderKey the session sender key
     * @param sessionId the session id
     */
    public void onNewSession(String senderKey, String sessionId) {
        String k = senderKey + "|" + sessionId;

        Map<String, List<Event>> pending = mPendingEvents.get(k);

        if (null != pending) {
            // Have another go at decrypting events sent with this session.
            mPendingEvents.remove(k);

            Set<String> timelineIds = pending.keySet();

            for (String timelineId : timelineIds) {
                List<Event> events = pending.get(timelineId);

                for (Event event : events) {
                    MXEventDecryptionResult result = null;

                    try {
                        result = decryptEvent(event, TextUtils.isEmpty(timelineId) ? null : timelineId);
                    } catch (MXDecryptionException e) {
                        Log.e(LOG_TAG, "## onNewSession() : Still can't decrypt " + event.eventId + ". Error " + e.getMessage(), e);
                        event.setCryptoError(e.getCryptoError());
                    }

                    if (null != result) {
                        final Event fEvent = event;
                        final MXEventDecryptionResult fResut = result;
                        mSession.getCrypto().getUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                fEvent.setClearData(fResut);
                                mSession.getDataHandler().onEventDecrypted(fEvent);
                            }
                        });
                        Log.d(LOG_TAG, "## onNewSession() : successful re-decryption of " + event.eventId);
                    }
                }
            }
        }
    }

    @Override
    public boolean hasKeysForKeyRequest(IncomingRoomKeyRequest request) {
        return (null != request)
                && (null != request.mRequestBody)
                && mOlmDevice.hasInboundSessionKeys(request.mRequestBody.room_id, request.mRequestBody.sender_key, request.mRequestBody.session_id);
    }

    @Override
    public void shareKeysWithDevice(final IncomingRoomKeyRequest request) {
        // sanity checks
        if ((null == request) || (null == request.mRequestBody)) {
            return;
        }

        final String userId = request.mUserId;

        mSession.getCrypto().getDeviceList().downloadKeys(Arrays.asList(userId), false, new ApiCallback<MXUsersDevicesMap<MXDeviceInfo>>() {
            @Override
            public void onSuccess(MXUsersDevicesMap<MXDeviceInfo> devicesMap) {
                final String deviceId = request.mDeviceId;
                final MXDeviceInfo deviceInfo = mSession.getCrypto().mCryptoStore.getUserDevice(deviceId, userId);

                if (null != deviceInfo) {
                    final RoomKeyRequestBody body = request.mRequestBody;

                    Map<String, List<MXDeviceInfo>> devicesByUser = new HashMap<>();
                    devicesByUser.put(userId, new ArrayList<>(Arrays.asList(deviceInfo)));

                    mSession.getCrypto().ensureOlmSessionsForDevices(devicesByUser, new ApiCallback<MXUsersDevicesMap<MXOlmSessionResult>>() {
                        @Override
                        public void onSuccess(MXUsersDevicesMap<MXOlmSessionResult> map) {
                            MXOlmSessionResult olmSessionResult = map.getObject(deviceId, userId);

                            if ((null == olmSessionResult) || (null == olmSessionResult.mSessionId)) {
                                // no session with this device, probably because there
                                // were no one-time keys.
                                //
                                // ensureOlmSessionsForUsers has already done the logging,
                                // so just skip it.
                                return;
                            }

                            Log.d(LOG_TAG, "## shareKeysWithDevice() : sharing keys for session " + body.sender_key + "|" + body.session_id
                                    + " with device " + userId + ":" + deviceId);

                            MXOlmInboundGroupSession2 inboundGroupSession = mSession.getCrypto()
                                    .getOlmDevice().getInboundGroupSession(body.session_id, body.sender_key, body.room_id);

                            Map<String, Object> payloadJson = new HashMap<>();
                            payloadJson.put("type", Event.EVENT_TYPE_FORWARDED_ROOM_KEY);
                            payloadJson.put("content", inboundGroupSession.exportKeys());

                            Map<String, Object> encodedPayload = mSession.getCrypto().encryptMessage(payloadJson, Arrays.asList(deviceInfo));
                            MXUsersDevicesMap<Map<String, Object>> sendToDeviceMap = new MXUsersDevicesMap<>();
                            sendToDeviceMap.setObject(encodedPayload, userId, deviceId);

                            Log.d(LOG_TAG, "## shareKeysWithDevice() : sending to " + userId + ":" + deviceId);
                            mSession.getCryptoRestClient().sendToDevice(Event.EVENT_TYPE_MESSAGE_ENCRYPTED, sendToDeviceMap, new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void info) {
                                    Log.d(LOG_TAG, "## shareKeysWithDevice() : sent to " + userId + ":" + deviceId);
                                }

                                @Override
                                public void onNetworkError(Exception e) {
                                    Log.e(LOG_TAG, "## shareKeysWithDevice() : sendToDevice " + userId + ":" + deviceId + " failed " + e.getMessage(), e);
                                }

                                @Override
                                public void onMatrixError(MatrixError e) {
                                    Log.e(LOG_TAG, "## shareKeysWithDevice() : sendToDevice " + userId + ":" + deviceId + " failed " + e.getMessage());
                                }

                                @Override
                                public void onUnexpectedError(Exception e) {
                                    Log.e(LOG_TAG, "## shareKeysWithDevice() : sendToDevice " + userId + ":" + deviceId + " failed " + e.getMessage(), e);
                                }
                            });
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            Log.e(LOG_TAG, "## shareKeysWithDevice() : ensureOlmSessionsForDevices " + userId + ":" + deviceId + " failed "
                                    + e.getMessage(), e);
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            Log.e(LOG_TAG, "## shareKeysWithDevice() : ensureOlmSessionsForDevices " + userId + ":" + deviceId + " failed " + e.getMessage());
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            Log.e(LOG_TAG, "## shareKeysWithDevice() : ensureOlmSessionsForDevices " + userId + ":" + deviceId + " failed "
                                    + e.getMessage(), e);
                        }
                    });
                } else {
                    Log.e(LOG_TAG, "## shareKeysWithDevice() : ensureOlmSessionsForDevices " + userId + ":" + deviceId + " not found");
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## shareKeysWithDevice() : downloadKeys " + userId + " failed " + e.getMessage(), e);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## shareKeysWithDevice() : downloadKeys " + userId + " failed " + e.getMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## shareKeysWithDevice() : downloadKeys " + userId + " failed " + e.getMessage(), e);
            }
        });
    }
}
