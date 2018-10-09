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

package im.vector.matrix.android.internal.legacy.crypto.data;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.crypto.MXCryptoAlgorithms;
import im.vector.matrix.android.internal.legacy.util.Log;
import org.matrix.olm.OlmInboundGroupSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class adds more context to a OLMInboundGroupSession object.
 * This allows additional checks. The class implements NSCoding so that the context can be stored.
 */
public class MXOlmInboundGroupSession2 implements Serializable {
    //
    private static final String LOG_TAG = "OlmInboundGroupSession";

    // define a serialVersionUID to avoid having to redefine the class after updates
    private static final long serialVersionUID = 201702011617L;

    // The associated olm inbound group session.
    public OlmInboundGroupSession mSession;

    // The room in which this session is used.
    public String mRoomId;

    // The base64-encoded curve25519 key of the sender.
    public String mSenderKey;

    // Other keys the sender claims.
    public Map<String, String> mKeysClaimed;

    // Devices which forwarded this session to us (normally empty).
    public List<String> mForwardingCurve25519KeyChain = new ArrayList<>();

    /**
     * Constructor
     *
     * @param prevFormatSession the previous session format
     */
    public MXOlmInboundGroupSession2(MXOlmInboundGroupSession prevFormatSession) {
        mSession = prevFormatSession.mSession;
        mRoomId = prevFormatSession.mRoomId;
        mSenderKey = prevFormatSession.mSenderKey;
        mKeysClaimed = prevFormatSession.mKeysClaimed;
    }

    /**
     * Constructor
     *
     * @param sessionKey the session key
     * @param isImported true if it is an imported session key
     */
    public MXOlmInboundGroupSession2(String sessionKey, boolean isImported) {
        try {
            if (!isImported) {
                mSession = new OlmInboundGroupSession(sessionKey);
            } else {
                mSession = OlmInboundGroupSession.importSession(sessionKey);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot create : " + e.getMessage(), e);
        }
    }

    /**
     * Create a new instance from the provided keys map.
     *
     * @param map the map
     * @throws Exception if the data are invalid
     */
    public MXOlmInboundGroupSession2(Map<String, Object> map) throws Exception {
        try {
            mSession = OlmInboundGroupSession.importSession((String) map.get("session_key"));

            if (!TextUtils.equals(mSession.sessionIdentifier(), (String) map.get("session_id"))) {
                throw new Exception("Mismatched group session Id");
            }

            mSenderKey = (String) map.get("sender_key");
            mKeysClaimed = (Map<String, String>) map.get("sender_claimed_keys");
            mRoomId = (String) map.get("room_id");
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Export the inbound group session keys
     *
     * @return the inbound group session as map if the operation succeeds
     */
    public Map<String, Object> exportKeys() {
        Map<String, Object> map = new HashMap<>();

        try {
            if (null == mForwardingCurve25519KeyChain) {
                mForwardingCurve25519KeyChain = new ArrayList<>();
            }

            map.put("sender_claimed_ed25519_key", mKeysClaimed.get("ed25519"));
            map.put("forwardingCurve25519KeyChain", mForwardingCurve25519KeyChain);
            map.put("sender_key", mSenderKey);
            map.put("sender_claimed_keys", mKeysClaimed);
            map.put("room_id", mRoomId);
            map.put("session_id", mSession.sessionIdentifier());
            map.put("session_key", mSession.export(mSession.getFirstKnownIndex()));
            map.put("algorithm", MXCryptoAlgorithms.MXCRYPTO_ALGORITHM_MEGOLM);
        } catch (Exception e) {
            map = null;
            Log.e(LOG_TAG, "## export() : senderKey " + mSenderKey + " failed " + e.getMessage(), e);
        }

        return map;
    }

    /**
     * @return the first known message index
     */
    public Long getFirstKnownIndex() {
        if (null != mSession) {
            try {
                return mSession.getFirstKnownIndex();
            } catch (Exception e) {
                Log.e(LOG_TAG, "## getFirstKnownIndex() : getFirstKnownIndex failed " + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Export the session for a message index.
     *
     * @param messageIndex the message index
     * @return the exported data
     */
    public String exportSession(long messageIndex) {
        if (null != mSession) {
            try {
                return mSession.export(messageIndex);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## exportSession() : export failed " + e.getMessage(), e);
            }
        }

        return null;
    }
}