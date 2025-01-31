/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.search

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.MatrixItem

/**
 * Domain class to represent the response of a search request in a room.
 */
data class SearchResult(
        /**
         * Token that can be used to get the next batch of results, by passing as the next_batch parameter to the next call.
         * If this field is null, there are no more results.
         */
        val nextBatch: String? = null,
        /**
         *  List of words which should be highlighted, useful for stemming which may change the query terms.
         */
        val highlights: List<String>? = null,
        /**
         * List of results in the requested order.
         */
        val results: List<EventAndSender>? = null
)

data class EventAndSender(
        val event: Event,
        val sender: MatrixItem.UserItem?
)
