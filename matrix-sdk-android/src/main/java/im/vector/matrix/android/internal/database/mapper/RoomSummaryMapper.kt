package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity


internal object RoomSummaryMapper {

    fun map(roomSummaryEntity: RoomSummaryEntity): RoomSummary {
        return RoomSummary(
                roomSummaryEntity.roomId,
                roomSummaryEntity.displayName ?: "",
                roomSummaryEntity.topic ?: "",
                roomSummaryEntity.avatarUrl ?: "",
                roomSummaryEntity.isDirect,
                roomSummaryEntity.otherMemberIds.toList()
        )
    }
}

internal fun RoomSummaryEntity.asDomain(): RoomSummary {
    return RoomSummaryMapper.map(this)
}