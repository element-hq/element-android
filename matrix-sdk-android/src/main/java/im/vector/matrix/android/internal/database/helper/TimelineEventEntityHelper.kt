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
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.next
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery

internal fun TimelineEventEntity.updateSenderData(realm: Realm, chunkEntity: ChunkEntity) {
    val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst() ?: return
    val stateIndex = root?.stateIndex ?: return
    val senderId = root?.sender ?: return
    var senderMembershipEvent: EventEntity?
    var senderRoomMemberContent: String?
    var senderRoomMemberPrevContent: String?
    when {
        stateIndex <= 0 -> {
            senderMembershipEvent = chunkEntity.timelineEvents.buildTimelineEventQuery(senderId).next(from = stateIndex)?.root
            senderRoomMemberContent = senderMembershipEvent?.prevContent
            senderRoomMemberPrevContent = senderMembershipEvent?.content
        }
        else            -> {
            senderMembershipEvent = chunkEntity.timelineEvents.buildTimelineEventQuery(senderId).prev(since = stateIndex)?.root
            senderRoomMemberContent = senderMembershipEvent?.content
            senderRoomMemberPrevContent = senderMembershipEvent?.prevContent
        }
    }

    // We fallback to chunk stateEvents if we can't find membership events in timeline
    if (senderMembershipEvent == null) {
        senderMembershipEvent = chunkEntity.stateEvents
                .buildStateEventQuery(senderId)
                .prev()
        senderRoomMemberContent = senderMembershipEvent?.content
        senderRoomMemberPrevContent = senderMembershipEvent?.prevContent
    }

    // We fallback to room stateEvents if we can't find membership events in timeline and chunk
    if (senderMembershipEvent == null) {
        senderMembershipEvent = roomEntity.untimelinedStateEvents
                .buildStateEventQuery(senderId)
                .prev()
        senderRoomMemberContent = senderMembershipEvent?.content
        senderRoomMemberPrevContent = senderMembershipEvent?.prevContent
    }

    ContentMapper.map(senderRoomMemberContent).toModel<RoomMember>()?.also {
        this.senderAvatar = it.avatarUrl
        this.senderName = it.displayName
    }

    // We try to fallback on prev content if we got a room member state events with null fields
    if (root?.type == EventType.STATE_ROOM_MEMBER) {
        ContentMapper.map(senderRoomMemberPrevContent).toModel<RoomMember>()?.also {
            if (this.senderAvatar == null && it.avatarUrl != null) {
                this.senderAvatar = it.avatarUrl
            }
            if (this.senderName == null && it.displayName != null) {
                this.senderName = it.displayName
            }
        }
    }
    this.senderMembershipEvent = senderMembershipEvent
}

internal fun TimelineEventEntity.Companion.nextId(realm: Realm): Long {
    val currentIdNum = realm.where(TimelineEventEntity::class.java).max(TimelineEventEntityFields.LOCAL_ID)
    return if (currentIdNum == null) {
        1
    } else {
        currentIdNum.toLong() + 1
    }
}

private fun RealmList<EventEntity>.buildStateEventQuery(sender: String): RealmQuery<EventEntity> {
    return where()
            .equalTo(EventEntityFields.STATE_KEY, sender)
            .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_MEMBER)
}


private fun RealmList<TimelineEventEntity>.buildTimelineEventQuery(sender: String): RealmQuery<TimelineEventEntity> {
    return where()
            .equalTo(TimelineEventEntityFields.ROOT.STATE_KEY, sender)
            .equalTo(TimelineEventEntityFields.ROOT.TYPE, EventType.STATE_ROOM_MEMBER)
}
