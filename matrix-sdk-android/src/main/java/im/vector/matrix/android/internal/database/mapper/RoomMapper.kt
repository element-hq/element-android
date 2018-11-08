package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.session.room.DefaultRoom


internal object RoomMapper {


    fun map(roomEntity: RoomEntity): Room {
        return DefaultRoom(
                roomEntity.roomId,
                roomEntity.membership
        )
    }
}

internal fun RoomEntity.asDomain(): Room {
    return RoomMapper.map(this)
}