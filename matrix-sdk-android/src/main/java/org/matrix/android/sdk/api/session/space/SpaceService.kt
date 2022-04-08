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
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.peeking.SpacePeekResult

typealias SpaceSummaryQueryParams = RoomSummaryQueryParams

interface SpaceService {

    /**
     * Create a space asynchronously
     * @return the spaceId of the created space
     */
    suspend fun createSpace(params: CreateSpaceParams): String

    /**
     * Just a shortcut for space creation for ease of use
     */
    suspend fun createSpace(name: String,
                            topic: String?,
                            avatarUri: Uri?,
                            isPublic: Boolean,
                            roomAliasLocalPart: String? = null): String

    /**
     * Get a space from a roomId
     * @param spaceId the roomId to look for.
     * @return a space with spaceId or null if room type is not space
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
     * @param suggestedOnly If true, return only child events and rooms where the m.space.child event has suggested: true.
     * @param limit a client-defined limit to the maximum number of rooms to return per page. Must be a non-negative integer.
     * @param from: Optional. Pagination token given to retrieve the next set of rooms. Note that if a pagination token is provided,
     * then the parameters given for suggested_only and max_depth must be the same.
     */
    suspend fun querySpaceChildren(spaceId: String,
                                   suggestedOnly: Boolean? = null,
                                   limit: Int? = null,
                                   from: String? = null,
                                   // when paginating, pass back the m.space.child state events
                                   knownStateList: List<Event>? = null): SpaceHierarchyData

    /**
     * Get a live list of space summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of List[SpaceSummary]
     */
    fun getSpaceSummariesLive(queryParams: SpaceSummaryQueryParams,
                              sortOrder: RoomSortOrder = RoomSortOrder.NONE): LiveData<List<RoomSummary>>

    fun getSpaceSummaries(spaceSummaryQueryParams: SpaceSummaryQueryParams,
                          sortOrder: RoomSortOrder = RoomSortOrder.NONE): List<RoomSummary>

    suspend fun joinSpace(spaceIdOrAlias: String,
                          reason: String? = null,
                          viaServers: List<String> = emptyList()): JoinSpaceResult

    suspend fun rejectInvite(spaceId: String, reason: String?)

    /**
     * Leave the space, or reject an invitation.
     * @param spaceId the spaceId of the space to leave
     * @param reason optional reason for leaving the space
     */
    suspend fun leaveSpace(spaceId: String, reason: String? = null)

//    fun getSpaceParentsOfRoom(roomId: String) : List<SpaceSummary>

    /**
     * Let this room declare that it has a parent.
     * @param canonical true if it should be the main parent of this room
     * In practice, well behaved rooms should only have one canonical parent, but given this is not enforced:
     * if multiple are present the client should select the one with the lowest room ID, as determined via a lexicographic utf-8 ordering.
     */
    suspend fun setSpaceParent(childRoomId: String, parentSpaceId: String, canonical: Boolean, viaServers: List<String>)

    suspend fun removeSpaceParent(childRoomId: String, parentSpaceId: String)

    fun getRootSpaceSummaries(): List<RoomSummary>
}
