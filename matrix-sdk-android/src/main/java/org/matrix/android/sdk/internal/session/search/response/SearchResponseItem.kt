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
import org.matrix.android.sdk.api.session.events.model.Event

@JsonClass(generateAdapter = true)
internal data class SearchResponseItem(
        /**
         *  A number that describes how closely this result matches the search. Higher is closer.
         */
        @Json(name = "rank")
        val rank: Double? = null,

        /**
         * The event that matched.
         */
        @Json(name = "result")
        val event: Event,

        /**
         * Context for result, if requested.
         */
        @Json(name = "context")
        val context: SearchResponseEventContext? = null
)
