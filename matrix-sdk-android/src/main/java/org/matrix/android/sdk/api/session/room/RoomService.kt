/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.identity.model.SignInvitationResult
import org.matrix.android.sdk.api.session.room.alias.RoomAliasDescription
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to get rooms. It's implemented at the session level.
 */
interface RoomService {

    /**
     * Create a room asynchronously
     */
    suspend fun createRoom(createRoomParams: CreateRoomParams): String

    /**
     * Create a direct room asynchronously. This is a facility method to create a direct room with the necessary parameters
     */
    suspend fun createDirectRoom(otherUserId: String): String {
        return createRoom(
                CreateRoomParams()
                        .apply {
                            invitedUserIds.add(otherUserId)
                            setDirectMessage()
                            enableEncryptionIfInvitedUsersSupportIt = true
                        }
        )
    }

    /**
     * Join a room by id
     * @param roomIdOrAlias the roomId or the room alias of the room to join
     * @param reason optional reason for joining the room
     * @param viaServers the servers to attempt to join the room through. One of the servers must be participating in the room.
     */
    suspend fun joinRoom(roomIdOrAlias: String,
                         reason: String? = null,
                         viaServers: List<String> = emptyList())

    /**
     * @param roomId the roomId of the room to join
     * @param reason optional reason for joining the room
     * @param thirdPartySigned A signature of an m.third_party_invite token to prove that this user owns a third party identity
     * which has been invited to the room.
     */
    suspend fun joinRoom(
            roomId: String,
            reason: String? = null,
            thirdPartySigned: SignInvitationResult
    )

    /**
     * Leave the room, or reject an invitation.
     * @param roomId the roomId of the room to leave
     * @param reason optional reason for leaving the room
     */
    suspend fun leaveRoom(roomId: String, reason: String? = null)

    /**
     * Get a room from a roomId
     * @param roomId the roomId to look for.
     * @return a room with roomId or null
     */
    fun getRoom(roomId: String): Room?

    /**
     * Get a roomSummary from a roomId or a room alias
     * @param roomIdOrAlias the roomId or the alias of a room to look for.
     * @return a matching room summary or null
     */
    fun getRoomSummary(roomIdOrAlias: String): RoomSummary?

    /**
     * Get a snapshot list of room summaries.
     * @return the immutable list of [RoomSummary]
     */
    fun getRoomSummaries(queryParams: RoomSummaryQueryParams,
                         sortOrder: RoomSortOrder = RoomSortOrder.NONE): List<RoomSummary>

    /**
     * Get a live list of room summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of List[RoomSummary]
     */
    fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                             sortOrder: RoomSortOrder = RoomSortOrder.ACTIVITY): LiveData<List<RoomSummary>>

    /**
     * Get a snapshot list of Breadcrumbs
     * @param queryParams parameters to query the room summaries. It can be use to keep only joined rooms, for instance.
     * @return the immutable list of [RoomSummary]
     */
    fun getBreadcrumbs(queryParams: RoomSummaryQueryParams): List<RoomSummary>

    /**
     * Get a live list of Breadcrumbs
     * @param queryParams parameters to query the room summaries. It can be use to keep only joined rooms, for instance.
     * @return the [LiveData] of [RoomSummary]
     */
    fun getBreadcrumbsLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>>

    /**
     * Inform the Matrix SDK that a room is displayed.
     * The SDK will update the breadcrumbs in the user account data
     */
    suspend fun onRoomDisplayed(roomId: String)

    /**
     * Mark all rooms as read
     */
    suspend fun markAllAsRead(roomIds: List<String>)

    /**
     * Resolve a room alias to a room ID.
     */
    suspend fun getRoomIdByAlias(roomAlias: String,
                                 searchOnServer: Boolean): Optional<RoomAliasDescription>

    /**
     * Delete a room alias
     */
    suspend fun deleteRoomAlias(roomAlias: String)

    /**
     * Return the current local changes membership for the given room.
     * see [getChangeMembershipsLive] for more details.
     */
    fun getChangeMemberships(roomIdOrAlias: String): ChangeMembershipState

    /**
     * Return a live data of all local changes membership that happened since the session has been opened.
     * It allows you to track this in your client to known what is currently being processed by the SDK.
     * It won't know anything about change being done in other client.
     * Keys are roomId or roomAlias, depending of what you used as parameter for the join/leave action
     */
    fun getChangeMembershipsLive(): LiveData<Map<String, ChangeMembershipState>>

    /**
     * Return the roomId of an existing DM with the other user, or null if such room does not exist
     * A room is a DM if:
     *  - it is listed in the `m.direct` account data
     *  - the current user has joined the room
     *  - the other user is invited or has joined the room
     *  - it has exactly 2 members
     * Note:
     *  - the returning room can be encrypted or not
     *  - the power level of the users are not taken into account. Normally in a DM, the 2 members are admins of the room
     */
    fun getExistingDirectRoomWithUser(otherUserId: String): String?

    /**
     * Get a room member for the tuple {userId,roomId}
     * @param userId the userId to look for.
     * @param roomId the roomId to look for.
     * @return the room member or null
     */
    fun getRoomMember(userId: String, roomId: String): RoomMemberSummary?

    /**
     * Observe a live room member for the tuple {userId,roomId}
     * @param userId the userId to look for.
     * @param roomId the roomId to look for.
     * @return a LiveData of the optional found room member
     */
    fun getRoomMemberLive(userId: String, roomId: String): LiveData<Optional<RoomMemberSummary>>

    /**
     * Get some state events about a room
     */
    suspend fun getRoomState(roomId: String): List<Event>

    /**
     * Use this if you want to get information from a room that you are not yet in (or invited)
     * It might be possible to get some information on this room if it is public or if guest access is allowed
     * This call will try to gather some information on this room, but it could fail and get nothing more
     */
    suspend fun peekRoom(roomIdOrAlias: String): PeekResult

    /**
     * TODO Doc
     */
    fun getPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                  pagedListConfig: PagedList.Config = defaultPagedListConfig,
                                  sortOrder: RoomSortOrder = RoomSortOrder.ACTIVITY): LiveData<PagedList<RoomSummary>>

    /**
     * TODO Doc
     */
    fun getFilteredPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                          pagedListConfig: PagedList.Config = defaultPagedListConfig,
                                          sortOrder: RoomSortOrder = RoomSortOrder.ACTIVITY): UpdatableLivePageResult

    /**
     * Return a LiveData on the number of rooms
     * @param queryParams parameters to query the room summaries. It can be use to keep only joined rooms, for instance.
     */
    fun getRoomCountLive(queryParams: RoomSummaryQueryParams): LiveData<Int>

    /**
     * TODO Doc
     */
    fun getNotificationCountForRooms(queryParams: RoomSummaryQueryParams): RoomAggregateNotificationCount

    private val defaultPagedListConfig
        get() = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(20)
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .build()

    fun getFlattenRoomSummaryChildrenOf(spaceId: String?, memberships: List<Membership> = Membership.activeMemberships()): List<RoomSummary>

    /**
     * Returns all the children of this space, as LiveData
     */
    fun getFlattenRoomSummaryChildrenOfLive(spaceId: String?,
                                            memberships: List<Membership> = Membership.activeMemberships()): LiveData<List<RoomSummary>>

    /**
     * Refreshes the RoomSummary LatestPreviewContent for the given @param roomId
     * If the roomId is null, all rooms are updated
     *
     * This is useful for refreshing summary content with encrypted messages after receiving new room keys
     */
    fun refreshJoinedRoomSummaryPreviews(roomId: String?)
}
