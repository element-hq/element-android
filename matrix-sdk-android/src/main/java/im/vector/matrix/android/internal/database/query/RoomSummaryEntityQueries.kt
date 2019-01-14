package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun RoomSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    val query = realm.where<RoomSummaryEntity>()
    if (roomId != null) {
        query.equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
    }
    return query
}
