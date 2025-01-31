/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

/**
 * Class representing the org.matrix.msc3381.poll.end event content.
 */
@JsonClass(generateAdapter = true)
data class MessageEndPollContent(
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent? = null
)
