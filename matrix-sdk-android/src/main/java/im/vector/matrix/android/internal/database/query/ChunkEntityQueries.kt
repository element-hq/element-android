package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntity
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults

fun ChunkEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ChunkEntity> {
    return realm.where(ChunkEntity::class.java).equalTo("room.roomId", roomId)
}

fun ChunkEntity.Companion.findWithPrevToken(realm: Realm, roomId: String, prevToken: String?): ChunkEntity? {
    if (prevToken == null) {
        return null
    }
    return where(realm, roomId)
            .and()
            .equalTo("prevToken", prevToken)
            .findFirst()
}

fun ChunkEntity.Companion.findWithNextToken(realm: Realm, roomId: String, nextToken: String?): ChunkEntity? {
    if (nextToken == null) {
        return null
    }
    return where(realm, roomId)
            .and()
            .equalTo("nextToken", nextToken)
            .findFirst()
}

fun ChunkEntity.Companion.findLastFromRoom(realm: Realm, roomId: String): ChunkEntity? {
    return where(realm, roomId)
            .and()
            .isNull("nextToken")
            .findAll()
            .last()
}

fun ChunkEntity.Companion.findAllIncludingEvents(realm: Realm, eventIds: List<String>): RealmResults<ChunkEntity> {
    return realm.where(ChunkEntity::class.java)
            .`in`("events.eventId", eventIds.toTypedArray())
            .findAll()
}