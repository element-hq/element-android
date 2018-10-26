package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

fun RoomEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<RoomEntity> {
    return realm.where<RoomEntity>().equalTo(RoomEntityFields.ROOM_ID, roomId)
}

fun RoomEntity.Companion.where(realm: Realm, membership: MyMembership? = null): RealmQuery<RoomEntity> {
    val query = realm.where<RoomEntity>()
    if (membership != null) {
        query.equalTo(RoomEntityFields.MEMBERSHIP_STR, membership.name)
    }
    return query
}
