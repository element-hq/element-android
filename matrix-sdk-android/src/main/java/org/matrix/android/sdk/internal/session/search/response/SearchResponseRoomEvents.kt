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
