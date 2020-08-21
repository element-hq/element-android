/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.session.room.members

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.Cancelable

/**
 * This interface defines methods to handling membership. It's implemented at the room level.
 */
interface MembershipService {

    /**
     * This methods load all room members if it was done yet.
     * @return a [Cancelable]
     */
    fun loadRoomMembersIfNeeded(matrixCallback: MatrixCallback<Unit>): Cancelable

    /**
     * Return the roomMember with userId or null.
     * @param userId the userId param to look for
     *
     * @return the roomMember with userId or null
     */
    fun getRoomMember(userId: String): RoomMemberSummary?

    /**
     * Return all the roomMembers of the room with params
     * @param queryParams the params to query for
     * @return a roomMember list.
     */
    fun getRoomMembers(queryParams: RoomMemberQueryParams): List<RoomMemberSummary>

    /**
     * Return all the roomMembers of the room filtered by memberships
     * @param queryParams the params to query for
     * @return a [LiveData] of roomMember list.
     */
    fun getRoomMembersLive(queryParams: RoomMemberQueryParams): LiveData<List<RoomMemberSummary>>

    fun getNumberOfJoinedMembers(): Int

    /**
     * Invite a user in the room
     */
    fun invite(userId: String,
               reason: String? = null,
               callback: MatrixCallback<Unit>): Cancelable

    /**
     * Invite a user with email or phone number in the room
     */
    fun invite3pid(threePid: ThreePid,
                   callback: MatrixCallback<Unit>): Cancelable

    /**
     * Ban a user from the room
     */
    fun ban(userId: String,
            reason: String? = null,
            callback: MatrixCallback<Unit>): Cancelable

    /**
     * Unban a user from the room
     */
    fun unban(userId: String,
              reason: String? = null,
              callback: MatrixCallback<Unit>): Cancelable

    /**
     * Kick a user from the room
     */
    fun kick(userId: String,
             reason: String? = null,
             callback: MatrixCallback<Unit>): Cancelable

    /**
     * Join the room, or accept an invitation.
     */
    fun join(reason: String? = null,
             viaServers: List<String> = emptyList(),
             callback: MatrixCallback<Unit>): Cancelable

    /**
     * Leave the room, or reject an invitation.
     */
    fun leave(reason: String? = null,
              callback: MatrixCallback<Unit>): Cancelable
}
