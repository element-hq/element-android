/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SpaceApi {

    /**
     * @param spaceId the space Id
     * @param suggestedOnly Optional. If true, return only child events and rooms where the m.space.child event has suggested: true.
     * @param limit Optional: a client-defined limit to the maximum number of rooms to return per page. Must be a non-negative integer.
     * @param maxDepth Optional: The maximum depth in the tree (from the root room) to return.
     * The deepest depth returned will not include children events. Defaults to no-limit. Must be a non-negative integer.
     * @param from Optional. Pagination token given to retrieve the next set of rooms.
     * Note that if a pagination token is provided, then the parameters given for suggested_only and max_depth must be the same.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_V1 + "rooms/{roomId}/hierarchy")
    suspend fun getSpaceHierarchy(
            @Path("roomId") spaceId: String,
            @Query("suggested_only") suggestedOnly: Boolean?,
            @Query("limit") limit: Int?,
            @Query("max_depth") maxDepth: Int?,
            @Query("from") from: String?
    ): SpacesResponse

    /**
     * Unstable version of [getSpaceHierarchy].
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2946/rooms/{roomId}/hierarchy")
    suspend fun getSpaceHierarchyUnstable(
            @Path("roomId") spaceId: String,
            @Query("suggested_only") suggestedOnly: Boolean?,
            @Query("limit") limit: Int?,
            @Query("max_depth") maxDepth: Int?,
            @Query("from") from: String?
    ): SpacesResponse
}
