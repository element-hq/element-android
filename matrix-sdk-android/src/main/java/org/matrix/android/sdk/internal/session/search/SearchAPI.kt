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
    suspend fun search(@Query("next_batch") nextBatch: String?,
                       @Body body: SearchRequestBody): SearchResponse
}
