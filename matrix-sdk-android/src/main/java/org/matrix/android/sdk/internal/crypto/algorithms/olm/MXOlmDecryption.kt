/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

internal class MXOlmDecryption(
        // The olm device interface
        private val olmDevice: MXOlmDevice,
        // the matrix userId
        private val userId: String)
    : IMXDecrypting {

    @Throws(MXCryptoError::class)
    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        val olmEventContent = event.content.toModel<OlmEventContent>() ?: run {
            Timber.e("## decryptEvent() : bad event format")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_EVENT_FORMAT,
                    MXCryptoError.BAD_EVENT_FORMAT_TEXT_REASON)
        }

        val cipherText = olmEventContent.ciphertext ?: run {
            Timber.e("## decryptEvent() : missing cipher text")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_CIPHER_TEXT,
                    MXCryptoError.MISSING_CIPHER_TEXT_REASON)
        }

        val senderKey = olmEventContent.senderKey ?: run {
            Timber.e("## decryptEvent() : missing sender key")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_SENDER_KEY,
                    MXCryptoError.MISSING_SENDER_KEY_TEXT_REASON)
        }

        val messageAny = cipherText[olmDevice.deviceCurve25519Key] ?: run {
            Timber.e("## decryptEvent() : our device ${olmDevice.deviceCurve25519Key} is not included in recipients")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.NOT_INCLUDE_IN_RECIPIENTS, MXCryptoError.NOT_INCLUDED_IN_RECIPIENT_REASON)
        }

        // The message for myUser
        @Suppress("UNCHECKED_CAST")
        val message = messageAny as JsonDict

        val decryptedPayload = decryptMessage(message, senderKey)

        if (decryptedPayload == null) {
            Timber.e("## decryptEvent() Failed to decrypt Olm event (id= ${event.eventId} from $senderKey")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_ENCRYPTED_MESSAGE, MXCryptoError.BAD_ENCRYPTED_MESSAGE_REASON)
        }
        val payloadString = convertFromUTF8(decryptedPayload)

        val adapter = MoshiProvider.providesMoshi().adapter<JsonDict>(JSON_DICT_PARAMETERIZED_TYPE)
        val payload = adapter.fromJson(payloadString)

        if (payload == null) {
            Timber.e("## decryptEvent failed : null payload")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_CIPHER_TEXT_REASON)
        }

        val olmPayloadContent = OlmPayloadContent.fromJsonString(payloadString) ?: run {
            Timber.e("## decryptEvent() : bad olmPayloadContent format")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_DECRYPTED_FORMAT, MXCryptoError.BAD_DECRYPTED_FORMAT_TEXT_REASON)
        }

        if (olmPayloadContent.recipient.isNullOrBlank()) {
            val reason = String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient")
            Timber.e("## decryptEvent() : $reason")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY, reason)
        }

        if (olmPayloadContent.recipient != userId) {
            Timber.e("## decryptEvent() : Event ${event.eventId}:" +
                    " Intended recipient ${olmPayloadContent.recipient} does not match our id $userId")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_RECIPIENT,
                    String.format(MXCryptoError.BAD_RECIPIENT_REASON, olmPayloadContent.recipient))
        }

        val recipientKeys = olmPayloadContent.recipient_keys ?: run {
            Timber.e("## decryptEvent() : Olm event (id=${event.eventId}) contains no 'recipient_keys'" +
                    " property; cannot prevent unknown-key attack")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY,
                    String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient_keys"))
        }

        val ed25519 = recipientKeys["ed25519"]

        if (ed25519 != olmDevice.deviceEd25519Key) {
            Timber.e("## decryptEvent() : Event ${event.eventId}: Intended recipient ed25519 key $ed25519 did not match ours")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_RECIPIENT_KEY,
                    MXCryptoError.BAD_RECIPIENT_KEY_REASON)
        }

        if (olmPayloadContent.sender.isNullOrBlank()) {
            Timber.e("## decryptEvent() : Olm event (id=${event.eventId}) contains no 'sender' property; cannot prevent unknown-key attack")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_PROPERTY,
                    String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "sender"))
        }

        if (olmPayloadContent.sender != event.senderId) {
            Timber.e("Event ${event.eventId}: original sender ${olmPayloadContent.sender} does not match reported sender ${event.senderId}")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.FORWARDED_MESSAGE,
                    String.format(MXCryptoError.FORWARDED_MESSAGE_REASON, olmPayloadContent.sender))
        }

        if (olmPayloadContent.room_id != event.roomId) {
            Timber.e("## decryptEvent() : Event ${event.eventId}: original room ${olmPayloadContent.room_id} does not match reported room ${event.roomId}")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_ROOM,
                    String.format(MXCryptoError.BAD_ROOM_REASON, olmPayloadContent.room_id))
        }

        val keys = olmPayloadContent.keys ?: run {
            Timber.e("## decryptEvent failed : null keys")
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
    private fun decryptMessage(message: JsonDict, theirDeviceIdentityKey: String): String? {
        val sessionIds = olmDevice.getSessionIds(theirDeviceIdentityKey) ?: emptySet()

        val messageBody = message["body"] as? String ?: return null
        val messageType = when (val typeAsVoid = message["type"]) {
            is Double -> typeAsVoid.toInt()
            is Int    -> typeAsVoid
            is Long   -> typeAsVoid.toInt()
            else      -> return null
        }

        // Try each session in turn
        // decryptionErrors = {};
        for (sessionId in sessionIds) {
            val payload = olmDevice.decryptMessage(messageBody, messageType, sessionId, theirDeviceIdentityKey)

            if (null != payload) {
                Timber.v("## decryptMessage() : Decrypted Olm message from $theirDeviceIdentityKey with session $sessionId")
                return payload
            } else {
                val foundSession = olmDevice.matchesSession(theirDeviceIdentityKey, sessionId, messageType, messageBody)

                if (foundSession) {
                    // Decryption failed, but it was a prekey message matching this
                    // session, so it should have worked.
                    Timber.e("## decryptMessage() : Error decrypting prekey message with existing session id $sessionId:TODO")
                    return null
                }
            }
        }

        if (messageType != 0) {
            // not a prekey message, so it should have matched an existing session, but it
            // didn't work.

            if (sessionIds.isEmpty()) {
                Timber.e("## decryptMessage() : No existing sessions")
            } else {
                Timber.e("## decryptMessage() : Error decrypting non-prekey message with existing sessions")
            }

            return null
        }

        // prekey message which doesn't match any existing sessions: make a new
        // session.
        val res = olmDevice.createInboundSession(theirDeviceIdentityKey, messageType, messageBody)

        if (null == res) {
            Timber.e("## decryptMessage() :  Error decrypting non-prekey message with existing sessions")
            return null
        }

        Timber.v("## decryptMessage() :  Created new inbound Olm session get id ${res["session_id"]} with $theirDeviceIdentityKey")

        return res["payload"]
    }

    override fun requestKeysForEvent(event: Event, withHeld: Boolean) {
        // nop
    }
}
