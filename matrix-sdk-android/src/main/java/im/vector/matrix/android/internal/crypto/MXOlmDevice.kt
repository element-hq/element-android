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

package im.vector.matrix.android.internal.crypto

import android.text.TextUtils
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.algorithms.MXDecryptionResult
import im.vector.matrix.android.internal.crypto.model.MXOlmInboundGroupSession2
import im.vector.matrix.android.internal.crypto.model.MXOlmSession
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.util.convertFromUTF8
import im.vector.matrix.android.internal.util.convertToUTF8
import org.matrix.olm.*
import timber.log.Timber
import java.net.URLEncoder
import java.util.*

// The libolm wrapper.
internal class MXOlmDevice(
        /**
         * The store where crypto data is saved.
         */
        private val mStore: IMXCryptoStore) {

    /**
     * @return the Curve25519 key for the account.
     */
    var deviceCurve25519Key: String? = null
        private set

    /**
     * @return the Ed25519 key for the account.
     */
    var deviceEd25519Key: String? = null
        private set

    // The OLMKit account instance.
    private var mOlmAccount: OlmAccount? = null

    // The OLMKit utility instance.
    private var mOlmUtility: OlmUtility? = null

    // The outbound group session.
    // They are not stored in 'store' to avoid to remember to which devices we sent the session key.
    // Plus, in cryptography, it is good to refresh sessions from time to time.
    // The key is the session id, the value the outbound group session.
    private val mOutboundGroupSessionStore: MutableMap<String, OlmOutboundGroupSession>

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
    private val mInboundGroupSessionMessageIndexes: MutableMap<String, MutableMap<String, Boolean>>

    /**
     * inboundGroupSessionWithId error
     */
    private var mInboundGroupSessionWithIdError: MXCryptoError? = null

    init {
        // Retrieve the account from the store
        mOlmAccount = mStore.getAccount()

        if (null == mOlmAccount) {
            Timber.d("MXOlmDevice : create a new olm account")
            // Else, create it
            try {
                mOlmAccount = OlmAccount()
                mStore.storeAccount(mOlmAccount!!)
            } catch (e: Exception) {
                Timber.e(e, "MXOlmDevice : cannot initialize mOlmAccount")
            }

        } else {
            Timber.d("MXOlmDevice : use an existing account")
        }

        try {
            mOlmUtility = OlmUtility()
        } catch (e: Exception) {
            Timber.e(e, "## MXOlmDevice : OlmUtility failed with error")
            mOlmUtility = null
        }

        mOutboundGroupSessionStore = HashMap()

        try {
            deviceCurve25519Key = mOlmAccount!!.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY]
        } catch (e: Exception) {
            Timber.e(e, "## MXOlmDevice : cannot find " + OlmAccount.JSON_KEY_IDENTITY_KEY + " with error")
        }

        try {
            deviceEd25519Key = mOlmAccount!!.identityKeys()[OlmAccount.JSON_KEY_FINGER_PRINT_KEY]
        } catch (e: Exception) {
            Timber.e(e, "## MXOlmDevice : cannot find " + OlmAccount.JSON_KEY_FINGER_PRINT_KEY + " with error")
        }

        mInboundGroupSessionMessageIndexes = HashMap()
    }

    /**
     * @return The current (unused, unpublished) one-time keys for this account.
     */
    fun getOneTimeKeys(): Map<String, Map<String, String>>? {
        try {
            return mOlmAccount!!.oneTimeKeys()
        } catch (e: Exception) {
            Timber.e(e, "## getOneTimeKeys() : failed")
        }

        return null
    }

    /**
     * @return The maximum number of one-time keys the olm account can store.
     */
    fun getMaxNumberOfOneTimeKeys(): Long {
        return mOlmAccount?.maxOneTimeKeys() ?: -1
    }

    /**
     * Release the instance
     */
    fun release() {
        if (null != mOlmAccount) {
            mOlmAccount!!.releaseAccount()
        }
    }

    /**
     * Signs a message with the ed25519 key for this account.
     *
     * @param message the message to be signed.
     * @return the base64-encoded signature.
     */
    fun signMessage(message: String): String? {
        try {
            return mOlmAccount!!.signMessage(message)
        } catch (e: Exception) {
            Timber.e(e, "## signMessage() : failed")
        }

        return null
    }

    /**
     * Marks all of the one-time keys as published.
     */
    fun markKeysAsPublished() {
        try {
            mOlmAccount!!.markOneTimeKeysAsPublished()
            mStore.storeAccount(mOlmAccount!!)
        } catch (e: Exception) {
            Timber.e(e, "## markKeysAsPublished() : failed")
        }
    }

    /**
     * Generate some new one-time keys
     *
     * @param numKeys number of keys to generate
     */
    fun generateOneTimeKeys(numKeys: Int) {
        try {
            mOlmAccount!!.generateOneTimeKeys(numKeys)
            mStore.storeAccount(mOlmAccount!!)
        } catch (e: Exception) {
            Timber.e(e, "## generateOneTimeKeys() : failed")
        }

    }

    /**
     * Generate a new outbound session.
     * The new session will be stored in the MXStore.
     *
     * @param theirIdentityKey the remote user's Curve25519 identity key
     * @param theirOneTimeKey  the remote user's one-time Curve25519 key
     * @return the session id for the outbound session.
     */
    fun createOutboundSession(theirIdentityKey: String, theirOneTimeKey: String): String? {
        Timber.d("## createOutboundSession() ; theirIdentityKey $theirIdentityKey theirOneTimeKey $theirOneTimeKey")
        var olmSession: OlmSession? = null

        try {
            olmSession = OlmSession()
            olmSession.initOutboundSession(mOlmAccount!!, theirIdentityKey, theirOneTimeKey)

            val mxOlmSession = MXOlmSession(olmSession, 0)

            // Pretend we've received a message at this point, otherwise
            // if we try to send a message to the device, it won't use
            // this session
            mxOlmSession.onMessageReceived()

            mStore.storeSession(mxOlmSession, theirIdentityKey)

            val sessionIdentifier = olmSession.sessionIdentifier()

            Timber.d("## createOutboundSession() ;  olmSession.sessionIdentifier: $sessionIdentifier")
            return sessionIdentifier

        } catch (e: Exception) {
            Timber.e(e, "## createOutboundSession() failed")

            olmSession?.releaseSession()
        }

        return null
    }

    /**
     * Generate a new inbound session, given an incoming message.
     *
     * @param theirDeviceIdentityKey the remote user's Curve25519 identity key.
     * @param messageType            the message_type field from the received message (must be 0).
     * @param ciphertext             base64-encoded body from the received message.
     * @return {{payload: string, session_id: string}} decrypted payload, and session id of new session.
     */
    fun createInboundSession(theirDeviceIdentityKey: String, messageType: Int, ciphertext: String): Map<String, String>? {

        Timber.d("## createInboundSession() : theirIdentityKey: $theirDeviceIdentityKey")

        var olmSession: OlmSession? = null

        try {
            try {
                olmSession = OlmSession()
                olmSession.initInboundSessionFrom(mOlmAccount!!, theirDeviceIdentityKey, ciphertext)
            } catch (e: Exception) {
                Timber.e(e, "## createInboundSession() : the session creation failed")
                return null
            }

            Timber.d("## createInboundSession() : sessionId: " + olmSession.sessionIdentifier())

            try {
                mOlmAccount!!.removeOneTimeKeys(olmSession)
                mStore.storeAccount(mOlmAccount!!)
            } catch (e: Exception) {
                Timber.e(e, "## createInboundSession() : removeOneTimeKeys failed")
            }

            Timber.d("## createInboundSession() : ciphertext: $ciphertext")
            try {
                Timber.d("## createInboundSession() :ciphertext: SHA256:" + mOlmUtility!!.sha256(URLEncoder.encode(ciphertext, "utf-8")))
            } catch (e: Exception) {
                Timber.e(e, "## createInboundSession() :ciphertext: cannot encode ciphertext")
            }

            val olmMessage = OlmMessage()
            olmMessage.mCipherText = ciphertext
            olmMessage.mType = messageType.toLong()

            var payloadString: String? = null

            try {
                payloadString = olmSession.decryptMessage(olmMessage)

                val mxOlmSession = MXOlmSession(olmSession, 0)
                // This counts as a received message: set last received message time to now
                mxOlmSession.onMessageReceived()

                mStore.storeSession(mxOlmSession, theirDeviceIdentityKey)
            } catch (e: Exception) {
                Timber.e(e, "## createInboundSession() : decryptMessage failed")
            }

            val res = HashMap<String, String>()

            if (!TextUtils.isEmpty(payloadString)) {
                res["payload"] = payloadString!!
            }

            val sessionIdentifier = olmSession.sessionIdentifier()

            if (!TextUtils.isEmpty(sessionIdentifier)) {
                res["session_id"] = sessionIdentifier
            }

            return res
        } catch (e: Exception) {
            Timber.e(e, "## createInboundSession() : OlmSession creation failed")

            olmSession?.releaseSession()
        }

        return null
    }

    /**
     * Get a list of known session IDs for the given device.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @return a list of known session ids for the device.
     */
    fun getSessionIds(theirDeviceIdentityKey: String): Set<String>? {
        return mStore.getDeviceSessionIds(theirDeviceIdentityKey)
    }

    /**
     * Get the right olm session id for encrypting messages to the given identity key.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @return the session id, or null if no established session.
     */
    fun getSessionId(theirDeviceIdentityKey: String): String? {
        return mStore.getLastUsedSessionId(theirDeviceIdentityKey)
    }

    /**
     * Encrypt an outgoing message using an existing session.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @param sessionId              the id of the active session
     * @param payloadString          the payload to be encrypted and sent
     * @return the cipher text
     */
    fun encryptMessage(theirDeviceIdentityKey: String, sessionId: String, payloadString: String): Map<String, Any>? {
        var res: MutableMap<String, Any>? = null
        val olmMessage: OlmMessage
        val mxOlmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId)

        if (mxOlmSession != null) {
            try {
                Timber.d("## encryptMessage() : olmSession.sessionIdentifier: $sessionId")
                //Timber.d("## encryptMessage() : payloadString: " + payloadString);

                olmMessage = mxOlmSession.olmSession.encryptMessage(payloadString)
                mStore.storeSession(mxOlmSession, theirDeviceIdentityKey)
                res = HashMap()

                res["body"] = olmMessage.mCipherText
                res["type"] = olmMessage.mType
            } catch (e: Exception) {
                Timber.e(e, "## encryptMessage() : failed " + e.message)
            }

        }

        return res
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
    fun decryptMessage(ciphertext: String, messageType: Int, sessionId: String, theirDeviceIdentityKey: String): String? {
        var payloadString: String? = null

        val mxOlmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId)

        if (null != mxOlmSession) {
            val olmMessage = OlmMessage()
            olmMessage.mCipherText = ciphertext
            olmMessage.mType = messageType.toLong()

            try {
                payloadString = mxOlmSession.olmSession.decryptMessage(olmMessage)
                mxOlmSession.onMessageReceived()
                mStore.storeSession(mxOlmSession, theirDeviceIdentityKey)
            } catch (e: Exception) {
                Timber.e(e, "## decryptMessage() : decryptMessage failed " + e.message)
            }

        }

        return payloadString
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
    fun matchesSession(theirDeviceIdentityKey: String, sessionId: String, messageType: Int, ciphertext: String): Boolean {
        if (messageType != 0) {
            return false
        }

        val mxOlmSession = getSessionForDevice(theirDeviceIdentityKey, sessionId)
        return null != mxOlmSession && mxOlmSession.olmSession.matchesInboundSession(ciphertext)
    }


    // Outbound group session

    /**
     * Generate a new outbound group session.
     *
     * @return the session id for the outbound session.
     */
    fun createOutboundGroupSession(): String? {
        var session: OlmOutboundGroupSession? = null
        try {
            session = OlmOutboundGroupSession()
            mOutboundGroupSessionStore[session.sessionIdentifier()] = session
            return session.sessionIdentifier()
        } catch (e: Exception) {
            Timber.e(e, "createOutboundGroupSession " + e.message)

            session?.releaseSession()
        }

        return null
    }

    /**
     * Get the current session key of  an outbound group session.
     *
     * @param sessionId the id of the outbound group session.
     * @return the base64-encoded secret key.
     */
    fun getSessionKey(sessionId: String): String? {
        if (!TextUtils.isEmpty(sessionId)) {
            try {
                return mOutboundGroupSessionStore[sessionId]!!.sessionKey()
            } catch (e: Exception) {
                Timber.e(e, "## getSessionKey() : failed " + e.message)
            }

        }
        return null
    }

    /**
     * Get the current message index of an outbound group session.
     *
     * @param sessionId the id of the outbound group session.
     * @return the current chain index.
     */
    fun getMessageIndex(sessionId: String): Int {
        return if (!TextUtils.isEmpty(sessionId)) {
            mOutboundGroupSessionStore[sessionId]!!.messageIndex()
        } else 0
    }

    /**
     * Encrypt an outgoing message with an outbound group session.
     *
     * @param sessionId     the id of the outbound group session.
     * @param payloadString the payload to be encrypted and sent.
     * @return ciphertext
     */
    fun encryptGroupMessage(sessionId: String, payloadString: String): String? {
        if (!TextUtils.isEmpty(sessionId) && !TextUtils.isEmpty(payloadString)) {
            try {
                return mOutboundGroupSessionStore[sessionId]!!.encryptMessage(payloadString)
            } catch (e: Exception) {
                Timber.e(e, "## encryptGroupMessage() : failed " + e.message)
            }

        }
        return null
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
    fun addInboundGroupSession(sessionId: String,
                               sessionKey: String,
                               roomId: String,
                               senderKey: String,
                               forwardingCurve25519KeyChain: List<String>,
                               keysClaimed: Map<String, String>,
                               exportFormat: Boolean): Boolean {
        val existingInboundSession = getInboundGroupSession(sessionId, senderKey, roomId)
        val session = MXOlmInboundGroupSession2(sessionKey, exportFormat)

        if (null != existingInboundSession) {
            // If we already have this session, consider updating it
            Timber.e("## addInboundGroupSession() : Update for megolm session $senderKey/$sessionId")

            val existingFirstKnown = existingInboundSession.firstKnownIndex!!
            val newKnownFirstIndex = session.firstKnownIndex!!

            //If our existing session is better we keep it
            if (newKnownFirstIndex != null && existingFirstKnown <= newKnownFirstIndex) {
                if (session.mSession != null) {
                    session.mSession!!.releaseSession()
                }
                return false
            }
        }

        // sanity check
        if (null == session.mSession) {
            Timber.e("## addInboundGroupSession : invalid session")
            return false
        }

        try {
            if (!TextUtils.equals(session.mSession!!.sessionIdentifier(), sessionId)) {
                Timber.e("## addInboundGroupSession : ERROR: Mismatched group session ID from senderKey: $senderKey")
                session.mSession!!.releaseSession()
                return false
            }
        } catch (e: Exception) {
            session.mSession!!.releaseSession()
            Timber.e(e, "## addInboundGroupSession : sessionIdentifier() failed")
            return false
        }

        session.mSenderKey = senderKey
        session.mRoomId = roomId
        session.mKeysClaimed = keysClaimed
        session.mForwardingCurve25519KeyChain = forwardingCurve25519KeyChain

        mStore.storeInboundGroupSessions(listOf(session))

        return true
    }

    /**
     * Import an inbound group sessions to the session store.
     *
     * @param megolmSessionsData the megolm sessions data
     * @return the successfully imported sessions.
     */
    fun importInboundGroupSessions(megolmSessionsData: List<MegolmSessionData>): List<MXOlmInboundGroupSession2> {
        val sessions = ArrayList<MXOlmInboundGroupSession2>(megolmSessionsData.size)

        for (megolmSessionData in megolmSessionsData) {

            val sessionId = megolmSessionData.sessionId
            val senderKey = megolmSessionData.senderKey
            val roomId = megolmSessionData.roomId

            var session: MXOlmInboundGroupSession2? = null

            try {
                session = MXOlmInboundGroupSession2(megolmSessionData)
            } catch (e: Exception) {
                Timber.e(e, "## importInboundGroupSession() : Update for megolm session $senderKey/$sessionId")
            }

            // sanity check
            if (null == session || null == session.mSession) {
                Timber.e("## importInboundGroupSession : invalid session")
                continue
            }

            try {
                if (!TextUtils.equals(session.mSession!!.sessionIdentifier(), sessionId)) {
                    Timber.e("## importInboundGroupSession : ERROR: Mismatched group session ID from senderKey: " + senderKey!!)
                    if (session.mSession != null) session.mSession!!.releaseSession()
                    continue
                }
            } catch (e: Exception) {
                Timber.e(e, "## importInboundGroupSession : sessionIdentifier() failed")
                session.mSession!!.releaseSession()
                continue
            }

            val existingOlmSession = getInboundGroupSession(sessionId, senderKey, roomId)
            if (null != existingOlmSession) {
                // If we already have this session, consider updating it
                Timber.e("## importInboundGroupSession() : Update for megolm session $senderKey/$sessionId")

                // For now we just ignore updates. TODO: implement something here
                if (existingOlmSession.firstKnownIndex!! <= session.firstKnownIndex!!) {
                    //Ignore this, keep existing
                    session.mSession!!.releaseSession()
                    continue
                }
            }

            sessions.add(session)
        }

        mStore.storeInboundGroupSessions(sessions)

        return sessions
    }

    /**
     * Remove an inbound group session
     *
     * @param sessionId  the session identifier.
     * @param sessionKey base64-encoded secret key.
     */
    fun removeInboundGroupSession(sessionId: String?, sessionKey: String?) {
        if (null != sessionId && null != sessionKey) {
            mStore.removeInboundGroupSession(sessionId, sessionKey)
        }
    }

    /**
     * Decrypt a received message with an inbound group session.
     *
     * @param body      the base64-encoded body of the encrypted message.
     * @param roomId    the room in which the message was received.
     * @param timeline  the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return the decrypting result. Nil if the sessionId is unknown.
     */
    @Throws(MXDecryptionException::class)
    fun decryptGroupMessage(body: String,
                            roomId: String,
                            timeline: String?,
                            sessionId: String,
                            senderKey: String): MXDecryptionResult? {
        val result = MXDecryptionResult()
        val session = getInboundGroupSession(sessionId, senderKey, roomId)

        if (null != session) {
            // Check that the room id matches the original one for the session. This stops
            // the HS pretending a message was targeting a different room.
            if (TextUtils.equals(roomId, session.mRoomId)) {
                var errorMessage = ""
                var decryptResult: OlmInboundGroupSession.DecryptMessageResult? = null
                try {
                    decryptResult = session.mSession!!.decryptMessage(body)
                } catch (e: Exception) {
                    Timber.e(e, "## decryptGroupMessage () : decryptMessage failed")
                    errorMessage = e.message ?: ""
                }

                if (null != decryptResult) {
                    if (null != timeline) {
                        if (!mInboundGroupSessionMessageIndexes.containsKey(timeline)) {
                            mInboundGroupSessionMessageIndexes[timeline] = HashMap()
                        }

                        val messageIndexKey = senderKey + "|" + sessionId + "|" + decryptResult.mIndex

                        if (null != mInboundGroupSessionMessageIndexes[timeline]!![messageIndexKey]) {
                            val reason = String.format(MXCryptoError.DUPLICATE_MESSAGE_INDEX_REASON, decryptResult.mIndex)
                            Timber.e("## decryptGroupMessage() : $reason")
                            throw MXDecryptionException(MXCryptoError(MXCryptoError.DUPLICATED_MESSAGE_INDEX_ERROR_CODE,
                                    MXCryptoError.UNABLE_TO_DECRYPT, reason))
                        }

                        mInboundGroupSessionMessageIndexes[timeline]!!.put(messageIndexKey, true)
                    }

                    mStore.storeInboundGroupSessions(listOf(session))
                    try {
                        val moshi = MoshiProvider.providesMoshi()
                        val adapter = moshi.adapter(Map::class.java)
                        result.mPayload = adapter.fromJson(convertFromUTF8(decryptResult.mDecryptedMessage)) as Event?
                    } catch (e: Exception) {
                        Timber.e(e, "## decryptGroupMessage() : RLEncoder.encode failed " + e.message)
                        return null
                    }

                    if (null == result.mPayload) {
                        Timber.e("## decryptGroupMessage() : fails to parse the payload")
                        return null
                    }

                    result.mKeysClaimed = session.mKeysClaimed
                    result.mSenderKey = senderKey
                    result.mForwardingCurve25519KeyChain = session.mForwardingCurve25519KeyChain
                } else {
                    Timber.e("## decryptGroupMessage() : failed to decode the message")
                    throw MXDecryptionException(MXCryptoError(MXCryptoError.OLM_ERROR_CODE, errorMessage, null))
                }
            } else {
                val reason = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, session.mRoomId)
                Timber.e("## decryptGroupMessage() : $reason")
                throw MXDecryptionException(MXCryptoError(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_DECRYPT, reason))
            }
        } else {
            Timber.e("## decryptGroupMessage() : Cannot retrieve inbound group session $sessionId")
            throw MXDecryptionException(mInboundGroupSessionWithIdError)
        }

        return result
    }

    /**
     * Reset replay attack data for the given timeline.
     *
     * @param timeline the id of the timeline.
     */
    fun resetReplayAttackCheckInTimeline(timeline: String?) {
        if (null != timeline) {
            mInboundGroupSessionMessageIndexes.remove(timeline)
        }
    }

    //  Utilities

    /**
     * Verify an ed25519 signature on a JSON object.
     *
     * @param key            the ed25519 key.
     * @param jsonDictionary the JSON object which was signed.
     * @param signature      the base64-encoded signature to be checked.
     * @throws Exception the exception
     */
    @Throws(Exception::class)
    fun verifySignature(key: String, jsonDictionary: Map<String, Any>, signature: String) {
        // Check signature on the canonical version of the JSON
        mOlmUtility!!.verifyEd25519Signature(signature, key, MoshiProvider.getCanonicalJson<Map<*, *>>(Map::class.java, jsonDictionary))
    }

    /**
     * Calculate the SHA-256 hash of the input and encodes it as base64.
     *
     * @param message the message to hash.
     * @return the base64-encoded hash value.
     */
    fun sha256(message: String): String {
        return mOlmUtility!!.sha256(convertToUTF8(message))
    }

    /**
     * Search an OlmSession
     *
     * @param theirDeviceIdentityKey the device key
     * @param sessionId              the session Id
     * @return the olm session
     */
    private fun getSessionForDevice(theirDeviceIdentityKey: String, sessionId: String): MXOlmSession? {
        // sanity check
        return if (!TextUtils.isEmpty(theirDeviceIdentityKey) && !TextUtils.isEmpty(sessionId)) {
            mStore.getDeviceSession(sessionId, theirDeviceIdentityKey)
        } else null

    }

    /**
     * Extract an InboundGroupSession from the session store and do some check.
     * mInboundGroupSessionWithIdError describes the failure reason.
     *
     * @param roomId    the room where the session is used.
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return the inbound group session.
     */
    fun getInboundGroupSession(sessionId: String?, senderKey: String?, roomId: String?): MXOlmInboundGroupSession2? {
        mInboundGroupSessionWithIdError = null

        val session = mStore.getInboundGroupSession(sessionId!!, senderKey!!)

        if (null != session) {
            // Check that the room id matches the original one for the session. This stops
            // the HS pretending a message was targeting a different room.
            if (!TextUtils.equals(roomId, session.mRoomId)) {
                val errorDescription = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, session.mRoomId)
                Timber.e("## getInboundGroupSession() : $errorDescription")
                mInboundGroupSessionWithIdError = MXCryptoError(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_DECRYPT, errorDescription)
            }
        } else {
            Timber.e("## getInboundGroupSession() : Cannot retrieve inbound group session $sessionId")
            mInboundGroupSessionWithIdError = MXCryptoError(MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE,
                    MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_REASON, null)
        }
        return session
    }

    /**
     * Determine if we have the keys for a given megolm session.
     *
     * @param roomId    room in which the message was received
     * @param senderKey base64-encoded curve25519 key of the sender
     * @param sessionId session identifier
     * @return true if the unbound session keys are known.
     */
    fun hasInboundSessionKeys(roomId: String, senderKey: String, sessionId: String): Boolean {
        return null != getInboundGroupSession(sessionId, senderKey, roomId)
    }
}
