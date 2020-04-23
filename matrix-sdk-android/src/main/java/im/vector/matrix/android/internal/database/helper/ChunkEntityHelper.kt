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
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.sqldelight.session.SessionDatabase
import im.vector.matrix.sqldelight.session.TimelineEventQueries
import io.realm.Realm
import io.realm.Sort
import timber.log.Timber

internal fun ChunkEntity.deleteOnCascade() {
    assertIsManaged()
    this.timelineEvents.deleteAllFromRealm()
    this.deleteFromRealm()
}

internal fun ChunkEntity.merge(roomId: String, chunkToMerge: ChunkEntity, direction: PaginationDirection) {
    assertIsManaged()
    val localRealm = this.realm
    val eventsToMerge: List<TimelineEventEntity>
    if (direction == PaginationDirection.FORWARDS) {
        this.nextToken = chunkToMerge.nextToken
        this.isLastForward = chunkToMerge.isLastForward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
    } else {
        this.prevToken = chunkToMerge.prevToken
        this.isLastBackward = chunkToMerge.isLastBackward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
    }
    chunkToMerge.stateEvents.forEach { stateEvent ->
        addStateEvent(roomId, stateEvent, direction)
    }
    return eventsToMerge
            .forEach {
                addTimelineEventFromMerge(localRealm, it, direction)
            }
}

internal fun ChunkEntity.addStateEvent(roomId: String, stateEvent: EventEntity, direction: PaginationDirection) {
    if (direction == PaginationDirection.BACKWARDS) {
        Timber.v("We don't keep chunk state events when paginating backward")
    } else {
        val stateKey = stateEvent.stateKey ?: return
        val type = stateEvent.type
        val pastStateEvent = stateEvents.where()
                .equalTo(EventEntityFields.ROOM_ID, roomId)
                .equalTo(EventEntityFields.STATE_KEY, stateKey)
                .equalTo(CurrentStateEventEntityFields.TYPE, type)
                .findFirst()

        if (pastStateEvent != null) {
            stateEvents.remove(pastStateEvent)
        }
        stateEvents.add(stateEvent)
    }
}

internal fun SessionDatabase.addTimelineEvent(roomId: String,
                                              chunkId: Long,
                                              event: Event,
                                              direction: PaginationDirection,
                                              roomMemberContentsByUser: Map<String, RoomMemberContent?>) {
    val eventId = event.eventId ?: "$roomId-$chunkId-${System.currentTimeMillis()}"
    val displayIndex = timelineEventQueries.nextDisplayIndex(direction, chunkId)
    val senderId = event.senderId ?: ""
    val roomMemberContent = roomMemberContentsByUser[senderId]
    // Update RR for the sender of a new message with a dummy one
    handleReadReceipts(this, roomId, eventId, event.originServerTs, senderId)
    val isDisplayNameUnique = if (roomMemberContent?.displayName != null) {
        computeIsUnique(this, roomId, chunkId, roomMemberContent, roomMemberContentsByUser)
    } else {
        true
    }
    timelineEventQueries.insert(
            event_id = eventId,
            sender_avatar = roomMemberContent?.avatarUrl,
            chunk_id = chunkId,
            room_id = roomId,
            display_index = displayIndex,
            is_unique_display_name = isDisplayNameUnique,
            sender_name = roomMemberContent?.displayName
    )
}

private fun computeIsUnique(
        sessionDatabase: SessionDatabase,
        roomId: String,
        chunkId: Long,
        myRoomMemberContent: RoomMemberContent,
        roomMemberContentsByUser: Map<String, RoomMemberContent?>
): Boolean {
    val isHistoricalUnique = roomMemberContentsByUser.values.find {
        it != myRoomMemberContent && it?.displayName == myRoomMemberContent.displayName
    } == null
    val isLastForward = sessionDatabase.chunkQueries.isLastForward(chunkId).executeAsOne()
    return if (isLastForward) {
        val countMembersWithName = sessionDatabase.roomMemberSummaryQueries.countMembersWithNameInRoom(myRoomMemberContent.displayName, roomId).executeAsOne()
        val isLiveUnique = countMembersWithName == 1L
        isHistoricalUnique && isLiveUnique
    } else {
        isHistoricalUnique
    }
}

private fun ChunkEntity.addTimelineEventFromMerge(realm: Realm, timelineEventEntity: TimelineEventEntity, direction: PaginationDirection) {
    /*
    val eventId = timelineEventEntity.eventId
    if (timelineEvents.find(eventId) != null) {
        return
    }
    val displayIndex = nextDisplayIndex(direction)
    val localId = TimelineEventEntity.nextId(realm)
    val copied = realm.createObject<TimelineEventEntity>().apply {
        this.localId = localId
        this.root = timelineEventEntity.root
        this.eventId = timelineEventEntity.eventId
        this.roomId = timelineEventEntity.roomId
        this.annotations = timelineEventEntity.annotations
        this.readReceipts = timelineEventEntity.readReceipts
        this.displayIndex = displayIndex
        this.senderAvatar = timelineEventEntity.senderAvatar
        this.senderName = timelineEventEntity.senderName
        this.isUniqueDisplayName = timelineEventEntity.isUniqueDisplayName
    }
    timelineEvents.add(copied)
     */
}

private fun handleReadReceipts(
        sessionDatabase: SessionDatabase,
        roomId: String,
        eventId: String,
        originServerTs: Long?,
        senderId: String) {
    if (originServerTs != null) {
        val timestampOfEvent = originServerTs.toDouble()
        val oldTimestamp = sessionDatabase.readReceiptQueries.getTimestampForUser(roomId, senderId).executeAsOneOrNull()
        // If the synced RR is older, update
        if (oldTimestamp == null || timestampOfEvent > oldTimestamp) {
            sessionDatabase.readReceiptQueries.updateReadReceipt(eventId, timestampOfEvent, roomId, senderId)
        }
    }
}

internal fun TimelineEventQueries.nextDisplayIndex(direction: PaginationDirection, chunkId: Long): Int {
    return when (direction) {
        PaginationDirection.FORWARDS -> (getMaxDisplayIndex(chunkId).executeAsOneOrNull()?.max_display_index?.toInt()
                ?: 0) + 1
        PaginationDirection.BACKWARDS -> (getMinDisplayIndex(chunkId).executeAsOneOrNull()?.min_display_index?.toInt()
                ?: 0) - 1
    }
}
