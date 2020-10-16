/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
