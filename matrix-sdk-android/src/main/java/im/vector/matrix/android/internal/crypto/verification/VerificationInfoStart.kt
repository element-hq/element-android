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
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.api.session.crypto.verification.SasMode
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_SAS

internal interface VerificationInfoStart : VerificationInfo<ValidVerificationInfoStart> {

    val method: String?

    /**
     * Alice’s device ID
     */
    val fromDevice: String?

    /**
     * An array of key agreement protocols that Alice’s client understands.
     * Must include “curve25519”.
     * Other methods may be defined in the future
     */
    val keyAgreementProtocols: List<String>?

    /**
     * An array of hashes that Alice’s client understands.
     * Must include “sha256”.  Other methods may be defined in the future.
     */
    val hashes: List<String>?

    /**
     * An array of message authentication codes that Alice’s client understands.
     * Must include “hkdf-hmac-sha256”.
     * Other methods may be defined in the future.
     */
    val messageAuthenticationCodes: List<String>?

    /**
     * An array of short authentication string methods that Alice’s client (and Alice) understands.
     * Must include “decimal”.
     * This document also describes the “emoji” method.
     * Other methods may be defined in the future
     */
    val shortAuthenticationStrings: List<String>?

    /**
     * Shared secret, when starting verification with QR code
     */
    val sharedSecret: String?

    fun toCanonicalJson(): String

    override fun asValidObject(): ValidVerificationInfoStart? {
        if (transactionID.isNullOrBlank()
                || fromDevice.isNullOrBlank()) {
            return null
        }

        return when (method) {
            VERIFICATION_METHOD_SAS         -> {
                if (keyAgreementProtocols.isNullOrEmpty()
                        || hashes.isNullOrEmpty()
                        || !hashes!!.contains("sha256") || messageAuthenticationCodes.isNullOrEmpty()
                        || (!messageAuthenticationCodes!!.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256)
                                && !messageAuthenticationCodes!!.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256_LONGKDF))
                        || shortAuthenticationStrings.isNullOrEmpty()
                        || !shortAuthenticationStrings!!.contains(SasMode.DECIMAL)) {
                    null
                } else {
                    ValidVerificationInfoStart.SasVerificationInfoStart(
                            transactionID!!,
                            fromDevice!!,
                            keyAgreementProtocols!!,
                            hashes!!,
                            messageAuthenticationCodes!!,
                            shortAuthenticationStrings!!,
                            canonicalJson = toCanonicalJson()
                    )
                }
            }
            VERIFICATION_METHOD_RECIPROCATE -> {
                if (sharedSecret.isNullOrBlank()) {
                    null
                } else {
                    ValidVerificationInfoStart.ReciprocateVerificationInfoStart(
                            transactionID!!,
                            fromDevice!!,
                            sharedSecret!!
                    )
                }
            }
            else                            -> null
        }
    }
}

sealed class ValidVerificationInfoStart(
        open val transactionID: String,
        open val fromDevice: String) {
    data class SasVerificationInfoStart(
            override val transactionID: String,
            override val fromDevice: String,
            val keyAgreementProtocols: List<String>,
            val hashes: List<String>,
            val messageAuthenticationCodes: List<String>,
            val shortAuthenticationStrings: List<String>,
            val canonicalJson: String
    ) : ValidVerificationInfoStart(transactionID, fromDevice)

    data class ReciprocateVerificationInfoStart(
            override val transactionID: String,
            override val fromDevice: String,
            val sharedSecret: String
    ) : ValidVerificationInfoStart(transactionID, fromDevice)
}
