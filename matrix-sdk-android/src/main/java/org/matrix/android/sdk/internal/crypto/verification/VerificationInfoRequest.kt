/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest

internal interface VerificationInfoRequest : VerificationInfo<ValidVerificationInfoRequest> {

    /**
     * Required. The device ID which is initiating the request.
     */
    val fromDevice: String?

    /**
     * Required. The verification methods supported by the sender.
     */
    val methods: List<String>?

    /**
     * The POSIX timestamp in milliseconds for when the request was made.
     * If the request is in the future by more than 5 minutes or more than 10 minutes in the past,
     * the message should be ignored by the receiver.
     */
    val timestamp: Long?

    override fun asValidObject(): ValidVerificationInfoRequest? {
        // FIXME No check on Timestamp?
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validFromDevice = fromDevice?.takeIf { it.isNotEmpty() } ?: return null
        val validMethods = methods?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoRequest(
                validTransactionId,
                validFromDevice,
                validMethods,
                timestamp
        )
    }
}
