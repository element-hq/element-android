/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.room.model.message.ValidVerificationDone

internal interface VerificationInfoDone : VerificationInfo<ValidVerificationDone> {

    override fun asValidObject(): ValidVerificationDone? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        return ValidVerificationDone(validTransactionId)
    }
}
