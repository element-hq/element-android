package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomEntity
import io.realm.Realm
import io.realm.RealmResults

fun RoomEntity.Companion.getForId(realm: Realm, roomId: String): RoomEntity? {
    return realm.where<RoomEntity>(RoomEntity::class.java)
            .equalTo("roomId", roomId)
            .findFirst()
}

fun RoomEntity.Companion.getAllAsync(realm: Realm, membership: RoomEntity.Membership? = null): RealmResults<RoomEntity> {
    val query = realm.where(RoomEntity::class.java)
    if (membership != null) {
        query.equalTo("membership", membership.name)
    }
    return query.findAllAsync()
}
