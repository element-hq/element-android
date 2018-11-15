package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection


internal fun ChunkEntity.merge(chunkEntity: ChunkEntity,
                               direction: PaginationDirection) {

    val events = chunkEntity.events.map { it.asDomain() }
    addAll(events, direction)
    if (direction == PaginationDirection.FORWARDS) {
        nextToken = chunkEntity.nextToken
    } else {
        prevToken = chunkEntity.prevToken
    }
}

internal fun ChunkEntity.addAll(events: List<Event>,
                                direction: PaginationDirection,
                                updateStateIndex: Boolean = true) {

    events.forEach { event ->
        if (updateStateIndex && event.isStateEvent()) {
            updateStateIndex(direction)
        }
        addOrUpdate(event, direction)
    }
}

internal fun ChunkEntity.addOrUpdate(event: Event,
                                     direction: PaginationDirection) {
    if (!isManaged) {
        throw IllegalStateException("Chunk entity should be managed to use fast contains")
    }

    if (event.eventId == null) {
        return
    }

    val currentStateIndex = stateIndex(direction)
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