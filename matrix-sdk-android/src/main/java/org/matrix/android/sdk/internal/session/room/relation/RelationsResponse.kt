/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.room.relation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

@JsonClass(generateAdapter = true)
internal data class RelationsResponse(
        @Json(name = "chunk") val chunks: List<Event>,
        @Json(name = "original_event") val originalEvent: Event?,
        @Json(name = "next_batch") val nextBatch: String?,
        @Json(name = "prev_batch") val prevBatch: String?
)
