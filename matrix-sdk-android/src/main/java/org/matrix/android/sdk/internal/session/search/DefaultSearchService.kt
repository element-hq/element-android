/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search

import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.api.session.search.SearchService
import javax.inject.Inject

internal class DefaultSearchService @Inject constructor(
        private val searchTask: SearchTask
) : SearchService {

    override suspend fun search(
            searchTerm: String,
            roomId: String,
            nextBatch: String?,
            orderByRecent: Boolean,
            limit: Int,
            beforeLimit: Int,
            afterLimit: Int,
            includeProfile: Boolean
    ): SearchResult {
        return searchTask.execute(
                SearchTask.Params(
                        searchTerm = searchTerm,
                        roomId = roomId,
                        nextBatch = nextBatch,
                        orderByRecent = orderByRecent,
                        limit = limit,
                        beforeLimit = beforeLimit,
                        afterLimit = afterLimit,
                        includeProfile = includeProfile
                )
        )
    }
}
