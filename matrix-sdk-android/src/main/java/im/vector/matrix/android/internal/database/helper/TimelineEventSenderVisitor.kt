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

package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.next
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import io.realm.RealmList
import io.realm.RealmQuery
import javax.inject.Inject

/**
 * This is an internal cache to avoid querying all the time the room member events
 */
@SessionScope
internal class TimelineEventSenderVisitor @Inject constructor() {

    internal data class Key(
            val roomId: String,
            val stateIndex: Int,
            val senderId: String
    )

    internal class Value(
            var senderAvatar: String? = null,
            var senderName: String? = null,
            var isUniqueDisplayName: Boolean = false,
            var senderMembershipEventId: String? = null
    )

    private val values = HashMap<Key, Value>()

    fun clear() {
        values.clear()
    }

    fun clear(roomId: String, senderId: String) {
        val keysToRemove = values.keys.filter { it.senderId == senderId && it.roomId == roomId }
        keysToRemove.forEach {
            values.remove(it)
        }
    }

    fun visit(timelineEventEntities: List<TimelineEventEntity>) = timelineEventEntities.forEach { visit(it) }

    fun visit(timelineEventEntity: TimelineEventEntity) {
        if (!timelineEventEntity.isValid) {
            return
        }
        val key = Key(
                roomId = timelineEventEntity.roomId,
                stateIndex = timelineEventEntity.root?.stateIndex ?: 0,
                senderId = timelineEventEntity.root?.sender ?: ""
        )
        val result = values.getOrPut(key) {
            timelineEventEntity.computeValue()
        }
        timelineEventEntity.apply {
            this.isUniqueDisplayName = result.isUniqueDisplayName
            this.senderAvatar = result.senderAvatar
            this.senderName = result.senderName
            this.senderMembershipEventId = result.senderMembershipEventId
        }
    }

    private fun RealmList<TimelineEventEntity>.buildQuery(sender: String, isUnlinked: Boolean): RealmQuery<TimelineEventEntity> {
        return where()
                .equalTo(TimelineEventEntityFields.ROOT.STATE_KEY, sender)
                .equalTo(TimelineEventEntityFields.ROOT.TYPE, EventType.STATE_ROOM_MEMBER)
                .equalTo(TimelineEventEntityFields.ROOT.IS_UNLINKED, isUnlinked)
    }

    private fun TimelineEventEntity.computeValue(): Value {
        assertIsManaged()
        val result = Value()
        val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst() ?: return result
        val stateIndex = root?.stateIndex ?: return result
        val senderId = root?.sender ?: return result
        val chunkEntity = chunk?.firstOrNull() ?: return result
        val isUnlinked = chunkEntity.isUnlinked
        var senderMembershipEvent: EventEntity?
        var senderRoomMemberContent: String?
        var senderRoomMemberPrevContent: String?

        if (stateIndex <= 0) {
            senderMembershipEvent = chunkEntity.timelineEvents.buildQuery(senderId, isUnlinked).next(from = stateIndex)?.root
            senderRoomMemberContent = senderMembershipEvent?.prevContent
            senderRoomMemberPrevContent = senderMembershipEvent?.content
        } else {
            senderMembershipEvent = chunkEntity.timelineEvents.buildQuery(senderId, isUnlinked).prev(since = stateIndex)?.root
            senderRoomMemberContent = senderMembershipEvent?.content
            senderRoomMemberPrevContent = senderMembershipEvent?.prevContent
        }

        // We fallback to untimelinedStateEvents if we can't find membership events in timeline
        if (senderMembershipEvent == null) {
            senderMembershipEvent = roomEntity.untimelinedStateEvents
                    .where()
                    .equalTo(EventEntityFields.STATE_KEY, senderId)
                    .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_MEMBER)
                    .prev(since = stateIndex)
            senderRoomMemberContent = senderMembershipEvent?.content
            senderRoomMemberPrevContent = senderMembershipEvent?.prevContent
        }

        ContentMapper.map(senderRoomMemberContent).toModel<RoomMemberContent>()?.also {
            result.senderAvatar = it.avatarUrl
            result.senderName = it.displayName
            result.isUniqueDisplayName = RoomMemberHelper(realm, roomId).isUniqueDisplayName(it.displayName)
        }
        // We try to fallback on prev content if we got a room member state events with null fields
        if (root?.type == EventType.STATE_ROOM_MEMBER) {
            ContentMapper.map(senderRoomMemberPrevContent).toModel<RoomMemberContent>()?.also {
                if (result.senderAvatar == null && it.avatarUrl != null) {
                    result.senderAvatar = it.avatarUrl
                }
                if (result.senderName == null && it.displayName != null) {
                    result.senderName = it.displayName
                    result.isUniqueDisplayName = RoomMemberHelper(realm, roomId).isUniqueDisplayName(it.displayName)
                }
            }
        }
        result.senderMembershipEventId = senderMembershipEvent?.eventId
        return result
    }
}
