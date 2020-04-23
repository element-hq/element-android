/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.membership

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.sqldelight.session.*

/**
 * This class is an helper around STATE_ROOM_MEMBER events.
 * It allows to get the live membership of a user.
 */

internal class RoomMemberHelper(private val sessionDatabase: SessionDatabase,
                                private val roomId: String
) {

    private val roomSummary: RoomSummaryWithTimeline? by lazy {
        sessionDatabase.roomSummaryQueries.get(roomId).executeAsOneOrNull()
    }

    fun getLastStateEvent(userId: String): EventEntity? {
        return sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = EventType.STATE_ROOM_MEMBER, stateKey = userId).executeAsOneOrNull()
    }

    fun getDisplayName(userId: String): String? {
        return sessionDatabase.roomMemberSummaryQueries.getDisplayName(userId = userId, roomId = roomId).executeAsOneOrNull()?.display_name
    }


    fun isUniqueDisplayName(displayName: String?): Boolean {
        if (displayName.isNullOrEmpty()) {
            return true
        }
        return sessionDatabase.roomMemberSummaryQueries
                .countMembersWithNameInRoom(displayName = displayName, roomId = roomId)
                .executeAsOne() == 1L
    }

    fun getJoinedRoomMembersEvent(): List<SimpleRoomMemberSummary> {
        return sessionDatabase.roomMemberSummaryQueries.getAllFromRoom(
                roomId = roomId,
                memberships = listOf(Memberships.JOIN),
                excludedIds = emptyList()
        ).executeAsList()
    }

    fun getInvitedRoomMembersEvent(): List<SimpleRoomMemberSummary> {
        return sessionDatabase.roomMemberSummaryQueries.getAllFromRoom(
                roomId = roomId,
                memberships = listOf(Memberships.INVITE),
                excludedIds = emptyList()
        ).executeAsList()
    }

    fun getActiveRoomMembersEvent(excludedIds: List<String> = emptyList()): List<SimpleRoomMemberSummary> {
        return sessionDatabase.roomMemberSummaryQueries.getAllFromRoom(
                roomId = roomId,
                memberships = listOf(Memberships.JOIN, Memberships.INVITE),
                excludedIds = excludedIds
        ).executeAsList()
    }

    fun getNumberOfJoinedMembers(): Int {
        return roomSummary?.joined_members_count
                ?: sessionDatabase.roomMemberSummaryQueries.getNumberOfMembersInRoom(
                        roomId = roomId,
                        memberships = listOf(Memberships.JOIN)
                ).executeAsOne().toInt()
    }

    fun getNumberOfInvitedMembers(): Int {
        return roomSummary?.invited_members_count
                ?: sessionDatabase.roomMemberSummaryQueries.getNumberOfMembersInRoom(
                        roomId = roomId,
                        memberships = listOf(Memberships.INVITE)
                ).executeAsOne().toInt()
    }

    fun getNumberOfMembers(): Int {
        return getNumberOfJoinedMembers() + getNumberOfInvitedMembers()
    }

    /**
     * Return all the roomMembers ids which are joined or invited to the room
     *
     * @return a roomMember id list of joined or invited members.
     */
    fun getActiveRoomMemberIds(): List<String> {
        return sessionDatabase.roomMemberSummaryQueries.getAllUserIdFromRoom(
                roomId = roomId,
                memberships = listOf(Memberships.INVITE, Memberships.JOIN),
                excludedIds = emptyList()
        ).executeAsList()
    }

    /**
     * Return all the roomMembers ids which are joined to the room
     *
     * @return a roomMember id list of joined members.
     */
    fun getJoinedRoomMemberIds(): List<String> {
        return sessionDatabase.roomMemberSummaryQueries.getAllUserIdFromRoom(
                roomId = roomId,
                memberships = listOf(Memberships.JOIN),
                excludedIds = emptyList()
        ).executeAsList()
    }
}
