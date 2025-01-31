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
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoCancel

@JsonClass(generateAdapter = true)
data class MessageVerificationCancelContent(
        @Json(name = "code") override val code: String? = null,
        @Json(name = "reason") override val reason: String? = null,
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerificationInfoCancel {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent() = toContent()

    companion object {
        fun create(transactionId: String, reason: CancelCode): MessageVerificationCancelContent {
            return MessageVerificationCancelContent(
                    reason.value,
                    reason.humanReadable,
                    RelationDefaultContent(
                            RelationType.REFERENCE,
                            transactionId
                    )
            )
        }
    }
}
