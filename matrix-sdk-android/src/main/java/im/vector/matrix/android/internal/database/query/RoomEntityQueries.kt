package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomEntity
import io.realm.Realm
import io.realm.RealmQuery

fun RoomEntity.Companion.getForId(realm: Realm, roomId: String): RoomEntity? {
    return realm.where<RoomEntity>(RoomEntity::class.java)
            .equalTo("roomId", roomId)
            .findFirst()
}

fun RoomEntity.Companion.getAll(realm: Realm, membership: RoomEntity.Membership? = null): RealmQuery<RoomEntity> {
    val query = realm.where(RoomEntity::class.java)
    if (membership != null) {
        query.equalTo("membership", membership.name)
    }
    return query
}
