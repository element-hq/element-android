package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Sort


internal fun ChunkEntity.merge(chunkEntity: ChunkEntity,
                               direction: PaginationDirection) {


    chunkEntity.events.forEach {
        addOrUpdate(it.asDomain(), direction)
    }
    if (direction == PaginationDirection.FORWARDS) {
        nextToken = chunkEntity.nextToken
    } else {
        prevToken = chunkEntity.prevToken
    }
}

internal fun ChunkEntity.addAll(events: List<Event>,
                                direction: PaginationDirection,
                                stateIndexOffset: Int = 0) {

    events.forEach { event ->
        addOrUpdate(event, direction, stateIndexOffset)
    }
}

internal fun ChunkEntity.addOrUpdate(event: Event,
                                     direction: PaginationDirection,
                                     stateIndexOffset: Int = 0) {
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

    if (!events.fastContains(event.eventId)) {
        val eventEntity = event.asEntity()
        eventEntity.stateIndex = currentStateIndex
        val position = if (direction == PaginationDirection.FORWARDS) 0 else this.events.size
        events.add(position, eventEntity)
    } else {
        val eventEntity = events.find(event.eventId)
        eventEntity?.stateIndex = currentStateIndex
    }
}

internal fun ChunkEntity.lastStateIndex(direction: PaginationDirection, defaultValue: Int = 0): Int {
    return when (direction) {
               PaginationDirection.FORWARDS  -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING).findFirst()?.stateIndex
               PaginationDirection.BACKWARDS -> events.where().sort(EventEntityFields.STATE_INDEX, Sort.ASCENDING).findFirst()?.stateIndex
           } ?: defaultValue
}