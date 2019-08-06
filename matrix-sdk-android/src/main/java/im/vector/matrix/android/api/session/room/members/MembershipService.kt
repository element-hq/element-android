/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.members

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.util.Cancelable

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
    fun getRoomMember(userId: String): RoomMember?

    /**
     * Return all the roomMembers ids of the room
     *
     * @return a [LiveData] of roomMember list.
     */
    fun getRoomMemberIdsLive(): LiveData<List<String>>

    fun getNumberOfJoinedMembers(): Int

    /**
     * Invite a user in the room
     */
    fun invite(userId: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Join the room, or accept an invitation.
     */

    fun join(viaServers: List<String> = emptyList(), callback: MatrixCallback<Unit>): Cancelable

    /**
     * Leave the room, or reject an invitation.
     */
    fun leave(callback: MatrixCallback<Unit>): Cancelable

}