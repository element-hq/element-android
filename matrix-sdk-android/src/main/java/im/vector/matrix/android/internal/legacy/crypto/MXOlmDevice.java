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

package im.vector.matrix.android.internal.legacy.crypto;

import android.text.TextUtils;

import com.google.gson.JsonParser;

import im.vector.matrix.android.internal.legacy.crypto.algorithms.MXDecryptionResult;
import im.vector.matrix.android.internal.legacy.crypto.data.MXOlmInboundGroupSession2;
import im.vector.matrix.android.internal.legacy.data.cryptostore.IMXCryptoStore;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import org.matrix.olm.OlmAccount;
import org.matrix.olm.OlmInboundGroupSession;
import org.matrix.olm.OlmMessage;
import org.matrix.olm.OlmOutboundGroupSession;
import org.matrix.olm.OlmSession;
import org.matrix.olm.OlmUtility;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MXOlmDevice {
    private static final String LOG_TAG = MXOlmDevice.class.getSimpleName();

    // Curve25519 key for the account.
    private String mDeviceCurve25519Key;

    // Ed25519 key for the account.
    private String mDeviceEd25519Key;

    // The store where crypto data is saved.
    private final IMXCryptoStore mStore;

    // The OLMKit account instance.
    private OlmAccount mOlmAccount;

    // The OLMKit utility instance.
    private OlmUtility mOlmUtility;

    // The outbound group session.
    // They are not stored in 'store' to avoid to remember to which devices we sent the session key.
    // Plus, in cryptography, it is good to refresh sessions from time to time.
    // The key is the session id, the value the outbound group session.
    private final Map<String, OlmOutboundGroupSession> mOutboundGroupSessionStore;

    // Store a set of decrypted message indexes for each group session.
    // This partially mitigates a replay attack where a MITM resends a group
    // message into the room.
    //
    // The Matrix SDK exposes events through MXEventTimelines. A developer can open several
    // timelines from a same room so that a message can be decrypted several times but from
    // a different timeline.
    // So, store these message indexes per timeline id.
    //
    // The first level keys are timeline ids.
    // The second level keys are strings of form "<senderKey>|<session_id>|<message_index>"
    // Values are true.
    private final Map<String, Map<String, Boolean>> mInboundGroupSessionMessageIndexes;

    /**
     * inboundGroupSessionWithId error
     */
    private MXCryptoError mInboundGroupSessionWithIdError = null;

    /**
     * Constructor
     *
     * @param store the used store
     */
    public MXOlmDevice(IMXCryptoStore store) {
        mStore = store;

        // Retrieve the account from the store
        mOlmAccount = mStore.getAccount();

        if (null == mOlmAccount) {
            Log.d(LOG_TAG, "MXOlmDevice : create a new olm account");
            // Else, create it
            try {
                mOlmAccount = new OlmAccount();
                mStore.storeAccount(mOlmAccount);
            } catch (Exception e) {
                Log.e(LOG_TAG, "MXOlmDevice : cannot initialize mOlmAccount " + e.getMessage(), e);
            }
        } else {
            Log.d(LOG_TAG, "MXOlmDevice : use an existing account");
        }

        try {
            mOlmUtility = new OlmUtility();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## MXOlmDevice : OlmUtility failed with error " + e.getMessage(), e);
            mOlmUtility = null;
        }

        mOutboundGroupSessionStore = new HashMap<>();

        try {
            mDeviceCurve25519Key = mOlmAccount.identityKeys().get(OlmAccount.JSON_KEY_IDENTITY_KEY);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## MXOlmDevice : cannot find " + OlmAccount.JSON_KEY_IDENTITY_KEY + " with error " + e.getMessage(), e);
        }

        try {
            mDeviceEd25519Key = mOlmAccount.identityKeys().get(OlmAccount.JSON_KEY_FINGER_PRINT_KEY);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## MXOlmDevice : cannot find " + OlmAccount.JSON_KEY_FINGER_PRINT_KEY + " with error " + e.getMessage(), e);
        }

        mInboundGroupSessionMessageIndexes = new HashMap<>();
    }

    /**
     * Release the instance
     */
    public void release() {
        if (null != mOlmAccount) {
            mOlmAccount.releaseAccount();
        }
    }

    /**
     * @return the Curve25519 key for the account.
     */
    public String getDeviceCurve25519Key() {
        return mDeviceCurve25519Key;
    }

    /**
     * @return the Ed25519 key for the account.
     */
    public String getDeviceEd25519Key() {
        return mDeviceEd25519Key;
    }

    /**
     * Signs a message with the ed25519 key for this account.
     *
     * @param message the message to be signed.
     * @return the base64-encoded signature.
     */
    private String signMessage(String message) {
        try {
            return mOlmAccount.signMessage(message);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## signMessage() : failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Signs a JSON dictionary with the ed25519 key for this account.
     * The signature is done on canonical version of the JSON.
     *
     * @param JSONDictionary the JSON to be signed.
     * @return the base64-encoded signature
     */
    public String signJSON(Map<String, Object> JSONDictionary) {
        return signMessage(JsonUtils.getCanonicalizedJsonString(JSONDictionary));
    }

    /**
     * @return The current (unused, unpublished) one-time keys for this account.
     */
    public Map<String, Map<String, String>> getOneTimeKeys() {
        try {
            return mOlmAccount.oneTimeKeys();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getOneTimeKeys() : failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * @return The maximum number of one-time keys the olm account can store.
     */
    public long getMaxNumberOfOneTimeKeys() {
        if (null != mOlmAccount) {
            return mOlmAccount.maxOneTimeKeys();
        } else {
            return -1;
        }
    }

    /**
     * Marks all of the one-time keys as published.
     */
    public void markKeysAsPublished() {
        try {
            mOlmAccount.markOneTimeKeysAsPublished();
            mStore.storeAccount(mOlmAccount);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## markKeysAsPublished() : failed " + e.getMessage(), e);
        }
    }

    /**
     * Generate some new one-time keys
     *
     * @param numKeys number of keys to generate
     */
    public void generateOneTimeKeys(int numKeys) {
        try {
            mOlmAccount.generateOneTimeKeys(numKeys);
            mStore.storeAccount(mOlmAccount);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## generateOneTimeKeys() : failed " + e.getMessage(), e);
        }
    }

    /**
     * Generate a new outbound session.
     * The new session will be stored in the MXStore.
     *
     * @param theirIdentityKey the remote user's Curve25519 identity key
     * @param theirOneTimeKey  the remote user's one-time Curve25519 key
     * @return the session id for the outbound session. @TODO OLMSession?
     */
    public String createOutboundSession(String theirIdentityKey, String theirOneTimeKey) {
        Log.d(LOG_TAG, "## createOutboundSession() ; theirIdentityKey " + theirIdentityKey + " theirOneTimeKey " + theirOneTimeKey);
        OlmSession olmSession = null;

        try {
            olmSession = new OlmSession();
            olmSession.initOutboundSession(mOlmAccount, theirIdentityKey, theirOneTimeKey);
            mStore.storeSession(olmSession, theirIdentityKey);

            String sessionIdentifier = olmSession.sessionIdentifier();

            Log.d(LOG_TAG, "## createOutboundSession() ;  olmSession.sessionIdentifier: " + sessionIdentifier);
            return sessionIdentifier;

        } catch (Exception e) {
            Log.e(LOG_TAG, "## createOutboundSession() failed ; " + e.getMessage(), e);

            if (null != olmSession) {
                olmSession.releaseSession();
            }
        }

        return null;
    }

    /**
     * Generate a new inbound session, given an incoming message.
     *
     * @param theirDeviceIdentityKey the remote user's Curve25519 identity key.
     * @param messageType            the message_type field from the received message (must be 0).
     * @param ciphertext             base64-encoded body from the received message.
     * @return {{payload: string, session_id: string}} decrypted payload, andsession id of new session.
     */
    public Map<String, String> createInboundSession(String theirDeviceIdentityKey, int messageType, String ciphertext) {

        Log.d(LOG_TAG, "## createInboundSession() : theirIdentityKey: " + theirDeviceIdentityKey);

        OlmSession olmSession = null;

        try {
            try {
                olmSession = new OlmSession();
                olmSession.initInboundSessionFrom(mOlmAccount, theirDeviceIdentityKey, ciphertext);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## createInboundSession() : the session creation failed " + e.getMessage(), e);
                return null;
            }

            Log.d(LOG_TAG, "## createInboundSession() : sessionId: " + olmSession.sessionIdentifier());

            try {
                mOlmAccount.removeOneTimeKeys(olmSession);
                mStore.storeAccount(mOlmAccount);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## createInboundSession() : removeOneTimeKeys failed " + e.getMessage(), e);
            }

            Log.d(LOG_TAG, "## createInboundSession() : ciphertext: " + ciphertext);
            try {
                Log.d(LOG_TAG, "## createInboundSession() :ciphertext: SHA256:" + mOlmUtility.sha256(URLEncoder.encode(ciphertext, "utf-8")));
            } catch (Exception e) {
                Log.e(LOG_TAG, "## createInboundSession() :ciphertext: cannot encode ciphertext", e);
            }

            OlmMessage olmMessage = new OlmMessage();
            olmMessage.mCipherText = ciphertext;
            olmMessage.mType = messageType;

            String payloadString = null;

            try {
                payloadString = olmSession.decryptMessage(olmMessage);
                mStore.storeSession(olmSession, theirDeviceIdentityKey);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## createInboundSession() : decryptMessage failed " + e.getMessage(), e);
            }

            Map<String, String> res = new HashMap<>();

            if (!TextUtils.isEmpty(payloadString)) {
                res.put("payload", payloadString);
            }

            String sessionIdentifier = olmSession.sessionIdentifier();

            if (!TextUtils.isEmpty(sessionIdentifier)) {
                res.put("session_id", sessionIdentifier);
            }

            return res;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## createInboundSession() : OlmSession creation failed " + e.getMessage(), e);

            if (null != olmSession) {
                olmSession.releaseSession();
            }
        }

        return null;
    }

    /**
     * Get a list of known session IDs for the given device.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @return a list of known session ids for the device.
     */
    public Set<String> getSessionIds(String theirDeviceIdentityKey) {
        Map<String, OlmSession> map = mStore.getDeviceSessions(theirDeviceIdentityKey);

        if (null != map) {
            return map.keySet();
        }

        return null;
    }

    /**
     * Get the right olm session id for encrypting messages to the given identity key.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @return the session id, or nil if no established session.
     */
    public String getSessionId(String theirDeviceIdentityKey) {
        String sessionId = null;
        Set<String> sessionIds = getSessionIds(theirDeviceIdentityKey);

        if ((null != sessionIds) && (0 != sessionIds.size())) {
            List<String> sessionIdsList = new ArrayList<>(sessionIds);
            Collections.sort(sessionIdsList);
            sessionId = sessionIdsList.get(0);
        }

        return sessionId;
    }

    /**
     * Encrypt an outgoing message using an existing session.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @param sessionId              the id of the active session
     * @param payloadString          the payload to be encrypted and sent
     * @return the cipher text
     */
    public Map<String, Object> encryptMessage(String theirDeviceIdentityKey, String sessionId, String payloadString) {
        Map<String, Object> res = null;
        OlmMessage olmMessage;
        OlmSession olmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId);

        if (null != olmSession) {
            try {
                Log.d(LOG_TAG, "## encryptMessage() : olmSession.sessionIdentifier: " + olmSession.sessionIdentifier());
                //Log.d(LOG_TAG, "## encryptMessage() : payloadString: " + payloadString);

                olmMessage = olmSession.encryptMessage(payloadString);
                mStore.storeSession(olmSession, theirDeviceIdentityKey);
                res = new HashMap<>();

                res.put("body", olmMessage.mCipherText);
                res.put("type", olmMessage.mType);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## encryptMessage() : failed " + e.getMessage(), e);
            }
        }

        return res;
    }

    /**
     * Decrypt an incoming message using an existing session.
     *
     * @param ciphertext             the base64-encoded body from the received message.
     * @param messageType            message_type field from the received message.
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @param sessionId              the id of the active session.
     * @return the decrypted payload.
     */
    public String decryptMessage(String ciphertext, int messageType, String sessionId, String theirDeviceIdentityKey) {
        String payloadString = null;

        OlmSession olmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId);

        if (null != olmSession) {
            OlmMessage olmMessage = new OlmMessage();
            olmMessage.mCipherText = ciphertext;
            olmMessage.mType = messageType;

            try {
                payloadString = olmSession.decryptMessage(olmMessage);
                mStore.storeSession(olmSession, theirDeviceIdentityKey);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## decryptMessage() : decryptMessage failed " + e.getMessage(), e);
            }
        }

        return payloadString;
    }

    /**
     * Determine if an incoming messages is a prekey message matching an existing session.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @param sessionId              the id of the active session.
     * @param messageType            message_type field from the received message.
     * @param ciphertext             the base64-encoded body from the received message.
     * @return YES if the received message is a prekey message which matchesthe given session.
     */
    public boolean matchesSession(String theirDeviceIdentityKey, String sessionId, int messageType, String ciphertext) {
        if (messageType != 0) {
            return false;
        }

        OlmSession olmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId);
        return (null != olmSession) && olmSession.matchesInboundSession(ciphertext);
    }


    // Outbound group session

    /**
     * Generate a new outbound group session.
     *
     * @return the session id for the outbound session.
     */
    public String createOutboundGroupSession() {
        OlmOutboundGroupSession session = null;
        try {
            session = new OlmOutboundGroupSession();
            mOutboundGroupSessionStore.put(session.sessionIdentifier(), session);
            return session.sessionIdentifier();
        } catch (Exception e) {
            Log.e(LOG_TAG, "createOutboundGroupSession " + e.getMessage(), e);

            if (null != session) {
                session.releaseSession();
            }
        }
        return null;
    }

    /**
     * Get the current session key of  an outbound group session.
     *
     * @param sessionId the id of the outbound group session.
     * @return the base64-encoded secret key.
     */
    public String getSessionKey(String sessionId) {
        if (!TextUtils.isEmpty(sessionId)) {
            try {
                return mOutboundGroupSessionStore.get(sessionId).sessionKey();
            } catch (Exception e) {
                Log.e(LOG_TAG, "## getSessionKey() : failed " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Get the current message index of an outbound group session.
     *
     * @param sessionId the id of the outbound group session.
     * @return the current chain index.
     */
    public int getMessageIndex(String sessionId) {
        if (!TextUtils.isEmpty(sessionId)) {
            return mOutboundGroupSessionStore.get(sessionId).messageIndex();
        }
        return 0;
    }

    /**
     * Encrypt an outgoing message with an outbound group session.
     *
     * @param sessionId     the id of the outbound group session.
     * @param payloadString the payload to be encrypted and sent.
     * @return ciphertext
     */
    public String encryptGroupMessage(String sessionId, String payloadString) {
        if (!TextUtils.isEmpty(sessionId) && !TextUtils.isEmpty(payloadString)) {
            try {
                return mOutboundGroupSessionStore.get(sessionId).encryptMessage(payloadString);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## encryptGroupMessage() : failed " + e.getMessage(), e);
            }
        }
        return null;
    }

    //  Inbound group session

    /**
     * Add an inbound group session to the session store.
     *
     * @param sessionId                    the session identifier.
     * @param sessionKey                   base64-encoded secret key.
     * @param roomId                       the id of the room in which this session will be used.
     * @param senderKey                    the base64-encoded curve25519 key of the sender.
     * @param forwardingCurve25519KeyChain Devices involved in forwarding this session to us.
     * @param keysClaimed                  Other keys the sender claims.
     * @param exportFormat                 true if the megolm keys are in export format
     * @return true if the operation succeeds.
     */
    public boolean addInboundGroupSession(String sessionId,
                                          String sessionKey,
                                          String roomId,
                                          String senderKey,
                                          List<String> forwardingCurve25519KeyChain,
                                          Map<String, String> keysClaimed,
                                          boolean exportFormat) {
        if (null != getInboundGroupSession(sessionId, senderKey, roomId)) {
            // If we already have this session, consider updating it
            Log.e(LOG_TAG, "## addInboundGroupSession() : Update for megolm session " + senderKey + "/" + sessionId);

            // For now we just ignore updates. TODO: implement something here
            return false;
        }

        MXOlmInboundGroupSession2 session = new MXOlmInboundGroupSession2(sessionKey, exportFormat);

        // sanity check
        if (null == session.mSession) {
            Log.e(LOG_TAG, "## addInboundGroupSession : invalid session");
            return false;
        }

        try {
            if (!TextUtils.equals(session.mSession.sessionIdentifier(), sessionId)) {
                Log.e(LOG_TAG, "## addInboundGroupSession : ERROR: Mismatched group session ID from senderKey: " + senderKey);
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## addInboundGroupSession : sessionIdentifier') failed " + e.getMessage(), e);
            return false;
        }

        session.mSenderKey = senderKey;
        session.mRoomId = roomId;
        session.mKeysClaimed = keysClaimed;
        session.mForwardingCurve25519KeyChain = forwardingCurve25519KeyChain;

        mStore.storeInboundGroupSession(session);

        return true;
    }

    /**
     * Import an inbound group session to the session store.
     *
     * @param exportedSessionMap the exported session map
     * @return the imported session if the operation succeeds.
     */
    public MXOlmInboundGroupSession2 importInboundGroupSession(Map<String, Object> exportedSessionMap) {
        String sessionId = (String) exportedSessionMap.get("session_id");
        String senderKey = (String) exportedSessionMap.get("sender_key");
        String roomId = (String) exportedSessionMap.get("room_id");

        if (null != getInboundGroupSession(sessionId, senderKey, roomId)) {
            // If we already have this session, consider updating it
            Log.e(LOG_TAG, "## importInboundGroupSession() : Update for megolm session " + senderKey + "/" + sessionId);

            // For now we just ignore updates. TODO: implement something here
            return null;
        }

        MXOlmInboundGroupSession2 session = null;

        try {
            session = new MXOlmInboundGroupSession2(exportedSessionMap);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## importInboundGroupSession() : Update for megolm session " + senderKey + "/" + sessionId, e);
        }

        // sanity check
        if ((null == session) || (null == session.mSession)) {
            Log.e(LOG_TAG, "## importInboundGroupSession : invalid session");
            return null;
        }

        try {
            if (!TextUtils.equals(session.mSession.sessionIdentifier(), sessionId)) {
                Log.e(LOG_TAG, "## importInboundGroupSession : ERROR: Mismatched group session ID from senderKey: " + senderKey);
                return null;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## importInboundGroupSession : sessionIdentifier') failed " + e.getMessage(), e);
            return null;
        }

        mStore.storeInboundGroupSession(session);

        return session;
    }

    /**
     * Remove an inbound group session
     *
     * @param sessionId  the session identifier.
     * @param sessionKey base64-encoded secret key.
     */
    public void removeInboundGroupSession(String sessionId, String sessionKey) {
        if ((null != sessionId) && (null != sessionKey)) {
            mStore.removeInboundGroupSession(sessionId, sessionKey);
        }
    }

    /**
     * Decrypt a received message with an inbound group session.
     *
     * @param body      the base64-encoded body of the encrypted message.
     * @param roomId    theroom in which the message was received.
     * @param timeline  the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return the decrypting result. Nil if the sessionId is unknown.
     */
    public MXDecryptionResult decryptGroupMessage(String body,
                                                  String roomId,
                                                  String timeline,
                                                  String sessionId,
                                                  String senderKey) throws MXDecryptionException {
        MXDecryptionResult result = new MXDecryptionResult();
        MXOlmInboundGroupSession2 session = getInboundGroupSession(sessionId, senderKey, roomId);

        if (null != session) {
            // Check that the room id matches the original one for the session. This stops
            // the HS pretending a message was targeting a different room.
            if (TextUtils.equals(roomId, session.mRoomId)) {
                String errorMessage = "";
                OlmInboundGroupSession.DecryptMessageResult decryptResult = null;
                try {
                    decryptResult = session.mSession.decryptMessage(body);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## decryptGroupMessage () : decryptMessage failed " + e.getMessage(), e);
                    errorMessage = e.getMessage();
                }

                if (null != decryptResult) {
                    if (null != timeline) {
                        if (!mInboundGroupSessionMessageIndexes.containsKey(timeline)) {
                            mInboundGroupSessionMessageIndexes.put(timeline, new HashMap<String, Boolean>());
                        }

                        String messageIndexKey = senderKey + "|" + sessionId + "|" + decryptResult.mIndex;

                        if (null != mInboundGroupSessionMessageIndexes.get(timeline).get(messageIndexKey)) {
                            String reason = String.format(MXCryptoError.DUPLICATE_MESSAGE_INDEX_REASON, decryptResult.mIndex);
                            Log.e(LOG_TAG, "## decryptGroupMessage() : " + reason);
                            throw new MXDecryptionException(new MXCryptoError(MXCryptoError.DUPLICATED_MESSAGE_INDEX_ERROR_CODE,
                                    MXCryptoError.UNABLE_TO_DECRYPT, reason));
                        }

                        mInboundGroupSessionMessageIndexes.get(timeline).put(messageIndexKey, true);
                    }

                    mStore.storeInboundGroupSession(session);
                    try {
                        JsonParser parser = new JsonParser();
                        result.mPayload = parser.parse(JsonUtils.convertFromUTF8(decryptResult.mDecryptedMessage));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## decryptGroupMessage() : RLEncoder.encode failed " + e.getMessage(), e);
                        return null;
                    }

                    if (null == result.mPayload) {
                        Log.e(LOG_TAG, "## decryptGroupMessage() : fails to parse the payload");
                        return null;
                    }

                    result.mKeysClaimed = session.mKeysClaimed;
                    result.mSenderKey = senderKey;
                    result.mForwardingCurve25519KeyChain = session.mForwardingCurve25519KeyChain;
                } else {
                    Log.e(LOG_TAG, "## decryptGroupMessage() : failed to decode the message");
                    throw new MXDecryptionException(new MXCryptoError(MXCryptoError.OLM_ERROR_CODE, errorMessage, null));
                }
            } else {
                String reason = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, session.mRoomId);
                Log.e(LOG_TAG, "## decryptGroupMessage() : " + reason);
                throw new MXDecryptionException(new MXCryptoError(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_DECRYPT, reason));
            }
        } else {
            Log.e(LOG_TAG, "## decryptGroupMessage() : Cannot retrieve inbound group session " + sessionId);
            throw new MXDecryptionException(mInboundGroupSessionWithIdError);
        }

        return result;
    }

    /**
     * Reset replay attack data for the given timeline.
     *
     * @param timeline the id of the timeline.
     */
    public void resetReplayAttackCheckInTimeline(String timeline) {
        if (null != timeline) {
            mInboundGroupSessionMessageIndexes.remove(timeline);
        }
    }

    //  Utilities

    /**
     * Verify an ed25519 signature on a JSON object.
     *
     * @param key           the ed25519 key.
     * @param JSONDictinary the JSON object which was signed.
     * @param signature     the base64-encoded signature to be checked.
     * @throws Exception the exception
     */
    public void verifySignature(String key, Map<String, Object> JSONDictinary, String signature) throws Exception {
        // Check signature on the canonical version of the JSON
        mOlmUtility.verifyEd25519Signature(signature, key, JsonUtils.getCanonicalizedJsonString(JSONDictinary));
    }

    /**
     * Calculate the SHA-256 hash of the input and encodes it as base64.
     *
     * @param message the message to hash.
     * @return the base64-encoded hash value.
     */
    public String sha256(String message) {
        return mOlmUtility.sha256(JsonUtils.convertToUTF8(message));
    }

    /**
     * Search an OlmSession
     *
     * @param theirDeviceIdentityKey the device key
     * @param sessionId              the session Id
     * @return the olm session
     */
    private OlmSession getSessionForDevice(String theirDeviceIdentityKey, String sessionId) {
        // sanity check
        if (!TextUtils.isEmpty(theirDeviceIdentityKey) && !TextUtils.isEmpty(sessionId)) {
            Map<String, OlmSession> map = mStore.getDeviceSessions(theirDeviceIdentityKey);

            if (null != map) {
                return map.get(sessionId);
            }
        }

        return null;
    }

    /**
     * Extract an InboundGroupSession from the session store and do some check.
     * mInboundGroupSessionWithIdError describes the failure reason.
     *
     * @param roomId    the room where the sesion is used.
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return the inbound group session.
     */
    public MXOlmInboundGroupSession2 getInboundGroupSession(String sessionId, String senderKey, String roomId) {
        mInboundGroupSessionWithIdError = null;

        MXOlmInboundGroupSession2 session = mStore.getInboundGroupSession(sessionId, senderKey);

        if (null != session) {
            // Check that the room id matches the original one for the session. This stops
            // the HS pretending a message was targeting a different room.
            if (!TextUtils.equals(roomId, session.mRoomId)) {
                String errorDescription = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, session.mRoomId);
                Log.e(LOG_TAG, "## getInboundGroupSession() : " + errorDescription);
                mInboundGroupSessionWithIdError = new MXCryptoError(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_DECRYPT, errorDescription);
            }
        } else {
            Log.e(LOG_TAG, "## getInboundGroupSession() : Cannot retrieve inbound group session " + sessionId);
            mInboundGroupSessionWithIdError = new MXCryptoError(MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE,
                    MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_REASON, null);
        }
        return session;
    }

    /**
     * Determine if we have the keys for a given megolm session.
     *
     * @param roomId    room in which the message was received
     * @param senderKey base64-encoded curve25519 key of the sender
     * @param sessionId session identifier
     * @return true if the unbound session keys are known.
     */
    public boolean hasInboundSessionKeys(String roomId, String senderKey, String sessionId) {
        return null != getInboundGroupSession(sessionId, senderKey, roomId);
    }
}
