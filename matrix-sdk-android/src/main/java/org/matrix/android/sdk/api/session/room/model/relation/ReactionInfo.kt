/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.relation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReactionInfo(
        @Json(name = "rel_type") override val type: String?,
        @Json(name = "event_id") override val eventId: String,
        @Json(name = "key") val key: String,
        // always null for reaction
        @Json(name = "m.in_reply_to") override val inReplyTo: ReplyToContent? = null,
        @Json(name = "option") override val option: Int? = null,
        @Json(name = "is_falling_back") override val isFallingBack: Boolean? = null
) : RelationContent
