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

import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.CurrentStateEventEntityFields
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntityFields
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.createObject
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
                if (timelineEvents.find(it.eventId) == null) {
                    val eventId = it.eventId
                    if (timelineEvents.find(eventId) != null) {
                        return
                    }
                    val displayIndex = nextDisplayIndex(direction)
                    val localId = TimelineEventEntity.nextId(realm)
                    val copied = localRealm.createObject<TimelineEventEntity>().apply {
                        this.localId = localId
                        this.root = it.root
                        this.eventId = it.eventId
                        this.roomId = it.roomId
                        this.annotations = it.annotations
                        this.readReceipts = it.readReceipts
                        this.displayIndex = displayIndex
                        this.senderAvatar = it.senderAvatar
                        this.senderName = it.senderName
                        this.isUniqueDisplayName = it.isUniqueDisplayName
                    }
                    timelineEvents.add(copied)
                }
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

internal fun ChunkEntity.addTimelineEvent(roomId: String,
                                          eventEntity: EventEntity,
                                          direction: PaginationDirection,
                                          roomMemberContentsByUser: HashMap<String, RoomMemberContent?>) {
    val eventId = eventEntity.eventId
    if (timelineEvents.find(eventId) != null) {
        return
    }
    val displayIndex = nextDisplayIndex(direction)
    val localId = TimelineEventEntity.nextId(realm)
    val senderId = eventEntity.sender ?: ""

    // Update RR for the sender of a new message with a dummy one
    val readReceiptsSummaryEntity = handleReadReceipts(realm, roomId, eventEntity, senderId)
    val timelineEventEntity = realm.createObject<TimelineEventEntity>().apply {
        this.localId = localId
        this.root = eventEntity
        this.eventId = eventId
        this.roomId = roomId
        this.annotations = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
        this.readReceipts = readReceiptsSummaryEntity
        this.displayIndex = displayIndex
        val roomMemberContent = roomMemberContentsByUser[senderId]
        this.senderAvatar = roomMemberContent?.avatarUrl
        this.senderName = roomMemberContent?.displayName
        if (roomMemberContent?.displayName != null) {
            val isHistoricalUnique = roomMemberContentsByUser.values.find {
                it != roomMemberContent && it?.displayName == roomMemberContent.displayName
            } == null
            isUniqueDisplayName = if (isLastForward) {
                val isLiveUnique = RoomMemberSummaryEntity
                        .where(realm, roomId)
                        .equalTo(RoomMemberSummaryEntityFields.DISPLAY_NAME, roomMemberContent.displayName)
                        .findAll().none {
                            !roomMemberContentsByUser.containsKey(it.userId)
                        }
                isHistoricalUnique && isLiveUnique
            } else {
                isHistoricalUnique
            }
        } else {
            isUniqueDisplayName = true
        }
    }
    timelineEvents.add(timelineEventEntity)
}

private fun handleReadReceipts(realm: Realm, roomId: String, eventEntity: EventEntity, senderId: String): ReadReceiptsSummaryEntity {
    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventEntity.eventId).findFirst()
            ?: realm.createObject<ReadReceiptsSummaryEntity>(eventEntity.eventId).apply {
                this.roomId = roomId
            }
    val originServerTs = eventEntity.originServerTs
    if (originServerTs != null) {
        val timestampOfEvent = originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).findFirst()
            readReceiptOfSender.eventId = eventEntity.eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }
    return readReceiptsSummaryEntity
}

internal fun ChunkEntity.nextDisplayIndex(direction: PaginationDirection): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> {
            (timelineEvents.where().max(TimelineEventEntityFields.DISPLAY_INDEX)?.toInt() ?: 0) + 1
        }
        PaginationDirection.BACKWARDS -> {
            (timelineEvents.where().min(TimelineEventEntityFields.DISPLAY_INDEX)?.toInt() ?: 0) - 1
        }
    }
}
