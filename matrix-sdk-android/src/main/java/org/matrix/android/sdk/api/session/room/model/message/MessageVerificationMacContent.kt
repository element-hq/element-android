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
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoMac
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoMacFactory

@JsonClass(generateAdapter = true)
internal data class MessageVerificationMacContent(
        @Json(name = "mac") override val mac: Map<String, String>? = null,
        @Json(name = "keys") override val keys: String? = null,
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerificationInfoMac {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent() = toContent()

    companion object : VerificationInfoMacFactory {
        override fun create(tid: String, mac: Map<String, String>, keys: String): VerificationInfoMac {
            return MessageVerificationMacContent(
                    mac,
                    keys,
                    RelationDefaultContent(
                            RelationType.REFERENCE,
                            tid
                    )
            )
        }
    }
}
