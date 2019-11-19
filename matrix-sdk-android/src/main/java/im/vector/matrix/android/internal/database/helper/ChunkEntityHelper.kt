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
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Sort

// By default if a chunk is empty we consider it unlinked
internal fun ChunkEntity.isUnlinked(): Boolean {
    assertIsManaged()
    return timelineEvents.where()
            .equalTo(TimelineEventEntityFields.ROOT.IS_UNLINKED, false)
            .findAll()
            .isEmpty()
}

internal fun ChunkEntity.deleteOnCascade() {
    assertIsManaged()
    this.timelineEvents.deleteAllFromRealm()
    this.deleteFromRealm()
}

internal fun ChunkEntity.merge(roomId: String,
                               chunkToMerge: ChunkEntity,
                               direction: PaginationDirection) {
    assertIsManaged()
    val isChunkToMergeUnlinked = chunkToMerge.isUnlinked()
    val isCurrentChunkUnlinked = this.isUnlinked()
    val isUnlinked = isCurrentChunkUnlinked && isChunkToMergeUnlinked

    if (isCurrentChunkUnlinked && !isChunkToMergeUnlinked) {
        this.timelineEvents.forEach { it.root?.isUnlinked = false }
    }
    val eventsToMerge: List<TimelineEventEntity>
    if (direction == PaginationDirection.FORWARDS) {
        this.nextToken = chunkToMerge.nextToken
        this.isLastForward = chunkToMerge.isLastForward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.ASCENDING)
    } else {
        this.prevToken = chunkToMerge.prevToken
        this.isLastBackward = chunkToMerge.isLastBackward
        eventsToMerge = chunkToMerge.timelineEvents.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
    }
    val events = eventsToMerge.mapNotNull { it.root?.asDomain() }
    val eventIds = ArrayList<String>()
    events.forEach { event ->
        add(roomId, event, direction, isUnlinked = isUnlinked)
        if (event.eventId != null) {
            eventIds.add(event.eventId)
        }
    }
    updateSenderDataFor(eventIds)
}

internal fun ChunkEntity.addAll(roomId: String,
                                events: List<Event>,
                                direction: PaginationDirection,
                                stateIndexOffset: Int = 0,
        // Set to true for Event retrieved from a Permalink (i.e. not linked to live Chunk)
                                isUnlinked: Boolean = false) {
    assertIsManaged()
    val eventIds = ArrayList<String>()
    events.forEach { event ->
        add(roomId, event, direction, stateIndexOffset, isUnlinked)
        if (event.eventId != null) {
            eventIds.add(event.eventId)
        }
    }
    updateSenderDataFor(eventIds)
}

internal fun ChunkEntity.updateSenderDataFor(eventIds: List<String>) {
    for (eventId in eventIds) {
        val timelineEventEntity = timelineEvents.find(eventId) ?: continue
        timelineEventEntity.updateSenderData()
    }
}

internal fun ChunkEntity.add(roomId: String,
                             event: Event,
                             direction: PaginationDirection,
                             stateIndexOffset: Int = 0,
                             isUnlinked: Boolean = false) {
    assertIsManaged()
    if (event.eventId != null && timelineEvents.find(event.eventId) != null) {
        return
    }
    var currentDisplayIndex = lastDisplayIndex(direction, 0)
    if (direction == PaginationDirection.FORWARDS) {
        currentDisplayIndex += 1
        forwardsDisplayIndex = currentDisplayIndex
    } else {
        currentDisplayIndex -= 1
        backwardsDisplayIndex = currentDisplayIndex
    }
    var currentStateIndex = lastStateIndex(direction, defaultValue = stateIndexOffset)
    if (direction == PaginationDirection.FORWARDS && EventType.isStateEvent(event.type)) {
        currentStateIndex += 1
        forwardsStateIndex = currentStateIndex
    } else if (direction == PaginationDirection.BACKWARDS && timelineEvents.isNotEmpty()) {
        val lastEventType = timelineEvents.last()?.root?.type ?: ""
        if (EventType.isStateEvent(lastEventType)) {
            currentStateIndex -= 1
            backwardsStateIndex = currentStateIndex
        }
    }

    val localId = TimelineEventEntity.nextId(realm)
    val eventId = event.eventId ?: ""
    val senderId = event.senderId ?: ""

    val readReceiptsSummaryEntity = ReadReceiptsSummaryEntity.where(realm, eventId).findFirst()
            ?: ReadReceiptsSummaryEntity(eventId, roomId)

    // Update RR for the sender of a new message with a dummy one

    if (event.originServerTs != null) {
        val timestampOfEvent = event.originServerTs.toDouble()
        val readReceiptOfSender = ReadReceiptEntity.getOrCreate(realm, roomId = roomId, userId = senderId)
        // If the synced RR is older, update
        if (timestampOfEvent > readReceiptOfSender.originServerTs) {
            val previousReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId = readReceiptOfSender.eventId).findFirst()
            readReceiptOfSender.eventId = eventId
            readReceiptOfSender.originServerTs = timestampOfEvent
            previousReceiptsSummary?.readReceipts?.remove(readReceiptOfSender)
            readReceiptsSummaryEntity.readReceipts.add(readReceiptOfSender)
        }
    }

    val eventEntity = TimelineEventEntity(localId).also {
        it.root = event.toEntity(roomId).apply {
            this.stateIndex = currentStateIndex
            this.isUnlinked = isUnlinked
            this.displayIndex = currentDisplayIndex
            this.sendState = SendState.SYNCED
        }
        it.eventId = eventId
        it.roomId = roomId
        it.annotations = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
        it.readReceipts = readReceiptsSummaryEntity
    }
    val position = if (direction == PaginationDirection.FORWARDS) 0 else this.timelineEvents.size
    timelineEvents.add(position, eventEntity)
}

internal fun ChunkEntity.lastDisplayIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> forwardsDisplayIndex
        PaginationDirection.BACKWARDS -> backwardsDisplayIndex
    } ?: defaultValue
}

internal fun ChunkEntity.lastStateIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS  -> forwardsStateIndex
        PaginationDirection.BACKWARDS -> backwardsStateIndex
    } ?: defaultValue
}
