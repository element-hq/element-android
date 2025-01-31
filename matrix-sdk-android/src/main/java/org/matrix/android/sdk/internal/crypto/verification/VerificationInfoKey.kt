/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

/**
 * Sent by both devices to send their ephemeral Curve25519 public key to the other device.
 */
internal interface VerificationInfoKey : VerificationInfo<ValidVerificationInfoKey> {
    /**
     * The device’s ephemeral public key, as an unpadded base64 string.
     */
    val key: String?

    override fun asValidObject(): ValidVerificationInfoKey? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validKey = key?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoKey(
                validTransactionId,
                validKey
        )
    }
}

internal interface VerificationInfoKeyFactory {
    fun create(tid: String, pubKey: String): VerificationInfoKey
}

internal data class ValidVerificationInfoKey(
        val transactionId: String,
        val key: String
)
