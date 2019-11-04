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
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort

/**
 * This class is an helper around STATE_ROOM_MEMBER events.
 * It allows to get the live membership of a user.
 */

internal class RoomMembers(private val realm: Realm,
                           private val roomId: String
) {

    private val roomSummary: RoomSummaryEntity? by lazy {
        RoomSummaryEntity.where(realm, roomId).findFirst()
    }

    fun getStateEvent(userId: String): EventEntity? {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
                .equalTo(EventEntityFields.STATE_KEY, userId)
                .findFirst()
    }

    fun get(userId: String): RoomMember? {
        return getStateEvent(userId)
                ?.let {
                    it.asDomain().content?.toModel<RoomMember>()
                }
    }

    fun isUniqueDisplayName(displayName: String?): Boolean {
        if (displayName.isNullOrEmpty()) {
            return true
        }
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .contains(EventEntityFields.CONTENT, "\"displayname\":\"$displayName\"")
                .distinct(EventEntityFields.STATE_KEY)
                .findAll()
                .size == 1
    }

    fun queryRoomMembersEvent(): RealmQuery<EventEntity> {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
                .isNotNull(EventEntityFields.STATE_KEY)
                .distinct(EventEntityFields.STATE_KEY)
                .isNotNull(EventEntityFields.CONTENT)
    }

    fun queryJoinedRoomMembersEvent(): RealmQuery<EventEntity> {
        return queryRoomMembersEvent().contains(EventEntityFields.CONTENT, "\"membership\":\"join\"")
    }

    fun queryInvitedRoomMembersEvent(): RealmQuery<EventEntity> {
        return queryRoomMembersEvent().contains(EventEntityFields.CONTENT, "\"membership\":\"invite\"")
    }

    fun queryRoomMemberEvent(userId: String): RealmQuery<EventEntity> {
        return queryRoomMembersEvent()
                .equalTo(EventEntityFields.STATE_KEY, userId)
    }

    fun getNumberOfJoinedMembers(): Int {
        return roomSummary?.joinedMembersCount
               ?: queryJoinedRoomMembersEvent().findAll().size
    }

    fun getNumberOfInvitedMembers(): Int {
        return roomSummary?.invitedMembersCount
               ?: queryInvitedRoomMembersEvent().findAll().size
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
        return getRoomMemberIdsFiltered { it.membership == Membership.JOIN || it.membership == Membership.INVITE }
    }

    /**
     * Return all the roomMembers ids which are joined to the room
     *
     * @return a roomMember id list of joined members.
     */
    fun getJoinedRoomMemberIds(): List<String> {
        return getRoomMemberIdsFiltered { it.membership == Membership.JOIN }
    }

    /* ==========================================================================================
     * Private
     * ========================================================================================== */

    private fun getRoomMemberIdsFiltered(predicate: (RoomMember) -> Boolean): List<String> {
        return RoomMembers(realm, roomId)
                .queryRoomMembersEvent()
                .findAll()
                .map { it.asDomain() }
                .associateBy { it.stateKey!! }
                .filterValues { predicate(it.content.toModel<RoomMember>()!!) }
                .keys
                .toList()
    }
}
