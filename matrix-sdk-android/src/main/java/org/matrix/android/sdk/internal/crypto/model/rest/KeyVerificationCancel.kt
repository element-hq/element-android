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
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoCancel

/**
 * To device event sent by either party to cancel a key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationCancel(
        /**
         * the transaction ID of the verification to cancel.
         */
        @Json(name = "transaction_id")
        override val transactionId: String? = null,

        /**
         * machine-readable reason for cancelling, see #CancelCode.
         */
        override val code: String? = null,

        /**
         * human-readable reason for cancelling. This should only be used if the receiving client does not understand the code given.
         */
        override val reason: String? = null
) : SendToDeviceObject, VerificationInfoCancel {

    companion object {
        fun create(tid: String, cancelCode: CancelCode): KeyVerificationCancel {
            return KeyVerificationCancel(
                    tid,
                    cancelCode.value,
                    cancelCode.humanReadable
            )
        }
    }

    override fun toSendToDeviceObject() = this
}
