/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.api.session.events.model.Content

internal interface VerificationInfo<ValidObjectType> {
    fun toEventContent(): Content? = null
    fun toSendToDeviceObject(): SendToDeviceObject? = null

    fun asValidObject(): ValidObjectType?

    /**
     * String to identify the transaction.
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid.
     * Alice’s device should record this ID and use it in future messages in this transaction.
     */
    val transactionId: String?
}
