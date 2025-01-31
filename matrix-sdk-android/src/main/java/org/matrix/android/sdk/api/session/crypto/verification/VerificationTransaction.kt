/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.verification

interface VerificationTransaction {

    var state: VerificationTxState

    val transactionId: String
    val otherUserId: String
    var otherDeviceId: String?

    // TODO Not used. Remove?
    val isIncoming: Boolean

    /**
     * User wants to cancel the transaction.
     */
    fun cancel()

    fun cancel(code: CancelCode)

    fun isToDeviceTransport(): Boolean
}
