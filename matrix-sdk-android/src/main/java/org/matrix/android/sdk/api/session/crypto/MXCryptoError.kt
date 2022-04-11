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

package org.matrix.android.sdk.api.session.crypto

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.olm.OlmException

/**
 * Represents a crypto error response.
 */
sealed class MXCryptoError : Throwable() {

    data class Base(val errorType: ErrorType,
                    val technicalMessage: String,
                    /**
                     * Describe the error with more details
                     */
                    val detailedErrorDescription: String? = null) : MXCryptoError()

    data class OlmError(val olmException: OlmException) : MXCryptoError()

    data class UnknownDevice(val deviceList: MXUsersDevicesMap<CryptoDeviceInfo>) : MXCryptoError()

    enum class ErrorType {
        ENCRYPTING_NOT_ENABLED,
        UNABLE_TO_ENCRYPT,
        UNABLE_TO_DECRYPT,
        UNKNOWN_INBOUND_SESSION_ID,
        INBOUND_SESSION_MISMATCH_ROOM_ID,
        MISSING_FIELDS,
        BAD_EVENT_FORMAT,
        MISSING_SENDER_KEY,
        MISSING_CIPHER_TEXT,
        BAD_DECRYPTED_FORMAT,
        NOT_INCLUDE_IN_RECIPIENTS,
        BAD_RECIPIENT,
        BAD_RECIPIENT_KEY,
        FORWARDED_MESSAGE,
        BAD_ROOM,
        BAD_ENCRYPTED_MESSAGE,
        DUPLICATED_MESSAGE_INDEX,
        MISSING_PROPERTY,
        OLM,
        UNKNOWN_DEVICES,
        UNKNOWN_MESSAGE_INDEX,
        KEYS_WITHHELD
    }

    companion object {
        /**
         * Resource for technicalMessage
         */
        const val UNABLE_TO_ENCRYPT_REASON = "Unable to encrypt %s"
        const val UNABLE_TO_DECRYPT_REASON = "Unable to decrypt %1\$s. Algorithm: %2\$s"
        const val OLM_REASON = "OLM error: %1\$s"
        const val DETAILED_OLM_REASON = "Unable to decrypt %1\$s. OLM error: %2\$s"
        const val UNKNOWN_INBOUND_SESSION_ID_REASON = "Unknown inbound session id"
        const val INBOUND_SESSION_MISMATCH_ROOM_ID_REASON = "Mismatched room_id for inbound group session (expected %1\$s, was %2\$s)"
        const val MISSING_FIELDS_REASON = "Missing fields in input"
        const val BAD_EVENT_FORMAT_TEXT_REASON = "Bad event format"
        const val MISSING_SENDER_KEY_TEXT_REASON = "Missing senderKey"
        const val MISSING_CIPHER_TEXT_REASON = "Missing ciphertext"
        const val BAD_DECRYPTED_FORMAT_TEXT_REASON = "Bad decrypted event format"
        const val NOT_INCLUDED_IN_RECIPIENT_REASON = "Not included in recipients"
        const val BAD_RECIPIENT_REASON = "Message was intended for %1\$s"
        const val BAD_RECIPIENT_KEY_REASON = "Message not intended for this device"
        const val FORWARDED_MESSAGE_REASON = "Message forwarded from %1\$s"
        const val BAD_ROOM_REASON = "Message intended for room %1\$s"
        const val BAD_ENCRYPTED_MESSAGE_REASON = "Bad Encrypted Message"
        const val DUPLICATE_MESSAGE_INDEX_REASON = "Duplicate message index, possible replay attack %1\$s"
        const val ERROR_MISSING_PROPERTY_REASON = "No '%1\$s' property. Cannot prevent unknown-key attack"
        const val UNKNOWN_DEVICES_REASON = "This room contains unknown devices which have not been verified.\n" +
                "We strongly recommend you verify them before continuing."
        const val NO_MORE_ALGORITHM_REASON = "Room was previously configured to use encryption, but is no longer." +
                " Perhaps the homeserver is hiding the configuration event."
    }
}
