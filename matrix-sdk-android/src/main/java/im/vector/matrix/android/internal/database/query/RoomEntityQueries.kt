package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.RoomEntity
import io.realm.Realm
import io.realm.RealmQuery

fun RoomEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<RoomEntity> {
    return realm.where<RoomEntity>(RoomEntity::class.java).equalTo("roomId", roomId)
}

fun RoomEntity.Companion.where(realm: Realm, membership: MyMembership? = null): RealmQuery<RoomEntity> {
    val query = realm.where(RoomEntity::class.java)
    if (membership != null) {
        query.equalTo("membership", membership.name)
    }
    return query
}
