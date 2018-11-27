package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.fillWith
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Sort
import io.realm.kotlin.createObject

internal fun ChunkEntity.isUnlinked(): Boolean {
    return events.where().equalTo(EventEntityFields.IS_UNLINKED, true).findAll().isNotEmpty()
}

internal fun ChunkEntity.merge(chunkToMerge: ChunkEntity,
                               direction: PaginationDirection) {

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
        addOrUpdate(it.asDomain(), direction, isUnlinked = isUnlinked)
    }
}

internal fun ChunkEntity.addAll(events: List<Event>,
                                direction: PaginationDirection,
                                stateIndexOffset: Int = 0,
                                isUnlinked: Boolean = false) {

    events.forEach { event ->
        addOrUpdate(event, direction, stateIndexOffset, isUnlinked)
    }
}

internal fun ChunkEntity.updateDisplayIndexes() {
    events.forEachIndexed { index, eventEntity -> eventEntity.displayIndex = index }
}

internal fun ChunkEntity.addOrUpdate(event: Event,
                                     direction: PaginationDirection,
                                     stateIndexOffset: Int = 0,
                                     isUnlinked: Boolean = false) {
    if (!isManaged) {
        throw IllegalStateException("Chunk entity should be managed to use fast contains")
    }

    if (event.eventId == null) {
        return
    }

    var currentStateIndex = lastStateIndex(direction, defaultValue = stateIndexOffset)
    if (direction == PaginationDirection.FORWARDS && event.isStateEvent()) {
        currentStateIndex += 1
    } else if (direction == PaginationDirection.BACKWARDS && events.isNotEmpty()) {
        val lastEventType = events.last()?.type ?: ""
        if (EventType.isStateEvent(lastEventType)) {
            currentStateIndex -= 1
        }
    }

    val eventEntity: EventEntity?
    if (!events.fastContains(event.eventId)) {
        eventEntity = realm.createObject()
        eventEntity.fillWith(event)
        val position = if (direction == PaginationDirection.FORWARDS) 0 else this.events.size
        events.add(position, eventEntity)
    } else {
        eventEntity = events.find(event.eventId)
    }
    eventEntity?.stateIndex = currentStateIndex
    eventEntity?.isUnlinked = isUnlinked
}

internal fun ChunkEntity.lastStateIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
        PaginationDirection.FORWARDS -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING).findFirst()?.stateIndex
        PaginationDirection.BACKWARDS -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.ASCENDING).findFirst()?.stateIndex
    } ?: defaultValue
}