/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.model;

import org.matrix.olm.OlmInboundGroupSession;

import java.io.Serializable;
import java.util.Map;

import timber.log.Timber;


/**
 * This class adds more context to a OLMInboundGroupSession object.
 * This allows additional checks. The class implements NSCoding so that the context can be stored.
 */
public class MXOlmInboundGroupSession implements Serializable {
    //
    private static final String LOG_TAG = "OlmInboundGroupSession";

    // The associated olm inbound group session.
    public OlmInboundGroupSession mSession;

    // The room in which this session is used.
    public String mRoomId;

    // The base64-encoded curve25519 key of the sender.
    public String mSenderKey;

    // Other keys the sender claims.
    public Map<String, String> mKeysClaimed;

    /**
     * Constructor
     *
     * @param sessionKey the session key
     */
    public MXOlmInboundGroupSession(String sessionKey) {
        try {
            mSession = new OlmInboundGroupSession(sessionKey);
        } catch (Exception e) {
            Timber.e(e, "Cannot create");
        }
    }
}