/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoStart
import org.matrix.android.sdk.internal.util.JsonCanonicalizer

/**
 * Sent by Alice to initiate an interactive key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationStart(
        @Json(name = "from_device") override val fromDevice: String? = null,
        @Json(name = "method") override val method: String? = null,
        @Json(name = "transaction_id") override val transactionId: String? = null,
        @Json(name = "key_agreement_protocols") override val keyAgreementProtocols: List<String>? = null,
        @Json(name = "hashes") override val hashes: List<String>? = null,
        @Json(name = "message_authentication_codes") override val messageAuthenticationCodes: List<String>? = null,
        @Json(name = "short_authentication_string") override val shortAuthenticationStrings: List<String>? = null,
        // For QR code verification
        @Json(name = "secret") override val sharedSecret: String? = null
) : SendToDeviceObject, VerificationInfoStart {

    override fun toCanonicalJson(): String {
        return JsonCanonicalizer.getCanonicalJson(KeyVerificationStart::class.java, this)
    }

    override fun toSendToDeviceObject() = this
}
