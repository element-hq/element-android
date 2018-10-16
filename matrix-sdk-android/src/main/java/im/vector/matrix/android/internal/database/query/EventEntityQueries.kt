package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.EventEntity
import io.realm.Realm
import io.realm.RealmResults

fun EventEntity.Companion.getAllFromRoom(realm: Realm, roomId: String): RealmResults<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
            .findAll()
}

fun RealmResults<EventEntity>.getLast(type: String? = null): EventEntity? {
    var query = this.where()
    if (type != null) {
        query = query.equalTo("type", type)
    }
    return query.findAll().sort("age").lastOrNull()
}