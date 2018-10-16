package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntity
import io.realm.Realm

fun ChunkEntity.Companion.getLastChunkFromRoom(realm: Realm, roomId: String): ChunkEntity? {
    return realm.where(ChunkEntity::class.java)
            .equalTo("room.roomId", roomId)
            .isNull("nextToken")
            .and()
            .isNotNull("prevToken")
            .findAll()
            .lastOrNull()
}

fun ChunkEntity.Companion.getChunkIncludingEvents(realm: Realm, eventIds: List<String>): ChunkEntity? {
    return realm.where(ChunkEntity::class.java)
            .`in`("events.eventId", eventIds.toTypedArray())
            .findFirst()
}