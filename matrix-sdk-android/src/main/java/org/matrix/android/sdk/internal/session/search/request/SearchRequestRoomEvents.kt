/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SearchRequestRoomEvents(
        /**
         * Required. The string to search events for.
         */
        @Json(name = "search_term")
        val searchTerm: String,

        /**
         * The keys to search. Defaults to all. One of: ["content.body", "content.name", "content.topic"]
         */
        @Json(name = "keys")
        val keys: Any? = null,

        /**
         * This takes a filter.
         */
        @Json(name = "filter")
        val filter: SearchRequestFilter? = null,

        /**
         * The order in which to search for results. By default, this is "rank". One of: ["recent", "rank"]
         */
        @Json(name = "order_by")
        val orderBy: SearchRequestOrder? = null,

        /**
         * Configures whether any context for the events returned are included in the response.
         */
        @Json(name = "event_context")
        val eventContext: SearchRequestEventContext? = null,

        /**
         * Requests the server return the current state for each room returned.
         */
        @Json(name = "include_state")
        val includeState: Boolean? = null

        /**
         * Requests that the server partitions the result set based on the provided list of keys.
         */
        // val groupings: SearchRequestGroupings? = null
)
