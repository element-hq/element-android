/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms.olm

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.MXDecryptionException
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.model.event.OlmEventContent
import im.vector.matrix.android.internal.crypto.model.event.OlmPayloadContent
import im.vector.matrix.android.internal.util.convertFromUTF8
import timber.log.Timber
import java.util.*

internal class MXOlmDecryption(
        // The olm device interface
        private val mOlmDevice: MXOlmDevice,

        // the matrix credentials
        private val mCredentials: Credentials)
    : IMXDecrypting {

    @Throws(MXDecryptionException::class)
    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult? {
        val olmEventContent = event.content.toModel<OlmEventContent>()!!

        if (null == olmEventContent.ciphertext) {
            Timber.e("## decryptEvent() : missing cipher text")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_CIPHER_TEXT_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_CIPHER_TEXT_REASON))
        }

        if (!olmEventContent.ciphertext!!.containsKey(mOlmDevice.deviceCurve25519Key)) {
            Timber.e("## decryptEvent() : our device " + mOlmDevice.deviceCurve25519Key
                    + " is not included in recipients. Event")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.NOT_INCLUDE_IN_RECIPIENTS_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.NOT_INCLUDED_IN_RECIPIENT_REASON))
        }

        // The message for myUser
        val message = olmEventContent.ciphertext!![mOlmDevice.deviceCurve25519Key] as Map<String, Any>
        val payloadString = decryptMessage(message, olmEventContent.senderKey!!)

        if (null == payloadString) {
            Timber.e("## decryptEvent() Failed to decrypt Olm event (id= " + event.eventId + " ) from " + olmEventContent.senderKey)
            throw MXDecryptionException(MXCryptoError(MXCryptoError.BAD_ENCRYPTED_MESSAGE_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.BAD_ENCRYPTED_MESSAGE_REASON))
        }

        val payload = convertFromUTF8(payloadString)

        if (null == payload) {
            Timber.e("## decryptEvent failed : null payload")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.UNABLE_TO_DECRYPT_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_CIPHER_TEXT_REASON))
        }

        val olmPayloadContent = OlmPayloadContent.fromJsonString(payload)

        if (TextUtils.isEmpty(olmPayloadContent.recipient)) {
            val reason = String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient")
            Timber.e("## decryptEvent() : $reason")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_PROPERTY_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, reason))
        }

        if (!TextUtils.equals(olmPayloadContent.recipient, mCredentials.userId)) {
            Timber.e("## decryptEvent() : Event " + event.eventId + ": Intended recipient " + olmPayloadContent.recipient
                    + " does not match our id " + mCredentials.userId)
            throw MXDecryptionException(MXCryptoError(MXCryptoError.BAD_RECIPIENT_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, String.format(MXCryptoError.BAD_RECIPIENT_REASON, olmPayloadContent.recipient)))
        }

        if (null == olmPayloadContent.recipient_keys) {
            Timber.e("## decryptEvent() : Olm event (id=" + event.eventId
                    + ") contains no " + "'recipient_keys' property; cannot prevent unknown-key attack")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_PROPERTY_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "recipient_keys")))
        }

        val ed25519 = olmPayloadContent.recipient_keys!!.get("ed25519")

        if (!TextUtils.equals(ed25519, mOlmDevice.deviceEd25519Key)) {
            Timber.e("## decryptEvent() : Event " + event.eventId + ": Intended recipient ed25519 key " + ed25519 + " did not match ours")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.BAD_RECIPIENT_KEY_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.BAD_RECIPIENT_KEY_REASON))
        }

        if (TextUtils.isEmpty(olmPayloadContent.sender)) {
            Timber.e("## decryptEvent() : Olm event (id=" + event.eventId
                    + ") contains no 'sender' property; cannot prevent unknown-key attack")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_PROPERTY_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, String.format(MXCryptoError.ERROR_MISSING_PROPERTY_REASON, "sender")))
        }

        if (!TextUtils.equals(olmPayloadContent.sender, event.sender)) {
            Timber.e("Event " + event.eventId + ": original sender " + olmPayloadContent.sender
                    + " does not match reported sender " + event.sender)
            throw MXDecryptionException(MXCryptoError(MXCryptoError.FORWARDED_MESSAGE_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, String.format(MXCryptoError.FORWARDED_MESSAGE_REASON, olmPayloadContent.sender)))
        }

        if (!TextUtils.equals(olmPayloadContent.room_id, event.roomId)) {
            Timber.e("## decryptEvent() : Event " + event.eventId + ": original room " + olmPayloadContent.room_id
                    + " does not match reported room " + event.roomId)
            throw MXDecryptionException(MXCryptoError(MXCryptoError.BAD_ROOM_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, String.format(MXCryptoError.BAD_ROOM_REASON, olmPayloadContent.room_id)))
        }

        if (null == olmPayloadContent.keys) {
            Timber.e("## decryptEvent failed : null keys")
            throw MXDecryptionException(MXCryptoError(MXCryptoError.UNABLE_TO_DECRYPT_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_CIPHER_TEXT_REASON))
        }

        val result = MXEventDecryptionResult()
        // FIXME result.mClearEvent = payload
        result.mSenderCurve25519Key = olmEventContent.senderKey
        result.mClaimedEd25519Key = olmPayloadContent.keys!!.get("ed25519")

        return result
    }

    /**
     * Attempt to decrypt an Olm message.
     *
     * @param theirDeviceIdentityKey the Curve25519 identity key of the sender.
     * @param message                message object, with 'type' and 'body' fields.
     * @return payload, if decrypted successfully.
     */
    private fun decryptMessage(message: Map<String, Any>, theirDeviceIdentityKey: String): String? {
        val sessionIdsSet = mOlmDevice.getSessionIds(theirDeviceIdentityKey)

        val sessionIds: List<String>

        if (null == sessionIdsSet) {
            sessionIds = ArrayList()
        } else {
            sessionIds = ArrayList(sessionIdsSet)
        }

        val messageBody = message["body"] as String?
        var messageType: Int? = null

        val typeAsVoid = message["type"]

        if (null != typeAsVoid) {
            if (typeAsVoid is Double) {
                messageType = typeAsVoid.toInt()
            } else if (typeAsVoid is Int) {
                messageType = typeAsVoid
            } else if (typeAsVoid is Long) {
                messageType = typeAsVoid.toInt()
            }
        }

        if (null == messageBody || null == messageType) {
            return null
        }

        // Try each session in turn
        // decryptionErrors = {};
        for (sessionId in sessionIds) {
            val payload = mOlmDevice.decryptMessage(messageBody, messageType, sessionId, theirDeviceIdentityKey)

            if (null != payload) {
                Timber.v("## decryptMessage() : Decrypted Olm message from $theirDeviceIdentityKey with session $sessionId")
                return payload
            } else {
                val foundSession = mOlmDevice.matchesSession(theirDeviceIdentityKey, sessionId, messageType, messageBody)

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

            if (sessionIds.size == 0) {
                Timber.e("## decryptMessage() : No existing sessions")
            } else {
                Timber.e("## decryptMessage() : Error decrypting non-prekey message with existing sessions")
            }

            return null
        }

        // prekey message which doesn't match any existing sessions: make a new
        // session.
        val res = mOlmDevice.createInboundSession(theirDeviceIdentityKey, messageType, messageBody)

        if (null == res) {
            Timber.e("## decryptMessage() :  Error decrypting non-prekey message with existing sessions")
            return null
        }

        Timber.v("## decryptMessage() :  Created new inbound Olm session get id " + res["session_id"] + " with " + theirDeviceIdentityKey)

        return res["payload"]
    }
}
