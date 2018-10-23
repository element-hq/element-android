package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import io.realm.Realm
import io.realm.RealmQuery

fun RoomSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    val query = realm.where(RoomSummaryEntity::class.java)
    if (roomId != null) {
        query.equalTo("roomId", roomId)
    }
    return query
}
