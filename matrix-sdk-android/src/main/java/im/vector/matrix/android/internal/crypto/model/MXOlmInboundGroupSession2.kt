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

package im.vector.matrix.android.internal.crypto.model

import android.text.TextUtils
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.MegolmSessionData
import org.matrix.olm.OlmInboundGroupSession
import timber.log.Timber
import java.io.Serializable
import java.util.*

/**
 * This class adds more context to a OLMInboundGroupSession object.
 * This allows additional checks. The class implements Serializable so that the context can be stored.
 */
class MXOlmInboundGroupSession2 : Serializable {

    // The associated olm inbound group session.
    var mSession: OlmInboundGroupSession? = null

    // The room in which this session is used.
    var mRoomId: String? = null

    // The base64-encoded curve25519 key of the sender.
    var mSenderKey: String? = null

    // Other keys the sender claims.
    var mKeysClaimed: Map<String, String>? = null

    // Devices which forwarded this session to us (normally empty).
    var mForwardingCurve25519KeyChain: List<String>? = ArrayList()

    /**
     * @return the first known message index
     */
    val firstKnownIndex: Long?
        get() {
            if (null != mSession) {
                try {
                    return mSession!!.firstKnownIndex
                } catch (e: Exception) {
                    Timber.e(e, "## getFirstKnownIndex() : getFirstKnownIndex failed")
                }

            }

            return null
        }

    /**
     * Constructor
     *
     * @param prevFormatSession the previous session format
     */
    constructor(prevFormatSession: MXOlmInboundGroupSession) {
        mSession = prevFormatSession.mSession
        mRoomId = prevFormatSession.mRoomId
        mSenderKey = prevFormatSession.mSenderKey
        mKeysClaimed = prevFormatSession.mKeysClaimed
    }

    /**
     * Constructor
     *
     * @param sessionKey the session key
     * @param isImported true if it is an imported session key
     */
    constructor(sessionKey: String, isImported: Boolean) {
        try {
            if (!isImported) {
                mSession = OlmInboundGroupSession(sessionKey)
            } else {
                mSession = OlmInboundGroupSession.importSession(sessionKey)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot create")
        }

    }

    /**
     * Create a new instance from the provided keys map.
     *
     * @param megolmSessionData the megolm session data
     * @throws Exception if the data are invalid
     */
    @Throws(Exception::class)
    constructor(megolmSessionData: MegolmSessionData) {
        try {
            mSession = OlmInboundGroupSession.importSession(megolmSessionData.sessionKey!!)

            if (!TextUtils.equals(mSession!!.sessionIdentifier(), megolmSessionData.sessionId)) {
                throw Exception("Mismatched group session Id")
            }

            mSenderKey = megolmSessionData.senderKey
            mKeysClaimed = megolmSessionData.senderClaimedKeys
            mRoomId = megolmSessionData.roomId
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    /**
     * Export the inbound group session keys
     *
     * @return the inbound group session as MegolmSessionData if the operation succeeds
     */
    fun exportKeys(): MegolmSessionData? {
        var megolmSessionData: MegolmSessionData? = MegolmSessionData()

        try {
            if (null == mForwardingCurve25519KeyChain) {
                mForwardingCurve25519KeyChain = ArrayList()
            }

            megolmSessionData!!.senderClaimedEd25519Key = mKeysClaimed!!["ed25519"]
            megolmSessionData.forwardingCurve25519KeyChain = ArrayList(mForwardingCurve25519KeyChain!!)
            megolmSessionData.senderKey = mSenderKey
            megolmSessionData.senderClaimedKeys = mKeysClaimed
            megolmSessionData.roomId = mRoomId
            megolmSessionData.sessionId = mSession!!.sessionIdentifier()
            megolmSessionData.sessionKey = mSession!!.export(mSession!!.firstKnownIndex)
            megolmSessionData.algorithm = MXCRYPTO_ALGORITHM_MEGOLM
        } catch (e: Exception) {
            megolmSessionData = null
            Timber.e(e, "## export() : senderKey " + mSenderKey + " failed")
        }

        return megolmSessionData
    }

    /**
     * Export the session for a message index.
     *
     * @param messageIndex the message index
     * @return the exported data
     */
    fun exportSession(messageIndex: Long): String? {
        if (null != mSession) {
            try {
                return mSession!!.export(messageIndex)
            } catch (e: Exception) {
                Timber.e(e, "## exportSession() : export failed")
            }

        }

        return null
    }
}