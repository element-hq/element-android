/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SearchResponseRoomEvents(
        /**
         * List of results in the requested order.
         */
        @Json(name = "results")
        val results: List<SearchResponseItem>? = null,
        @Json(name = "count")
        val count: Int? = null,
        /**
         * List of words which should be highlighted, useful for stemming which may change the query terms.
         */
        @Json(name = "highlights")
        val highlights: List<String>? = null,
        /**
         * Token that can be used to get the next batch of results, by passing as the next_batch parameter to the next call.
         * If this field is absent, there are no more results.
         */
        @Json(name = "next_batch")
        val nextBatch: String? = null
)
