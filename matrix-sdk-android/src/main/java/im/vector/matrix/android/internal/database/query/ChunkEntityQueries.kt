package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.where

internal fun ChunkEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .equalTo("${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
}

internal fun ChunkEntity.Companion.findWithPrevToken(realm: Realm, roomId: String, prevToken: String?): ChunkEntity? {
    if (prevToken == null) {
        return null
    }
    return where(realm, roomId)
            .and()
            .equalTo(ChunkEntityFields.PREV_TOKEN, prevToken)
            .findFirst()
}

internal fun ChunkEntity.Companion.findWithNextToken(realm: Realm, roomId: String, nextToken: String?): ChunkEntity? {
    if (nextToken == null) {
        return null
    }
    return where(realm, roomId)
            .and()
            .equalTo(ChunkEntityFields.NEXT_TOKEN, nextToken)
            .findFirst()
}

internal fun ChunkEntity.Companion.findLastLiveChunkFromRoom(realm: Realm, roomId: String): ChunkEntity? {
    return where(realm, roomId)
            .equalTo(ChunkEntityFields.IS_LAST, true)
            .findFirst()
}

internal fun ChunkEntity.Companion.findAllIncludingEvents(realm: Realm, eventIds: List<String>): RealmResults<ChunkEntity> {
    return realm.where<ChunkEntity>()
            .`in`(ChunkEntityFields.EVENTS.EVENT_ID, eventIds.toTypedArray())
            .notEqualTo(ChunkEntityFields.PREV_TOKEN, DBConstants.STATE_EVENTS_CHUNK_TOKEN)
            .notEqualTo(ChunkEntityFields.NEXT_TOKEN, DBConstants.STATE_EVENTS_CHUNK_TOKEN)
            .findAll()
}