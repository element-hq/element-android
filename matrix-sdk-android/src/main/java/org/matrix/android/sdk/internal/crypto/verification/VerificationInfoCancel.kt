/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

internal interface VerificationInfoCancel : VerificationInfo<ValidVerificationInfoCancel> {
    /**
     * machine-readable reason for cancelling, see [CancelCode].
     */
    val code: String?

    /**
     * human-readable reason for cancelling.  This should only be used if the receiving client does not understand the code given.
     */
    val reason: String?

    override fun asValidObject(): ValidVerificationInfoCancel? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validCode = code?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoCancel(
                validTransactionId,
                validCode,
                reason
        )
    }
}

internal data class ValidVerificationInfoCancel(
        val transactionId: String,
        val code: String,
        val reason: String?
)
