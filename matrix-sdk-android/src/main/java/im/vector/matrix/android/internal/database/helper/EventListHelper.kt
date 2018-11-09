package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.mapper.fillWith
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.database.query.where
import io.realm.kotlin.createObject

internal fun List<Event>.addManagedToChunk(chunkEntity: ChunkEntity) {
    if (!chunkEntity.isManaged) {
        throw IllegalStateException("Chunk entity should be managed to use fast contains")
    }
    val realm = chunkEntity.realm
    this.forEach { event ->
        val eventEntity = EventEntity.where(realm, event.eventId).findFirst()
                ?: realm.createObject()

        eventEntity.fillWith(event)

        if (!chunkEntity.events.fastContains(eventEntity)) {
            chunkEntity.events.add(eventEntity)
        }
    }
}