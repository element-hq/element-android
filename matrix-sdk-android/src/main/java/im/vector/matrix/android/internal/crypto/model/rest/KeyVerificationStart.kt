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
import im.vector.matrix.android.api.session.crypto.sas.SasMode
import im.vector.matrix.android.internal.crypto.verification.SASVerificationTransaction
import timber.log.Timber

/**
 * Sent by Alice to initiate an interactive key verification.
 */
@JsonClass(generateAdapter = true)
class KeyVerificationStart : SendToDeviceObject {

    /**
     * Alice’s device ID
     */
    @Json(name = "from_device")
    var fromDevice: String? = null

    var method: String? = null

    /**
     * String to identify the transaction.
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid.
     * Alice’s device should record this ID and use it in future messages in this transaction.
     */
    @Json(name = "transaction_id")
    var transactionID: String? = null

    /**
     * An array of key agreement protocols that Alice’s client understands.
     * Must include “curve25519”.
     * Other methods may be defined in the future
     */
    @Json(name = "key_agreement_protocols")
    var keyAgreementProtocols: List<String>? = null

    /**
     * An array of hashes that Alice’s client understands.
     * Must include “sha256”.  Other methods may be defined in the future.
     */
    var hashes: List<String>? = null

    /**
     * An array of message authentication codes that Alice’s client understands.
     * Must include “hkdf-hmac-sha256”.
     * Other methods may be defined in the future.
     */
    @Json(name = "message_authentication_codes")
    var messageAuthenticationCodes: List<String>? = null

    /**
     * An array of short authentication string methods that Alice’s client (and Alice) understands.
     * Must include “decimal”.
     * This document also describes the “emoji” method.
     * Other methods may be defined in the future
     */
    @Json(name = "short_authentication_string")
    var shortAuthenticationStrings: List<String>? = null


    companion object {
        const val VERIF_METHOD_SAS = "m.sas.v1"
    }

    fun isValid(): Boolean {
        if (transactionID.isNullOrBlank()
                || fromDevice.isNullOrBlank()
                || method != VERIF_METHOD_SAS
                || keyAgreementProtocols.isNullOrEmpty()
                || hashes.isNullOrEmpty()
                || hashes?.contains("sha256") == false
                || messageAuthenticationCodes.isNullOrEmpty()
                || (messageAuthenticationCodes?.contains(SASVerificationTransaction.SAS_MAC_SHA256) == false
                        && messageAuthenticationCodes?.contains(SASVerificationTransaction.SAS_MAC_SHA256_LONGKDF) == false)
                || shortAuthenticationStrings.isNullOrEmpty()
                || shortAuthenticationStrings?.contains(SasMode.DECIMAL) == false) {
            Timber.e("## received invalid verification request")
            return false
        }
        return true
    }
}
