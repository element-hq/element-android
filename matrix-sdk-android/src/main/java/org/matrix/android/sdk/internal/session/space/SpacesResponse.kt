/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.space

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SpacesResponse(
        /** Its presence indicates that there are more results to return. */
        @Json(name = "next_batch") val nextBatch: String? = null,
        /** Rooms information like name/avatar/type ... */
        @Json(name = "rooms") val rooms: List<SpaceChildSummaryResponse>? = null
)
