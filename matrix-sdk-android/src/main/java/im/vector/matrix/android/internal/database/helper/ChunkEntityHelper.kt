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
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.mapper.updateWith
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.extensions.assertIsManaged
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Sort

internal fun ChunkEntity.deleteOnCascade() {
    assertIsManaged()
    this.events.deleteAllFromRealm()
    this.deleteFromRealm()
}

// By default if a chunk is empty we consider it unlinked
internal fun ChunkEntity.isUnlinked(): Boolean {
    assertIsManaged()
    return events.where().equalTo(EventEntityFields.IS_UNLINKED, false).findAll().isEmpty()
}

internal fun ChunkEntity.merge(roomId: String,
                               chunkToMerge: ChunkEntity,
                               direction: PaginationDirection) {
    assertIsManaged()
    val isChunkToMergeUnlinked = chunkToMerge.isUnlinked()
    val isCurrentChunkUnlinked = this.isUnlinked()
    val isUnlinked = isCurrentChunkUnlinked && isChunkToMergeUnlinked

    if (isCurrentChunkUnlinked && !isChunkToMergeUnlinked) {
        this.events.forEach { it.isUnlinked = false }
    }
    val eventsToMerge: List<EventEntity>
    if (direction == PaginationDirection.FORWARDS) {
        this.nextToken = chunkToMerge.nextToken
        this.isLast = chunkToMerge.isLast
        eventsToMerge = chunkToMerge.events.reversed()
    } else {
        this.prevToken = chunkToMerge.prevToken
        eventsToMerge = chunkToMerge.events
    }
    eventsToMerge.forEach {
        add(roomId, it.asDomain(), direction, isUnlinked = isUnlinked)
    }
}

internal fun ChunkEntity.addAll(roomId: String,
                                events: List<Event>,
                                direction: PaginationDirection,
                                stateIndexOffset: Int = 0,
                                // Set to true for Event retrieved from a Permalink (i.e. not linked to live Chunk)
                                isUnlinked: Boolean = false) {
    assertIsManaged()
    events.forEach { event ->
        add(roomId, event, direction, stateIndexOffset, isUnlinked)
    }
}

internal fun ChunkEntity.updateDisplayIndexes() {
    events.forEachIndexed { index, eventEntity -> eventEntity.displayIndex = index }
}

internal fun ChunkEntity.add(roomId: String,
                             event: Event,
                             direction: PaginationDirection,
                             stateIndexOffset: Int = 0,
                             isUnlinked: Boolean = false) {

    assertIsManaged()
    if (event.eventId.isNullOrEmpty() || events.fastContains(event.eventId)) {
        return
    }
    var currentStateIndex = lastStateIndex(direction, defaultValue = stateIndexOffset)
    if (direction == PaginationDirection.FORWARDS && EventType.isStateEvent(event.type)) {
        currentStateIndex += 1
    } else if (direction == PaginationDirection.BACKWARDS && events.isNotEmpty()) {
        val lastEventType = events.last()?.type ?: ""
        if (EventType.isStateEvent(lastEventType)) {
            currentStateIndex -= 1
        }
    }
    val eventEntity = event.toEntity(roomId)
    eventEntity.updateWith(currentStateIndex, isUnlinked)
    val position = if (direction == PaginationDirection.FORWARDS) 0 else this.events.size
    events.add(position, eventEntity)
}

internal fun ChunkEntity.lastStateIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
               PaginationDirection.FORWARDS  -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING).findFirst()?.stateIndex
               PaginationDirection.BACKWARDS -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.ASCENDING).findFirst()?.stateIndex
           } ?: defaultValue
}