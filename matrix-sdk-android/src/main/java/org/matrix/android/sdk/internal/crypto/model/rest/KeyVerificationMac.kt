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
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoMac
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoMacFactory

/**
 * Sent by both devices to send the MAC of their device key to the other device.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationMac(
        @Json(name = "transaction_id") override val transactionId: String? = null,
        @Json(name = "mac") override val mac: Map<String, String>? = null,
        @Json(name = "keys") override val keys: String? = null

) : SendToDeviceObject, VerificationInfoMac {

    override fun toSendToDeviceObject(): SendToDeviceObject? = this

    companion object : VerificationInfoMacFactory {
        override fun create(tid: String, mac: Map<String, String>, keys: String): VerificationInfoMac {
            return KeyVerificationMac(tid, mac, keys)
        }
    }
}
