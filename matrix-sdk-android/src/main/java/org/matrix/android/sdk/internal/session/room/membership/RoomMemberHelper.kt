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

package org.matrix.android.sdk.internal.session.room.membership

import io.realm.Realm
import io.realm.RealmQuery
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where

/**
 * This class is an helper around STATE_ROOM_MEMBER events.
 * It allows to get the live membership of a user.
 */

internal class RoomMemberHelper(private val realm: Realm,
                                private val roomId: String
) {

    private val roomSummary: RoomSummaryEntity? by lazy {
        RoomSummaryEntity.where(realm, roomId).findFirst()
    }

    fun getLastStateEvent(userId: String): EventEntity? {
        return CurrentStateEventEntity.getOrNull(realm, roomId, userId, EventType.STATE_ROOM_MEMBER)?.root
    }

    fun getLastRoomMember(userId: String): RoomMemberSummaryEntity? {
        return RoomMemberSummaryEntity
                .where(realm, roomId, userId)
                .findFirst()
    }

    fun isUniqueDisplayName(displayName: String?): Boolean {
        if (displayName.isNullOrEmpty()) {
            return true
        }
        return RoomMemberSummaryEntity.where(realm, roomId)
                .equalTo(RoomMemberSummaryEntityFields.DISPLAY_NAME, displayName)
                .findAll()
                .size == 1
    }

    fun queryRoomMembersEvent(): RealmQuery<RoomMemberSummaryEntity> {
        return RoomMemberSummaryEntity.where(realm, roomId)
    }

    fun queryJoinedRoomMembersEvent(): RealmQuery<RoomMemberSummaryEntity> {
        return queryRoomMembersEvent()
                .equalTo(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
    }

    fun queryInvitedRoomMembersEvent(): RealmQuery<RoomMemberSummaryEntity> {
        return queryRoomMembersEvent()
                .equalTo(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, Membership.INVITE.name)
    }

    fun queryLeftRoomMembersEvent(): RealmQuery<RoomMemberSummaryEntity> {
        return queryRoomMembersEvent()
                .equalTo(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, Membership.LEAVE.name)
    }

    fun queryActiveRoomMembersEvent(): RealmQuery<RoomMemberSummaryEntity> {
        return queryRoomMembersEvent()
                .beginGroup()
                .equalTo(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, Membership.INVITE.name)
                .or()
                .equalTo(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
                .endGroup()
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
        return queryActiveRoomMembersEvent().findAll().map { it.userId }
    }

    /**
     * Return all the roomMembers ids which are joined to the room
     *
     * @return a roomMember id list of joined members.
     */
    fun getJoinedRoomMemberIds(): List<String> {
        return queryJoinedRoomMembersEvent().findAll().map { it.userId }
    }
}
