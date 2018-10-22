package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort

fun EventEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
}

fun EventEntity.Companion.where(realm: Realm, chunk: ChunkEntity?): RealmQuery<EventEntity> {
    var query = realm.where(EventEntity::class.java)
    if (chunk?.prevToken != null) {
        query = query.equalTo("chunk.prevToken", chunk.prevToken)
    }
    if (chunk?.nextToken != null) {
        query = query.equalTo("chunk.nextToken", chunk.nextToken)
    }
    return query
}

fun RealmResults<EventEntity>.getLast(type: String? = null): EventEntity? {
    var query = this.where().sort("originServerTs", Sort.DESCENDING)
    if (type != null) {
        query = query.equalTo("type", type)
    }
    return query.findFirst()
}