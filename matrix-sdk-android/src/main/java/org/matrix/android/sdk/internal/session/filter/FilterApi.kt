/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 * Copyright 2018 Matthias Kesler
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface FilterApi {

    /**
     * Upload FilterBody to get a filter_id which can be used for /sync requests.
     *
     * @param userId the user id
     * @param body the Json representation of a FilterBody object
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter")
    suspend fun uploadFilter(
            @Path("userId") userId: String,
            @Body body: Filter
    ): FilterResponse

    /**
     * Gets a filter with a given filterId from the homeserver.
     *
     * @param userId the user id
     * @param filterId the filterID
     * @return Filter
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter/{filterId}")
    suspend fun getFilterById(
            @Path("userId") userId: String,
            @Path("filterId") filterId: String
    ): Filter
}
