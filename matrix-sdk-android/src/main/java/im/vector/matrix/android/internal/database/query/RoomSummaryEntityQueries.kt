package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

fun RoomSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    val query = realm.where<RoomSummaryEntity>()
    if (roomId != null) {
        query.equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
    }
    return query
}

fun RoomSummaryEntity.Companion.lastSelected(realm: Realm): RoomSummaryEntity? {
    return realm.where<RoomSummaryEntity>()
            .equalTo(RoomSummaryEntityFields.IS_LATEST_SELECTED, true)
            .findFirst()
}
