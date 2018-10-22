package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.EventEntity
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort

fun EventEntity.Companion.where(realm: Realm, roomId: String, type: String? = null): RealmQuery<EventEntity> {
    var query = realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
    if (type != null) {
        query = query.equalTo("type", type)
    }
    return query
}


fun RealmQuery<EventEntity>.last(from: Long? = null): EventEntity? {
    var query = this
    if (from != null) {
        query = query.lessThanOrEqualTo("originServerTs", from)
    }
    return query
            .sort("originServerTs", Sort.DESCENDING)
            .findFirst()
}
