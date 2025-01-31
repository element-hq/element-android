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
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKey
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKeyFactory

/**
 * Sent by both devices to send their ephemeral Curve25519 public key to the other device.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationKey(
        /**
         * The ID of the transaction that the message is part of.
         */
        @Json(name = "transaction_id") override val transactionId: String? = null,

        /**
         * The device’s ephemeral public key, as an unpadded base64 string.
         */
        @Json(name = "key") override val key: String? = null

) : SendToDeviceObject, VerificationInfoKey {

    companion object : VerificationInfoKeyFactory {
        override fun create(tid: String, pubKey: String): KeyVerificationKey {
            return KeyVerificationKey(tid, pubKey)
        }
    }

    override fun toSendToDeviceObject() = this
}
