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

package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.membership.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import im.vector.matrix.android.internal.session.sync.model.RoomSyncSummary
import im.vector.matrix.android.internal.session.sync.model.RoomSyncUnreadNotifications
import io.realm.Realm
import io.realm.kotlin.createObject
import javax.inject.Inject

internal class RoomSummaryUpdater @Inject constructor(private val credentials: Credentials,
                                                      private val roomDisplayNameResolver: RoomDisplayNameResolver,
                                                      private val roomAvatarResolver: RoomAvatarResolver) {

    // TODO: maybe allow user of SDK to give that list
    private val PREVIEWABLE_TYPES = listOf(
            EventType.MESSAGE,
            EventType.STATE_ROOM_NAME,
            EventType.STATE_ROOM_TOPIC,
            EventType.STATE_ROOM_MEMBER,
            EventType.STATE_HISTORY_VISIBILITY,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER,
            EventType.ENCRYPTED,
            EventType.ENCRYPTION,
            EventType.STATE_ROOM_THIRD_PARTY_INVITE,
            EventType.STICKER,
            EventType.STATE_ROOM_CREATE
    )

    fun update(realm: Realm,
               roomId: String,
               membership: Membership? = null,
               roomSummary: RoomSyncSummary? = null,
               unreadNotifications: RoomSyncUnreadNotifications? = null) {

        val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)

        if (roomSummary != null) {
            if (roomSummary.heroes.isNotEmpty()) {
                roomSummaryEntity.heroes.clear()
                roomSummaryEntity.heroes.addAll(roomSummary.heroes)
            }
            if (roomSummary.invitedMembersCount != null) {
                roomSummaryEntity.invitedMembersCount = roomSummary.invitedMembersCount
            }
            if (roomSummary.joinedMembersCount != null) {
                roomSummaryEntity.joinedMembersCount = roomSummary.joinedMembersCount
            }
        }
        if (unreadNotifications?.highlightCount != null) {
            roomSummaryEntity.highlightCount = unreadNotifications.highlightCount
        }
        if (unreadNotifications?.notificationCount != null) {
            roomSummaryEntity.notificationCount = unreadNotifications.notificationCount
        }
        if (membership != null) {
            roomSummaryEntity.membership = membership
        }

        val lastEvent = EventEntity.latestEvent(realm, roomId, includedTypes = PREVIEWABLE_TYPES)
        val lastTopicEvent = EventEntity.where(realm, roomId, EventType.STATE_ROOM_TOPIC).prev()?.asDomain()
        val otherRoomMembers = RoomMembers(realm, roomId).getLoaded().filterKeys { it != credentials.userId }
        roomSummaryEntity.displayName = roomDisplayNameResolver.resolve(roomId).toString()
        roomSummaryEntity.avatarUrl = roomAvatarResolver.resolve(roomId)
        roomSummaryEntity.topic = lastTopicEvent?.content.toModel<RoomTopicContent>()?.topic
        roomSummaryEntity.latestEvent = lastEvent
        roomSummaryEntity.otherMemberIds.clear()
        roomSummaryEntity.otherMemberIds.addAll(otherRoomMembers.keys)
    }
}