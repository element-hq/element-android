package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection

internal fun ChunkEntity.add(event: Event, stateIndex: Int, paginationDirection: PaginationDirection) {
    if (!this.isManaged) {
        throw IllegalStateException("Chunk entity should be managed to use fast contains")
    }

    val eventEntity = event.asEntity()
    eventEntity.stateIndex = stateIndex

    if (!this.events.fastContains(eventEntity)) {
        val position = if (paginationDirection == PaginationDirection.FORWARDS) 0 else this.events.size
        this.events.add(position, eventEntity)
    }
}