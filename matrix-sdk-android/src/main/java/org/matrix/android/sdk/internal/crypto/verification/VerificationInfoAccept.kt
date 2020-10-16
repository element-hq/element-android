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
package org.matrix.android.sdk.internal.crypto.verification

internal interface VerificationInfoAccept : VerificationInfo<ValidVerificationInfoAccept> {
    /**
     * The key agreement protocol that Bob’s device has selected to use, out of the list proposed by Alice’s device
     */
    val keyAgreementProtocol: String?

    /**
     * The hash algorithm that Bob’s device has selected to use, out of the list proposed by Alice’s device
     */
    val hash: String?

    /**
     * The message authentication code that Bob’s device has selected to use, out of the list proposed by Alice’s device
     */
    val messageAuthenticationCode: String?

    /**
     * An array of short authentication string methods that Bob’s client (and Bob) understands.  Must be a subset of the list proposed by Alice’s device
     */
    val shortAuthenticationStrings: List<String>?

    /**
     * The hash (encoded as unpadded base64) of the concatenation of the device’s ephemeral public key (QB, encoded as unpadded base64)
     *  and the canonical JSON representation of the m.key.verification.start message.
     */
    var commitment: String?

    override fun asValidObject(): ValidVerificationInfoAccept? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validKeyAgreementProtocol = keyAgreementProtocol?.takeIf { it.isNotEmpty() } ?: return null
        val validHash = hash?.takeIf { it.isNotEmpty() } ?: return null
        val validMessageAuthenticationCode = messageAuthenticationCode?.takeIf { it.isNotEmpty() } ?: return null
        val validShortAuthenticationStrings = shortAuthenticationStrings?.takeIf { it.isNotEmpty() } ?: return null
        val validCommitment = commitment?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoAccept(
                validTransactionId,
                validKeyAgreementProtocol,
                validHash,
                validMessageAuthenticationCode,
                validShortAuthenticationStrings,
                validCommitment
        )
    }
}

internal interface VerificationInfoAcceptFactory {

    fun create(tid: String,
               keyAgreementProtocol: String,
               hash: String,
               commitment: String,
               messageAuthenticationCode: String,
               shortAuthenticationStrings: List<String>): VerificationInfoAccept
}

internal data class ValidVerificationInfoAccept(
        val transactionId: String,
        val keyAgreementProtocol: String,
        val hash: String,
        val messageAuthenticationCode: String,
        val shortAuthenticationStrings: List<String>,
        var commitment: String?
)
