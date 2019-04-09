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

package im.vector.matrix.android.internal.session.room.members

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

internal class RoomMembers(private val realm: Realm,
                           private val roomId: String
) {

    private val roomSummary: RoomSummaryEntity? by lazy {
        RoomSummaryEntity.where(realm, roomId).findFirst()
    }

    fun get(userId: String): RoomMember? {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
                .equalTo(EventEntityFields.STATE_KEY, userId)
                .findFirst()
                ?.let {
                    it.asDomain().content?.toModel<RoomMember>()
                }
    }

    fun queryRoomMembersEvent(): RealmQuery<EventEntity> {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
                .distinct(EventEntityFields.STATE_KEY)
                .isNotNull(EventEntityFields.CONTENT)
    }

    fun queryRoomMemberEvent(userId: String): RealmQuery<EventEntity> {
        return queryRoomMembersEvent()
                .equalTo(EventEntityFields.STATE_KEY, userId)
    }

    fun getLoaded(): Map<String, RoomMember> {
        return queryRoomMembersEvent()
                .findAll()
                .map { it.asDomain() }
                .associateBy { it.stateKey!! }
                .mapValues { it.value.content.toModel<RoomMember>()!! }
    }

    fun getNumberOfJoinedMembers(): Int {
        return roomSummary?.joinedMembersCount
               ?: getLoaded().filterValues { it.membership == Membership.JOIN }.size
    }

    fun getNumberOfInvitedMembers(): Int {
        return roomSummary?.invitedMembersCount
               ?: getLoaded().filterValues { it.membership == Membership.INVITE }.size
    }

    fun getNumberOfMembers(): Int {
        return getNumberOfJoinedMembers() + getNumberOfInvitedMembers()
    }


}