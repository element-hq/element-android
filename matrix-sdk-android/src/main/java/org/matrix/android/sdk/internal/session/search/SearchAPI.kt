/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.search.request.SearchRequestBody
import org.matrix.android.sdk.internal.session.search.response.SearchResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

internal interface SearchAPI {

    /**
     * Performs a full text search across different categories.
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-search
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "search")
    suspend fun search(
            @Query("next_batch") nextBatch: String?,
            @Body body: SearchRequestBody
    ): SearchResponse
}
