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
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKey
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKeyFactory

@JsonClass(generateAdapter = true)
internal data class MessageVerificationKeyContent(
        /**
         * The device’s ephemeral public key, as an unpadded base64 string.
         */
        @Json(name = "key") override val key: String? = null,
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerificationInfoKey {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent() = toContent()

    companion object : VerificationInfoKeyFactory {

        override fun create(tid: String, pubKey: String): VerificationInfoKey {
            return MessageVerificationKeyContent(
                    pubKey,
                    RelationDefaultContent(
                            RelationType.REFERENCE,
                            tid
                    )
            )
        }
    }
}
