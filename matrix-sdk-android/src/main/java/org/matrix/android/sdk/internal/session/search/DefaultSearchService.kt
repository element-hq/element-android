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

package org.matrix.android.sdk.internal.session.search

import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.api.session.search.SearchService
import javax.inject.Inject

internal class DefaultSearchService @Inject constructor(
        private val searchTask: SearchTask
) : SearchService {

    override suspend fun search(searchTerm: String,
                                roomId: String,
                                nextBatch: String?,
                                orderByRecent: Boolean,
                                limit: Int,
                                beforeLimit: Int,
                                afterLimit: Int,
                                includeProfile: Boolean): SearchResult {
        return searchTask.execute(SearchTask.Params(
                searchTerm = searchTerm,
                roomId = roomId,
                nextBatch = nextBatch,
                orderByRecent = orderByRecent,
                limit = limit,
                beforeLimit = beforeLimit,
                afterLimit = afterLimit,
                includeProfile = includeProfile
        ))
    }
}
