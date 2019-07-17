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

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.membership.RoomMembers

internal fun RoomEntity.deleteOnCascade(chunkEntity: ChunkEntity) {
    chunks.remove(chunkEntity)
    chunkEntity.deleteOnCascade()
}

internal fun RoomEntity.addOrUpdate(chunkEntity: ChunkEntity) {
    if (!chunks.contains(chunkEntity)) {
        chunks.add(chunkEntity)
    }
}

internal fun RoomEntity.addStateEvent(stateEvent: Event,
                                      stateIndex: Int = Int.MIN_VALUE,
                                      filterDuplicates: Boolean = false,
                                      isUnlinked: Boolean = false) {
    assertIsManaged()
    if (stateEvent.eventId == null || (filterDuplicates && fastContains(stateEvent.eventId))) {
        return
    } else {
        val entity = stateEvent.toEntity(roomId).apply {
            this.stateIndex = stateIndex
            this.isUnlinked = isUnlinked
            this.sendState = SendState.SYNCED
        }
        untimelinedStateEvents.add(entity)
    }
}
internal fun RoomEntity.addSendingEvent(event: Event) {
    assertIsManaged()
    val senderId = event.senderId ?: return
    val eventEntity = event.toEntity(roomId).apply {
        this.sendState = SendState.UNSENT
    }
    val roomMembers = RoomMembers(realm, roomId)
    val myUser = roomMembers.get(senderId)
    val localId = TimelineEventEntity.nextId(realm)
    val timelineEventEntity = TimelineEventEntity(localId).also {
        it.root = eventEntity
        it.eventId = event.eventId ?: ""
        it.roomId = roomId
        it.senderName = myUser?.displayName
        it.senderAvatar = myUser?.avatarUrl
        it.isUniqueDisplayName = roomMembers.isUniqueDisplayName(myUser?.displayName)
        it.senderMembershipEvent = roomMembers.queryRoomMemberEvent(senderId).findFirst()
    }
    sendingTimelineEvents.add(0, timelineEventEntity)
}
