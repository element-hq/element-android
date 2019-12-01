/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.crypto.verification.VerifInfoAccept
import im.vector.matrix.android.internal.crypto.verification.AcceptVerifInfoFactory
import timber.log.Timber

/**
 * Sent by Bob to accept a verification from a previously sent m.key.verification.start message.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationAccept(

        /**
         * string to identify the transaction.
         * This string must be unique for the pair of users performing verification for the duration that the transaction is valid.
         * Alice’s device should record this ID and use it in future messages in this transaction.
         */
        @Json(name = "transaction_id")
        override var transactionID: String? = null,

        /**
         * The key agreement protocol that Bob’s device has selected to use, out of the list proposed by Alice’s device
         */
        @Json(name = "key_agreement_protocol")
        override var keyAgreementProtocol: String? = null,

        /**
         * The hash algorithm that Bob’s device has selected to use, out of the list proposed by Alice’s device
         */
        @Json(name = "hash")
        override var hash: String? = null,

        /**
         * The message authentication code that Bob’s device has selected to use, out of the list proposed by Alice’s device
         */
        @Json(name = "message_authentication_code")
        override var messageAuthenticationCode: String? = null,

        /**
         * An array of short authentication string methods that Bob’s client (and Bob) understands.  Must be a subset of the list proposed by Alice’s device
         */
        @Json(name = "short_authentication_string")
        override var shortAuthenticationStrings: List<String>? = null,

        /**
         * The hash (encoded as unpadded base64) of the concatenation of the device’s ephemeral public key (QB, encoded as unpadded base64)
         *  and the canonical JSON representation of the m.key.verification.start message.
         */
        @Json(name = "commitment")
        override var commitment: String? = null
) : SendToDeviceObject, VerifInfoAccept {

    override fun isValid(): Boolean {
        if (transactionID.isNullOrBlank()
                || keyAgreementProtocol.isNullOrBlank()
                || hash.isNullOrBlank()
                || commitment.isNullOrBlank()
                || messageAuthenticationCode.isNullOrBlank()
                || shortAuthenticationStrings.isNullOrEmpty()) {
            Timber.e("## received invalid verification request")
            return false
        }
        return true
    }

    override fun toSendToDeviceObject() = this

    companion object : AcceptVerifInfoFactory {
        override fun create(tid: String,
                            keyAgreementProtocol: String,
                            hash: String,
                            commitment: String,
                            messageAuthenticationCode: String,
                            shortAuthenticationStrings: List<String>): VerifInfoAccept {
            return KeyVerificationAccept().apply {
                this.transactionID = tid
                this.keyAgreementProtocol = keyAgreementProtocol
                this.hash = hash
                this.commitment = commitment
                this.messageAuthenticationCode = messageAuthenticationCode
                this.shortAuthenticationStrings = shortAuthenticationStrings
            }
        }
    }
}
