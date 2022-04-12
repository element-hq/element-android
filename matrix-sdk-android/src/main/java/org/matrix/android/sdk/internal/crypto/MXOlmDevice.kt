/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXOutboundSessionInfo
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.SharedWithHelper
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.convertFromUTF8
import org.matrix.android.sdk.internal.util.convertToUTF8
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import org.matrix.olm.OlmMessage
import org.matrix.olm.OlmOutboundGroupSession
import org.matrix.olm.OlmSession
import org.matrix.olm.OlmUtility
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject

private val loggerTag = LoggerTag("MXOlmDevice", LoggerTag.CRYPTO)

// The libolm wrapper.
@SessionScope
internal class MXOlmDevice @Inject constructor(
        /**
         * The store where crypto data is saved.
         */
        private val store: IMXCryptoStore,
        private val olmSessionStore: OlmSessionStore,
        private val inboundGroupSessionStore: InboundGroupSessionStore
) {

    val mutex = Mutex()

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

    // The OLM lib utility instance.
    private var olmUtility: OlmUtility? = null

    private data class GroupSessionCacheItem(
            val groupId: String,
            val groupSession: OlmOutboundGroupSession
    )

    // The outbound group session.
    // Caches active outbound session to avoid to sync with DB before read
    // The key is the session id, the value the <roomID,outbound group session>.
    private val outboundGroupSessionCache: MutableMap<String, GroupSessionCacheItem> = HashMap()

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
    private val inboundGroupSessionMessageIndexes: MutableMap<String, MutableSet<String>> = HashMap()

    init {
        // Retrieve the account from the store
        try {
            store.getOrCreateOlmAccount()
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "MXOlmDevice : cannot initialize olmAccount")
        }

        try {
            olmUtility = OlmUtility()
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## MXOlmDevice : OlmUtility failed with error")
            olmUtility = null
        }

        try {
            deviceCurve25519Key = store.doWithOlmAccount { it.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY] }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## MXOlmDevice : cannot find ${OlmAccount.JSON_KEY_IDENTITY_KEY} with error")
        }

        try {
            deviceEd25519Key = store.doWithOlmAccount { it.identityKeys()[OlmAccount.JSON_KEY_FINGER_PRINT_KEY] }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## MXOlmDevice : cannot find ${OlmAccount.JSON_KEY_FINGER_PRINT_KEY} with error")
        }
    }

    /**
     * @return The current (unused, unpublished) one-time keys for this account.
     */
    fun getOneTimeKeys(): Map<String, Map<String, String>>? {
        try {
            return store.doWithOlmAccount { it.oneTimeKeys() }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## getOneTimeKeys() : failed")
        }

        return null
    }

    /**
     * @return The maximum number of one-time keys the olm account can store.
     */
    fun getMaxNumberOfOneTimeKeys(): Long {
        return store.doWithOlmAccount { it.maxOneTimeKeys() }
    }

    /**
     * Returns an unpublished fallback key
     * A call to markKeysAsPublished will mark it as published and this
     * call will return null (until a call to generateFallbackKey is made)
     */
    fun getFallbackKey(): MutableMap<String, MutableMap<String, String>>? {
        try {
            return store.doWithOlmAccount { it.fallbackKey() }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e("## getFallbackKey() : failed")
        }
        return null
    }

    /**
     * Generates a new fallback key if there is not already
     * an unpublished one.
     * @return true if a new key was generated
     */
    fun generateFallbackKeyIfNeeded(): Boolean {
        try {
            if (!hasUnpublishedFallbackKey()) {
                store.doWithOlmAccount {
                    it.generateFallbackKey()
                    store.saveOlmAccount()
                }
                return true
            }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e("## generateFallbackKey() : failed")
        }
        return false
    }

    internal fun hasUnpublishedFallbackKey(): Boolean {
        return getFallbackKey()?.get(OlmAccount.JSON_KEY_ONE_TIME_KEY).orEmpty().isNotEmpty()
    }

    fun forgetFallbackKey() {
        try {
            store.doWithOlmAccount {
                it.forgetFallbackKey()
                store.saveOlmAccount()
            }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e("## forgetFallbackKey() : failed")
        }
    }

    /**
     * Release the instance
     */
    fun release() {
        olmUtility?.releaseUtility()
        outboundGroupSessionCache.values.forEach {
            it.groupSession.releaseSession()
        }
        outboundGroupSessionCache.clear()
        inboundGroupSessionStore.clear()
        olmSessionStore.clear()
    }

    /**
     * Signs a message with the ed25519 key for this account.
     *
     * @param message the message to be signed.
     * @return the base64-encoded signature.
     */
    fun signMessage(message: String): String? {
        try {
            return store.doWithOlmAccount { it.signMessage(message) }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## signMessage() : failed")
        }

        return null
    }

    /**
     * Marks all of the one-time keys as published.
     */
    fun markKeysAsPublished() {
        try {
            store.doWithOlmAccount {
                it.markOneTimeKeysAsPublished()
                store.saveOlmAccount()
            }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## markKeysAsPublished() : failed")
        }
    }

    /**
     * Generate some new one-time keys
     *
     * @param numKeys number of keys to generate
     */
    fun generateOneTimeKeys(numKeys: Int) {
        try {
            store.doWithOlmAccount {
                it.generateOneTimeKeys(numKeys)
                store.saveOlmAccount()
            }
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## generateOneTimeKeys() : failed")
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
        Timber.tag(loggerTag.value).d("## createOutboundSession() ; theirIdentityKey $theirIdentityKey theirOneTimeKey $theirOneTimeKey")
        var olmSession: OlmSession? = null

        try {
            olmSession = OlmSession()
            store.doWithOlmAccount { olmAccount ->
                olmSession.initOutboundSession(olmAccount, theirIdentityKey, theirOneTimeKey)
            }

            val olmSessionWrapper = OlmSessionWrapper(olmSession, 0)

            // Pretend we've received a message at this point, otherwise
            // if we try to send a message to the device, it won't use
            // this session
            olmSessionWrapper.onMessageReceived()

            olmSessionStore.storeSession(olmSessionWrapper, theirIdentityKey)

            val sessionIdentifier = olmSession.sessionIdentifier()

            Timber.tag(loggerTag.value).v("## createOutboundSession() ;  olmSession.sessionIdentifier: $sessionIdentifier")
            return sessionIdentifier
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## createOutboundSession() failed")

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
        Timber.tag(loggerTag.value).d("## createInboundSession() : theirIdentityKey: $theirDeviceIdentityKey")

        var olmSession: OlmSession? = null

        try {
            try {
                olmSession = OlmSession()
                store.doWithOlmAccount { olmAccount ->
                    olmSession.initInboundSessionFrom(olmAccount, theirDeviceIdentityKey, ciphertext)
                }
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## createInboundSession() : the session creation failed")
                return null
            }

            Timber.tag(loggerTag.value).v("## createInboundSession() : sessionId: ${olmSession.sessionIdentifier()}")

            try {
                store.doWithOlmAccount { olmAccount ->
                    olmAccount.removeOneTimeKeys(olmSession)
                    store.saveOlmAccount()
                }
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## createInboundSession() : removeOneTimeKeys failed")
            }

            Timber.tag(loggerTag.value).v("## createInboundSession() : ciphertext: $ciphertext")
            try {
                val sha256 = olmUtility!!.sha256(URLEncoder.encode(ciphertext, "utf-8"))
                Timber.tag(loggerTag.value).v("## createInboundSession() :ciphertext: SHA256: $sha256")
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## createInboundSession() :ciphertext: cannot encode ciphertext")
            }

            val olmMessage = OlmMessage()
            olmMessage.mCipherText = ciphertext
            olmMessage.mType = messageType.toLong()

            var payloadString: String? = null

            try {
                payloadString = olmSession.decryptMessage(olmMessage)

                val olmSessionWrapper = OlmSessionWrapper(olmSession, 0)
                // This counts as a received message: set last received message time to now
                olmSessionWrapper.onMessageReceived()

                olmSessionStore.storeSession(olmSessionWrapper, theirDeviceIdentityKey)
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## createInboundSession() : decryptMessage failed")
            }

            val res = HashMap<String, String>()

            if (!payloadString.isNullOrEmpty()) {
                res["payload"] = payloadString
            }

            val sessionIdentifier = olmSession.sessionIdentifier()

            if (!sessionIdentifier.isNullOrEmpty()) {
                res["session_id"] = sessionIdentifier
            }

            return res
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "## createInboundSession() : OlmSession creation failed")

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
    fun getSessionIds(theirDeviceIdentityKey: String): List<String> {
        return olmSessionStore.getDeviceSessionIds(theirDeviceIdentityKey)
    }

    /**
     * Get the right olm session id for encrypting messages to the given identity key.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @return the session id, or null if no established session.
     */
    fun getSessionId(theirDeviceIdentityKey: String): String? {
        return olmSessionStore.getLastUsedSessionId(theirDeviceIdentityKey)
    }

    /**
     * Encrypt an outgoing message using an existing session.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key for the remote device.
     * @param sessionId              the id of the active session
     * @param payloadString          the payload to be encrypted and sent
     * @return the cipher text
     */
    suspend fun encryptMessage(theirDeviceIdentityKey: String, sessionId: String, payloadString: String): Map<String, Any>? {
        val olmSessionWrapper = getSessionForDevice(theirDeviceIdentityKey, sessionId)

        if (olmSessionWrapper != null) {
            try {
                Timber.tag(loggerTag.value).v("## encryptMessage() : olmSession.sessionIdentifier: $sessionId")

                val olmMessage = olmSessionWrapper.mutex.withLock {
                    olmSessionWrapper.olmSession.encryptMessage(payloadString)
                }
                return mapOf(
                        "body" to olmMessage.mCipherText,
                        "type" to olmMessage.mType,
                ).also {
                    olmSessionStore.storeSession(olmSessionWrapper, theirDeviceIdentityKey)
                }
            } catch (e: Throwable) {
                Timber.tag(loggerTag.value).e(e, "## encryptMessage() : failed to encrypt olm with device|session:$theirDeviceIdentityKey|$sessionId")
                return null
            }
        } else {
            Timber.tag(loggerTag.value).e("## encryptMessage() : Failed to encrypt unknown session $sessionId")
            return null
        }
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
    @kotlin.jvm.Throws
    suspend fun decryptMessage(ciphertext: String, messageType: Int, sessionId: String, theirDeviceIdentityKey: String): String? {
        var payloadString: String? = null

        val olmSessionWrapper = getSessionForDevice(theirDeviceIdentityKey, sessionId)

        if (null != olmSessionWrapper) {
            val olmMessage = OlmMessage()
            olmMessage.mCipherText = ciphertext
            olmMessage.mType = messageType.toLong()

            payloadString =
                    olmSessionWrapper.mutex.withLock {
                        olmSessionWrapper.olmSession.decryptMessage(olmMessage).also {
                            olmSessionWrapper.onMessageReceived()
                        }
                    }
            olmSessionStore.storeSession(olmSessionWrapper, theirDeviceIdentityKey)
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

        val olmSessionWrapper = getSessionForDevice(theirDeviceIdentityKey, sessionId)
        return null != olmSessionWrapper && olmSessionWrapper.olmSession.matchesInboundSession(ciphertext)
    }

    // Outbound group session

    /**
     * Generate a new outbound group session.
     *
     * @return the session id for the outbound session.
     */
    fun createOutboundGroupSessionForRoom(roomId: String): String? {
        var session: OlmOutboundGroupSession? = null
        try {
            session = OlmOutboundGroupSession()
            outboundGroupSessionCache[session.sessionIdentifier()] = GroupSessionCacheItem(roomId, session)
            store.storeCurrentOutboundGroupSessionForRoom(roomId, session)
            return session.sessionIdentifier()
        } catch (e: Exception) {
            Timber.tag(loggerTag.value).e(e, "createOutboundGroupSession")

            session?.releaseSession()
        }

        return null
    }

    fun storeOutboundGroupSessionForRoom(roomId: String, sessionId: String) {
        outboundGroupSessionCache[sessionId]?.let {
            store.storeCurrentOutboundGroupSessionForRoom(roomId, it.groupSession)
        }
    }

    fun restoreOutboundGroupSessionForRoom(roomId: String): MXOutboundSessionInfo? {
        val restoredOutboundGroupSession = store.getCurrentOutboundGroupSessionForRoom(roomId)
        if (restoredOutboundGroupSession != null) {
            val sessionId = restoredOutboundGroupSession.outboundGroupSession.sessionIdentifier()
            // cache it
            outboundGroupSessionCache[sessionId] = GroupSessionCacheItem(roomId, restoredOutboundGroupSession.outboundGroupSession)

            return MXOutboundSessionInfo(
                    sessionId = sessionId,
                    sharedWithHelper = SharedWithHelper(roomId, sessionId, store),
                    restoredOutboundGroupSession.creationTime
            )
        }
        return null
    }

    fun discardOutboundGroupSessionForRoom(roomId: String) {
        val toDiscard = outboundGroupSessionCache.filter {
            it.value.groupId == roomId
        }
        toDiscard.forEach { (sessionId, cacheItem) ->
            cacheItem.groupSession.releaseSession()
            outboundGroupSessionCache.remove(sessionId)
        }
        store.storeCurrentOutboundGroupSessionForRoom(roomId, null)
    }

    /**
     * Get the current session key of  an outbound group session.
     *
     * @param sessionId the id of the outbound group session.
     * @return the base64-encoded secret key.
     */
    fun getSessionKey(sessionId: String): String? {
        if (sessionId.isNotEmpty()) {
            try {
                return outboundGroupSessionCache[sessionId]!!.groupSession.sessionKey()
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## getSessionKey() : failed")
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
        return if (sessionId.isNotEmpty()) {
            outboundGroupSessionCache[sessionId]!!.groupSession.messageIndex()
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
        if (sessionId.isNotEmpty() && payloadString.isNotEmpty()) {
            try {
                return outboundGroupSessionCache[sessionId]!!.groupSession.encryptMessage(payloadString)
            } catch (e: Throwable) {
                Timber.tag(loggerTag.value).e(e, "## encryptGroupMessage() : failed")
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
        val candidateSession = OlmInboundGroupSessionWrapper2(sessionKey, exportFormat)
        val existingSessionHolder = tryOrNull { getInboundGroupSession(sessionId, senderKey, roomId) }
        val existingSession = existingSessionHolder?.wrapper
        // If we have an existing one we should check if the new one is not better
        if (existingSession != null) {
            Timber.tag(loggerTag.value).d("## addInboundGroupSession() check if known session is better than candidate session")
            try {
                val existingFirstKnown = existingSession.firstKnownIndex ?: return false.also {
                    // This is quite unexpected, could throw if native was released?
                    Timber.tag(loggerTag.value).e("## addInboundGroupSession() null firstKnownIndex on existing session")
                    candidateSession.olmInboundGroupSession?.releaseSession()
                    // Probably should discard it?
                }
                val newKnownFirstIndex = candidateSession.firstKnownIndex
                // If our existing session is better we keep it
                if (newKnownFirstIndex != null && existingFirstKnown <= newKnownFirstIndex) {
                    Timber.tag(loggerTag.value).d("## addInboundGroupSession() : ignore session our is better $senderKey/$sessionId")
                    candidateSession.olmInboundGroupSession?.releaseSession()
                    return false
                }
            } catch (failure: Throwable) {
                Timber.tag(loggerTag.value).e("## addInboundGroupSession() Failed to add inbound: ${failure.localizedMessage}")
                candidateSession.olmInboundGroupSession?.releaseSession()
                return false
            }
        }

        Timber.tag(loggerTag.value).d("## addInboundGroupSession() : Candidate session should be added $senderKey/$sessionId")

        // sanity check on the new session
        val candidateOlmInboundSession = candidateSession.olmInboundGroupSession
        if (null == candidateOlmInboundSession) {
            Timber.tag(loggerTag.value).e("## addInboundGroupSession : invalid session <null>")
            return false
        }

        try {
            if (candidateOlmInboundSession.sessionIdentifier() != sessionId) {
                Timber.tag(loggerTag.value).e("## addInboundGroupSession : ERROR: Mismatched group session ID from senderKey: $senderKey")
                candidateOlmInboundSession.releaseSession()
                return false
            }
        } catch (e: Throwable) {
            candidateOlmInboundSession.releaseSession()
            Timber.tag(loggerTag.value).e(e, "## addInboundGroupSession : sessionIdentifier() failed")
            return false
        }

        candidateSession.senderKey = senderKey
        candidateSession.roomId = roomId
        candidateSession.keysClaimed = keysClaimed
        candidateSession.forwardingCurve25519KeyChain = forwardingCurve25519KeyChain

        if (existingSession != null) {
            inboundGroupSessionStore.replaceGroupSession(existingSessionHolder, InboundGroupSessionHolder(candidateSession), sessionId, senderKey)
        } else {
            inboundGroupSessionStore.storeInBoundGroupSession(InboundGroupSessionHolder(candidateSession), sessionId, senderKey)
        }

        return true
    }

    /**
     * Import an inbound group sessions to the session store.
     *
     * @param megolmSessionsData the megolm sessions data
     * @return the successfully imported sessions.
     */
    fun importInboundGroupSessions(megolmSessionsData: List<MegolmSessionData>): List<OlmInboundGroupSessionWrapper2> {
        val sessions = ArrayList<OlmInboundGroupSessionWrapper2>(megolmSessionsData.size)

        for (megolmSessionData in megolmSessionsData) {
            val sessionId = megolmSessionData.sessionId ?: continue
            val senderKey = megolmSessionData.senderKey ?: continue
            val roomId = megolmSessionData.roomId

            var candidateSessionToImport: OlmInboundGroupSessionWrapper2? = null

            try {
                candidateSessionToImport = OlmInboundGroupSessionWrapper2(megolmSessionData)
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## importInboundGroupSession() : Update for megolm session $senderKey/$sessionId")
            }

            // sanity check
            if (candidateSessionToImport?.olmInboundGroupSession == null) {
                Timber.tag(loggerTag.value).e("## importInboundGroupSession : invalid session")
                continue
            }

            val candidateOlmInboundGroupSession = candidateSessionToImport.olmInboundGroupSession
            try {
                if (candidateOlmInboundGroupSession?.sessionIdentifier() != sessionId) {
                    Timber.tag(loggerTag.value).e("## importInboundGroupSession : ERROR: Mismatched group session ID from senderKey: $senderKey")
                    candidateOlmInboundGroupSession?.releaseSession()
                    continue
                }
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e(e, "## importInboundGroupSession : sessionIdentifier() failed")
                candidateOlmInboundGroupSession?.releaseSession()
                continue
            }

            val existingSessionHolder = tryOrNull { getInboundGroupSession(sessionId, senderKey, roomId) }
            val existingSession = existingSessionHolder?.wrapper

            if (existingSession == null) {
                // Session does not already exist, add it
                Timber.tag(loggerTag.value).d("## importInboundGroupSession() : importing new megolm session $senderKey/$sessionId")
                sessions.add(candidateSessionToImport)
            } else {
                Timber.tag(loggerTag.value).e("## importInboundGroupSession() : Update for megolm session $senderKey/$sessionId")
                val existingFirstKnown = tryOrNull { existingSession.firstKnownIndex }
                val candidateFirstKnownIndex = tryOrNull { candidateSessionToImport.firstKnownIndex }

                if (existingFirstKnown == null || candidateFirstKnownIndex == null) {
                    // should not happen?
                    candidateSessionToImport.olmInboundGroupSession?.releaseSession()
                    Timber.tag(loggerTag.value)
                            .w("## importInboundGroupSession() : Can't check session null index $existingFirstKnown/$candidateFirstKnownIndex")
                } else {
                    if (existingFirstKnown <= candidateSessionToImport.firstKnownIndex!!) {
                        // Ignore this, keep existing
                        candidateOlmInboundGroupSession.releaseSession()
                    } else {
                        // update cache with better session
                        inboundGroupSessionStore.replaceGroupSession(
                                existingSessionHolder,
                                InboundGroupSessionHolder(candidateSessionToImport),
                                sessionId,
                                senderKey
                        )
                        sessions.add(candidateSessionToImport)
                    }
                }
            }
        }

        store.storeInboundGroupSessions(sessions)

        return sessions
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
    @Throws(MXCryptoError::class)
    suspend fun decryptGroupMessage(body: String,
                                    roomId: String,
                                    timeline: String?,
                                    sessionId: String,
                                    senderKey: String): OlmDecryptionResult {
        val sessionHolder = getInboundGroupSession(sessionId, senderKey, roomId)
        val wrapper = sessionHolder.wrapper
        val inboundGroupSession = wrapper.olmInboundGroupSession
                ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, "Session is null")
        // Check that the room id matches the original one for the session. This stops
        // the HS pretending a message was targeting a different room.
        if (roomId == wrapper.roomId) {
            val decryptResult = try {
                sessionHolder.mutex.withLock {
                    inboundGroupSession.decryptMessage(body)
                }
            } catch (e: OlmException) {
                Timber.tag(loggerTag.value).e(e, "## decryptGroupMessage () : decryptMessage failed")
                throw MXCryptoError.OlmError(e)
            }

            if (timeline?.isNotBlank() == true) {
                val timelineSet = inboundGroupSessionMessageIndexes.getOrPut(timeline) { mutableSetOf() }

                val messageIndexKey = senderKey + "|" + sessionId + "|" + decryptResult.mIndex

                if (timelineSet.contains(messageIndexKey)) {
                    val reason = String.format(MXCryptoError.DUPLICATE_MESSAGE_INDEX_REASON, decryptResult.mIndex)
                    Timber.tag(loggerTag.value).e("## decryptGroupMessage() : $reason")
                    throw MXCryptoError.Base(MXCryptoError.ErrorType.DUPLICATED_MESSAGE_INDEX, reason)
                }

                timelineSet.add(messageIndexKey)
            }

            inboundGroupSessionStore.storeInBoundGroupSession(sessionHolder, sessionId, senderKey)
            val payload = try {
                val adapter = MoshiProvider.providesMoshi().adapter<JsonDict>(JSON_DICT_PARAMETERIZED_TYPE)
                val payloadString = convertFromUTF8(decryptResult.mDecryptedMessage)
                adapter.fromJson(payloadString)
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).e("## decryptGroupMessage() : fails to parse the payload")
                throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_DECRYPTED_FORMAT, MXCryptoError.BAD_DECRYPTED_FORMAT_TEXT_REASON)
            }

            return OlmDecryptionResult(
                    payload,
                    wrapper.keysClaimed,
                    senderKey,
                    wrapper.forwardingCurve25519KeyChain
            )
        } else {
            val reason = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, wrapper.roomId)
            Timber.tag(loggerTag.value).e("## decryptGroupMessage() : $reason")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.INBOUND_SESSION_MISMATCH_ROOM_ID, reason)
        }
    }

    /**
     * Reset replay attack data for the given timeline.
     *
     * @param timeline the id of the timeline.
     */
    fun resetReplayAttackCheckInTimeline(timeline: String?) {
        if (null != timeline) {
            inboundGroupSessionMessageIndexes.remove(timeline)
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
        olmUtility!!.verifyEd25519Signature(signature, key, JsonCanonicalizer.getCanonicalJson(Map::class.java, jsonDictionary))
    }

    /**
     * Calculate the SHA-256 hash of the input and encodes it as base64.
     *
     * @param message the message to hash.
     * @return the base64-encoded hash value.
     */
    fun sha256(message: String): String {
        return olmUtility!!.sha256(convertToUTF8(message))
    }

    /**
     * Search an OlmSession
     *
     * @param theirDeviceIdentityKey the device key
     * @param sessionId              the session Id
     * @return the olm session
     */
    private fun getSessionForDevice(theirDeviceIdentityKey: String, sessionId: String): OlmSessionWrapper? {
        // sanity check
        return if (theirDeviceIdentityKey.isEmpty() || sessionId.isEmpty()) null else {
            olmSessionStore.getDeviceSession(sessionId, theirDeviceIdentityKey)
        }
    }

    /**
     * Extract an InboundGroupSession from the session store and do some check.
     * inboundGroupSessionWithIdError describes the failure reason.
     *
     * @param roomId    the room where the session is used.
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return the inbound group session.
     */
    fun getInboundGroupSession(sessionId: String?, senderKey: String?, roomId: String?): InboundGroupSessionHolder {
        if (sessionId.isNullOrBlank() || senderKey.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_SENDER_KEY, MXCryptoError.ERROR_MISSING_PROPERTY_REASON)
        }

        val holder = inboundGroupSessionStore.getInboundGroupSession(sessionId, senderKey)
        val session = holder?.wrapper

        if (session != null) {
            // Check that the room id matches the original one for the session. This stops
            // the HS pretending a message was targeting a different room.
            if (roomId != session.roomId) {
                val errorDescription = String.format(MXCryptoError.INBOUND_SESSION_MISMATCH_ROOM_ID_REASON, roomId, session.roomId)
                Timber.tag(loggerTag.value).e("## getInboundGroupSession() : $errorDescription")
                throw MXCryptoError.Base(MXCryptoError.ErrorType.INBOUND_SESSION_MISMATCH_ROOM_ID, errorDescription)
            } else {
                return holder
            }
        } else {
            Timber.tag(loggerTag.value).w("## getInboundGroupSession() : UISI $sessionId")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID, MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_REASON)
        }
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
        return runCatching { getInboundGroupSession(sessionId, senderKey, roomId) }.isSuccess
    }

    @VisibleForTesting
    fun clearOlmSessionCache() {
        olmSessionStore.clear()
    }
}
