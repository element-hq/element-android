package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity


object RoomSummaryMapper {

    internal fun map(roomSummaryEntity: RoomSummaryEntity): RoomSummary {
        return RoomSummary(
                roomSummaryEntity.roomId,
                roomSummaryEntity.displayName ?: "",
                roomSummaryEntity.topic ?: ""
        )
    }
}

fun RoomSummaryEntity.asDomain(): RoomSummary {
    return RoomSummaryMapper.map(this)
}