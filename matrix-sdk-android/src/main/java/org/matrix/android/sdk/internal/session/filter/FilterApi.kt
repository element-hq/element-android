/*
 * Copyright 2018 Matthias Kesler
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
package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface FilterApi {

    /**
     * Upload FilterBody to get a filter_id which can be used for /sync requests
     *
     * @param userId the user id
     * @param body   the Json representation of a FilterBody object
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter")
    suspend fun uploadFilter(@Path("userId") userId: String,
                             @Body body: Filter): FilterResponse

    /**
     * Gets a filter with a given filterId from the homeserver
     *
     * @param userId   the user id
     * @param filterId the filterID
     * @return Filter
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter/{filterId}")
    suspend fun getFilterById(@Path("userId") userId: String,
                              @Path("filterId") filterId: String): Filter
}
