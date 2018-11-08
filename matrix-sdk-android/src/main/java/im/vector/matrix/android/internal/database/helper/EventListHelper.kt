package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.fastContains

internal fun List<Event>.addManagedToChunk(chunkEntity: ChunkEntity) {
    if (!chunkEntity.isManaged) {
        throw IllegalStateException("Chunk entity should be managed to use fast contains")
    }
    this.forEach { event ->
        val eventEntity = event.asEntity()
        if (!chunkEntity.events.fastContains(eventEntity)) {
            chunkEntity.events.add(eventEntity)
        }
    }
}