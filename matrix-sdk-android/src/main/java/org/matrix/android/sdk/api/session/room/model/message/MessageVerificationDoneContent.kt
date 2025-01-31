/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfo

@JsonClass(generateAdapter = true)
internal data class MessageVerificationDoneContent(
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerificationInfo<ValidVerificationDone> {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent(): Content? = toContent()

    override fun asValidObject(): ValidVerificationDone? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationDone(
                validTransactionId
        )
    }
}

internal data class ValidVerificationDone(
        val transactionId: String
)
