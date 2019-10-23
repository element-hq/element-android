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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.isEventRead
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.membership.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import im.vector.matrix.android.internal.session.sync.model.RoomSyncSummary
import im.vector.matrix.android.internal.session.sync.model.RoomSyncUnreadNotifications
import io.realm.Realm
import io.realm.kotlin.createObject
import javax.inject.Inject

internal class RoomSummaryUpdater @Inject constructor(@UserId private val userId: String,
                                                      private val roomDisplayNameResolver: RoomDisplayNameResolver,
                                                      private val roomAvatarResolver: RoomAvatarResolver,
                                                      private val monarchy: Monarchy) {

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
        val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

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
        roomSummaryEntity.highlightCount = unreadNotifications?.highlightCount ?: 0
        roomSummaryEntity.notificationCount = unreadNotifications?.notificationCount ?: 0

        if (membership != null) {
            roomSummaryEntity.membership = membership
        }

        val latestPreviewableEvent = TimelineEventEntity.latestEvent(realm, roomId, includesSending = true, filterTypes = PREVIEWABLE_TYPES)
        val lastTopicEvent = EventEntity.where(realm, roomId, EventType.STATE_ROOM_TOPIC).prev()?.asDomain()

        roomSummaryEntity.hasUnreadMessages = roomSummaryEntity.notificationCount > 0
                // avoid this call if we are sure there are unread events
                || !isEventRead(monarchy, userId, roomId, latestPreviewableEvent?.eventId)

        val otherRoomMembers = RoomMembers(realm, roomId)
                .queryRoomMembersEvent()
                .notEqualTo(EventEntityFields.STATE_KEY, userId)
                .findAll()
                .asSequence()
                .map { it.stateKey }

        roomSummaryEntity.displayName = roomDisplayNameResolver.resolve(roomId).toString()
        roomSummaryEntity.avatarUrl = roomAvatarResolver.resolve(roomId)
        roomSummaryEntity.topic = lastTopicEvent?.content.toModel<RoomTopicContent>()?.topic
        roomSummaryEntity.latestPreviewableEvent = latestPreviewableEvent
        roomSummaryEntity.otherMemberIds.clear()
        roomSummaryEntity.otherMemberIds.addAll(otherRoomMembers)
    }
}
