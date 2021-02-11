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

package org.matrix.android.sdk.api.session.space

import android.net.Uri
import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.internal.session.space.peeking.SpacePeekResult

typealias SpaceSummaryQueryParams = RoomSummaryQueryParams

interface SpaceService {

    /**
     * Create a room asynchronously
     */
    suspend fun createSpace(params: CreateSpaceParams): String

    /**
     * Just a shortcut for space creation for ease of use
     */
    suspend fun createSpace(name: String, topic: String?, avatarUri: Uri?, isPublic: Boolean): String

    /**
     * Get a space from a roomId
     * @param roomId the roomId to look for.
     * @return a room with roomId or null if room type is not space
     */
    fun getSpace(spaceId: String): Space?

    /**
     * Try to resolve (peek) rooms and subspace in this space.
     * Use this call get preview of children of this space, particularly useful to get a
     * preview of rooms that you did not join yet.
     */
    suspend fun peekSpace(spaceId: String): SpacePeekResult

    /**
     * Get's information of a space by querying the server
     */
    suspend fun querySpaceChildren(spaceId: String): Pair<RoomSummary, List<SpaceChildInfo>>

    /**
     * Get a live list of space summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of List[SpaceSummary]
     */
    fun getSpaceSummariesLive(queryParams: SpaceSummaryQueryParams): LiveData<List<SpaceSummary>>

    fun getSpaceSummaries(spaceSummaryQueryParams: SpaceSummaryQueryParams): List<SpaceSummary>

    data class ChildAutoJoinInfo(
            val roomIdOrAlias: String,
            val viaServers: List<String> = emptyList()
    )

    sealed class JoinSpaceResult {
        object Success : JoinSpaceResult()
        data class Fail(val error: Throwable) : JoinSpaceResult()

        /** Success fully joined the space, but failed to join all or some of it's rooms */
        data class PartialSuccess(val failedRooms: Map<String, Throwable>) : JoinSpaceResult()

        fun isSuccess() = this is Success || this is PartialSuccess
    }

    suspend fun joinSpace(spaceIdOrAlias: String,
                          reason: String? = null,
                          viaServers: List<String> = emptyList()): JoinSpaceResult

    suspend fun rejectInvite(spaceId: String, reason: String?)
}
