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
import timber.log.Timber

internal interface VerificationInfoStart : VerificationInfo {

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

    fun toCanonicalJson(): String?

    override fun isValid(): Boolean {
        if (transactionID.isNullOrBlank()
                || fromDevice.isNullOrBlank()
                || (method == VERIFICATION_METHOD_SAS && !isValidSas())
                || (method == VERIFICATION_METHOD_RECIPROCATE && !isValidReciprocate())) {
            Timber.e("## received invalid verification request")
            return false
        }
        return true
    }

    private fun isValidSas(): Boolean {
        val myHashes = hashes
        val myMessageAuthenticationCodes = messageAuthenticationCodes
        val myShortAuthenticationStrings = shortAuthenticationStrings

        if (keyAgreementProtocols.isNullOrEmpty()
                || myHashes.isNullOrEmpty()
                || !myHashes.contains("sha256") || myMessageAuthenticationCodes.isNullOrEmpty()
                || (!myMessageAuthenticationCodes.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256)
                        && !myMessageAuthenticationCodes.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256_LONGKDF))
                || myShortAuthenticationStrings.isNullOrEmpty()
                || !myShortAuthenticationStrings.contains(SasMode.DECIMAL)) {
            return false
        }

        return true
    }

    private fun isValidReciprocate(): Boolean {
        if (sharedSecret.isNullOrBlank()) {
            return false
        }

        return true
    }
}
