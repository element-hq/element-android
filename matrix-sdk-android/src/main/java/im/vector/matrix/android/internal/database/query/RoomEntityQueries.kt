package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntity_
import io.objectbox.Box

fun RoomEntity.Companion.getForId(roomBox: Box<RoomEntity>, roomId: String): RoomEntity? {
    return roomBox
            .query()
            .equal(RoomEntity_.roomId, roomId)
            .build()
            .findUnique()
}

fun RoomEntity.Companion.getAll(roomBox: Box<RoomEntity>, membership: RoomEntity.Membership? = null): List<RoomEntity> {
    val query = roomBox.query()
    if (membership != null) {
        query.filter { it.membership == membership }
    }
    return query.build().find()
}