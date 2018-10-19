package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults

fun EventEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
}

fun EventEntity.Companion.where(realm: Realm, chunk: ChunkEntity): RealmQuery<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("chunk.prevToken", chunk.prevToken)
            .and()
            .equalTo("chunk.nextToken", chunk.nextToken)
}

fun RealmResults<EventEntity>.getLast(type: String? = null): EventEntity? {
    var query = this.where()
    if (type != null) {
        query = query.equalTo("type", type)
    }
    return query.findAll().sort("age").last()
}