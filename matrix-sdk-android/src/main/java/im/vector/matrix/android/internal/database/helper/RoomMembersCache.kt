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
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import io.realm.RealmList
import io.realm.RealmQuery
import timber.log.Timber

/**
 * This is an internal cache to avoid querying all the time the room member events
 */
internal class RoomMembersCache {

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

    fun get(timelineEventEntity: TimelineEventEntity): Value {
        val key = Key(
                roomId = timelineEventEntity.roomId,
                stateIndex = timelineEventEntity.root?.stateIndex ?: 0,
                senderId = timelineEventEntity.root?.sender ?: ""
        )
        val result: Value
        val start = System.currentTimeMillis()
        result = values.getOrPut(key) {
            doQueryAndBuildValue(timelineEventEntity)
        }
        val end = System.currentTimeMillis()
        Timber.v("Get value took: ${end - start} millis")
        return result
    }

    private fun doQueryAndBuildValue(timelineEventEntity: TimelineEventEntity): Value {
        return timelineEventEntity.computeValue()
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
        val isUnlinked = chunkEntity.isUnlinked()
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
            result.isUniqueDisplayName = RoomMembers(realm, roomId).isUniqueDisplayName(it.displayName)
        }
        // We try to fallback on prev content if we got a room member state events with null fields
        if (root?.type == EventType.STATE_ROOM_MEMBER) {
            ContentMapper.map(senderRoomMemberPrevContent).toModel<RoomMemberContent>()?.also {
                if (result.senderAvatar == null && it.avatarUrl != null) {
                    result.senderAvatar = it.avatarUrl
                }
                if (result.senderName == null && it.displayName != null) {
                    result.senderName = it.displayName
                    result.isUniqueDisplayName = RoomMembers(realm, roomId).isUniqueDisplayName(it.displayName)
                }
            }
        }
        result.senderMembershipEventId = senderMembershipEvent?.eventId
        return result
    }

}
