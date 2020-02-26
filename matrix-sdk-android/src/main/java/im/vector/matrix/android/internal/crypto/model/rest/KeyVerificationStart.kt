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
import im.vector.matrix.android.api.session.crypto.verification.SasMode
import im.vector.matrix.android.internal.crypto.verification.SASDefaultVerificationTransaction
import im.vector.matrix.android.internal.crypto.verification.VerificationInfoStart
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import timber.log.Timber

/**
 * Sent by Alice to initiate an interactive key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationStart(
        @Json(name = "from_device") override val fromDevice: String? = null,
        override val method: String? = null,
        @Json(name = "transaction_id") override val transactionID: String? = null,
        @Json(name = "key_agreement_protocols") override val keyAgreementProtocols: List<String>? = null,
        @Json(name = "hashes") override val hashes: List<String>? = null,
        @Json(name = "message_authentication_codes") override val messageAuthenticationCodes: List<String>? = null,
        @Json(name = "short_authentication_string") override val shortAuthenticationStrings: List<String>? = null,
        // For QR code verification
        @Json(name = "secret") override val sharedSecret: String? = null
) : SendToDeviceObject, VerificationInfoStart {

    override fun toCanonicalJson(): String? {
        return JsonCanonicalizer.getCanonicalJson(KeyVerificationStart::class.java, this)
    }

    // TODO Move those method to the interface?
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
        if (keyAgreementProtocols.isNullOrEmpty()
                || hashes.isNullOrEmpty()
                || !hashes.contains("sha256") || messageAuthenticationCodes.isNullOrEmpty()
                || (!messageAuthenticationCodes.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256)
                        && !messageAuthenticationCodes.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256_LONGKDF))
                || shortAuthenticationStrings.isNullOrEmpty()
                || !shortAuthenticationStrings.contains(SasMode.DECIMAL)) {
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

    override fun toSendToDeviceObject() = this
}
