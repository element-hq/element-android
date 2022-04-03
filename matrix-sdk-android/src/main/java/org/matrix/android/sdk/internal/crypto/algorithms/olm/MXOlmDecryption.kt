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

package org.matrix.android.sdk.internal.crypto.algorithms.olm

import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.algorithms.IMXDecrypting
import org.matrix.android.sdk.internal.crypto.model.event.OlmEventContent
import org.matrix.android.sdk.internal.crypto.model.event.OlmPayloadContent
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.convertFromUTF8
import timber.log.Timber

private val loggerTag = LoggerTag("MXOlmDecryption", LoggerTag.CRYPTO)
internal class MXOlmDecryption(
        // The olm device interface
        private val olmDevice: MXOlmDevice,
        // the matrix userId
        private val userId: String) :
    IMXDecrypting {

    @Throws(MXCryptoError::class)
    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        val olmEventContent = event.content.toModel<OlmEventContent>() ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : bad event format")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_EVENT_FORMAT,
                    MXCryptoError.BAD_EVENT_FORMAT_TEXT_REASON)
        }

        val cipherText = olmEventContent.ciphertext ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : missing cipher text")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_CIPHER_TEXT,
                    MXCryptoError.MISSING_CIPHER_TEXT_REASON)
        }

        val senderKey = olmEventContent.senderKey ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : missing sender key")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_SENDER_KEY,
                    MXCryptoError.MISSING_SENDER_KEY_TEXT_REASON)
        }

        val messageAny = cipherText[olmDevice.deviceCurve25519Key] ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : our device ${olmDevice.deviceCurve25519Key} is not included in recipients")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.NOT_INCLUDE_IN_RECIPIENTS, MXCryptoError.NOT_INCLUDED_IN_RECIPIENT_REASON)
        }

        // The message for myUser
        @Suppress("UNCHECKED_CAST")
        val message = messageAny as JsonDict

        val decryptedPayload = decryptMessage(message, senderKey)

        if (decryptedPayload == null) {
            Timber.tag(loggerTag.value).e("## decryptEvent() Failed to decrypt Olm event (id= ${event.eventId} from $senderKey")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_ENCRYPTED_MESSAGE, MXCryptoError.BAD_ENCRYPTED_MESSAGE_REASON)
        }
        val payloadString = convertFromUTF8(decryptedPayload)

        val adapter = MoshiProvider.providesMoshi().adapter<JsonDict>(JSON_DICT_PARAMETERIZED_TYPE)
        val payload = adapter.fromJson(payloadString)

        if (payload == null) {
            Timber.tag(loggerTag.value).e("## decryptEvent failed : null payload")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_CIPHER_TEXT_REASON)
        }

        val olmPayloadContent = OlmPayloadContent.fromJsonString(payloadString) ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : bad olmPayloadContent format")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_DECRYPTED_FORMAT, MXCryptoError.BAD_DECRYPTED_FORMAT_TEXT_REASON)
        }

        if (olmPayloadContent.recipient.isNullOrBlank()) {
            val reason = String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient")
            Timber.tag(loggerTag.value).e("## decryptEvent() : $reason")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY, reason)
        }

        if (olmPayloadContent.recipient != userId) {
            Timber.tag(loggerTag.value).e("## decryptEvent() : Event ${event.eventId}:" +
                    " Intended recipient ${olmPayloadContent.recipient} does not match our id $userId")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_RECIPIENT,
                    String.format(MXCryptoError.BAD_RECIPIENT_REASON, olmPayloadContent.recipient))
        }

        val recipientKeys = olmPayloadContent.recipientKeys ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent() : Olm event (id=${event.eventId}) contains no 'recipient_keys'" +
                    " property; cannot prevent unknown-key attack")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY,
                    String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient_keys"))
        }

        val ed25519 = recipientKeys["ed25519"]

        if (ed25519 != olmDevice.deviceEd25519Key) {
            Timber.tag(loggerTag.value).e("## decryptEvent() : Event ${event.eventId}: Intended recipient ed25519 key $ed25519 did not match ours")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_RECIPIENT_KEY,
                    MXCryptoError.BAD_RECIPIENT_KEY_REASON)
        }

        if (olmPayloadContent.sender.isNullOrBlank()) {
            Timber.tag(loggerTag.value)
                    .e("## decryptEvent() : Olm event (id=${event.eventId}) contains no 'sender' property; cannot prevent unknown-key attack")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY,
                    String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "sender"))
        }

        if (olmPayloadContent.sender != event.senderId) {
            Timber.tag(loggerTag.value)
                    .e("Event ${event.eventId}:  sender ${olmPayloadContent.sender} does not match reported sender ${event.senderId}")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.FORWARDED_MESSAGE,
                    String.format(MXCryptoError.FORWARDED_MESSAGE_REASON, olmPayloadContent.sender))
        }

        if (olmPayloadContent.roomId != event.roomId) {
            Timber.tag(loggerTag.value)
                    .e("## decryptEvent() : Event ${event.eventId}:  room ${olmPayloadContent.roomId} does not match reported room ${event.roomId}")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_ROOM,
                    String.format(MXCryptoError.BAD_ROOM_REASON, olmPayloadContent.roomId))
        }

        val keys = olmPayloadContent.keys ?: run {
            Timber.tag(loggerTag.value).e("## decryptEvent failed : null keys")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT,
                    MXCryptoError.MISSING_CIPHER_TEXT_REASON)
        }

        return MXEventDecryptionResult(
                clearEvent = payload,
                senderCurve25519Key = senderKey,
                claimedEd25519Key = keys["ed25519"]
        )
    }

    /**
     * Attempt to decrypt an Olm message.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key of the sender.
     * @param message                message object, with 'type' and 'body' fields.
     * @return payload, if decrypted successfully.
     */
    private suspend fun decryptMessage(message: JsonDict, theirDeviceIdentityKey: String): String? {
        val sessionIds = olmDevice.getSessionIds(theirDeviceIdentityKey)

        val messageBody = message["body"] as? String ?: return null
        val messageType = when (val typeAsVoid = message["type"]) {
            is Double -> typeAsVoid.toInt()
            is Int    -> typeAsVoid
            is Long   -> typeAsVoid.toInt()
            else      -> return null
        }

        // Try each session in turn
        // decryptionErrors = {};

        val isPreKey = messageType == 0
        // we want to synchronize on prekey if not we could end up create two olm sessions
        // Not very clear but it looks like the js-sdk for consistency
        return if (isPreKey) {
            olmDevice.mutex.withLock {
                reallyDecryptMessage(sessionIds, messageBody, messageType, theirDeviceIdentityKey)
            }
        } else {
            reallyDecryptMessage(sessionIds, messageBody, messageType, theirDeviceIdentityKey)
        }
    }

    private suspend fun reallyDecryptMessage(sessionIds: List<String>, messageBody: String, messageType: Int, theirDeviceIdentityKey: String): String? {
        Timber.tag(loggerTag.value).d("decryptMessage() try to decrypt olm message type:$messageType from ${sessionIds.size} known sessions")
        for (sessionId in sessionIds) {
            val payload = try {
                olmDevice.decryptMessage(messageBody, messageType, sessionId, theirDeviceIdentityKey)
            } catch (throwable: Exception) {
                // As we are trying one by one, we don't really care of the error here
                Timber.tag(loggerTag.value).d("decryptMessage() failed with session $sessionId")
                null
            }

            if (null != payload) {
                Timber.tag(loggerTag.value).v("## decryptMessage() : Decrypted Olm message from $theirDeviceIdentityKey with session $sessionId")
                return payload
            } else {
                val foundSession = olmDevice.matchesSession(theirDeviceIdentityKey, sessionId, messageType, messageBody)

                if (foundSession) {
                    // Decryption failed, but it was a prekey message matching this
                    // session, so it should have worked.
                    Timber.tag(loggerTag.value).e("## decryptMessage() : Error decrypting prekey message with existing session id $sessionId:TODO")
                    return null
                }
            }
        }

        if (messageType != 0) {
            // not a prekey message, so it should have matched an existing session, but it
            // didn't work.

            if (sessionIds.isEmpty()) {
                Timber.tag(loggerTag.value).e("## decryptMessage() : No existing sessions")
            } else {
                Timber.tag(loggerTag.value).e("## decryptMessage() : Error decrypting non-prekey message with existing sessions")
            }

            return null
        }

        // prekey message which doesn't match any existing sessions: make a new
        // session.
        // XXXX Possible races here? if concurrent access for same prekey message, we might create 2 sessions?
        Timber.tag(loggerTag.value).d("## decryptMessage() :  Create inbound group session from prekey sender:$theirDeviceIdentityKey")

        val res = olmDevice.createInboundSession(theirDeviceIdentityKey, messageType, messageBody)

        if (null == res) {
            Timber.tag(loggerTag.value).e("## decryptMessage() :  Error decrypting non-prekey message with existing sessions")
            return null
        }

        Timber.tag(loggerTag.value).v("## decryptMessage() :  Created new inbound Olm session get id ${res["session_id"]} with $theirDeviceIdentityKey")

        return res["payload"]
    }

    override fun requestKeysForEvent(event: Event, withHeld: Boolean) {
        // nop
    }
}
