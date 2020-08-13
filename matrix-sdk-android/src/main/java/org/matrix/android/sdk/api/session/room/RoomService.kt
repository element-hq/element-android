/*
 * Copyright 2019 New Vector Ltd
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
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to get rooms. It's implemented at the session level.
 */
interface RoomService {

    /**
     * Create a room asynchronously
     */
    fun createRoom(createRoomParams: CreateRoomParams,
                   callback: MatrixCallback<String>): Cancelable

    /**
     * Join a room by id
     * @param roomIdOrAlias the roomId or the room alias of the room to join
     * @param reason optional reason for joining the room
     * @param viaServers the servers to attempt to join the room through. One of the servers must be participating in the room.
     */
    fun joinRoom(roomIdOrAlias: String,
                 reason: String? = null,
                 viaServers: List<String> = emptyList(),
                 callback: MatrixCallback<Unit>): Cancelable

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
    fun getRoomSummaries(queryParams: RoomSummaryQueryParams): List<RoomSummary>

    /**
     * Get a live list of room summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of List[RoomSummary]
     */
    fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>>

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
    fun onRoomDisplayed(roomId: String): Cancelable

    /**
     * Mark all rooms as read
     */
    fun markAllAsRead(roomIds: List<String>,
                      callback: MatrixCallback<Unit>): Cancelable

    /**
     * Resolve a room alias to a room ID.
     */
    fun getRoomIdByAlias(roomAlias: String,
                         searchOnServer: Boolean,
                         callback: MatrixCallback<Optional<String>>): Cancelable

    /**
     * Return a live data of all local changes membership that happened since the session has been opened.
     * It allows you to track this in your client to known what is currently being processed by the SDK.
     * It won't know anything about change being done in other client.
     * Keys are roomId or roomAlias, depending of what you used as parameter for the join/leave action
     */
    fun getChangeMembershipsLive(): LiveData<Map<String, ChangeMembershipState>>

    fun getExistingDirectRoomWithUser(otherUserId: String): Room?
}
