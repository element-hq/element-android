package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity


object GroupSummaryMapper {

    internal fun map(roomSummaryEntity: GroupSummaryEntity): GroupSummary {
        return GroupSummary(
                roomSummaryEntity.groupId,
                roomSummaryEntity.displayName,
                roomSummaryEntity.shortDescription,
                roomSummaryEntity.avatarUrl,
                roomSummaryEntity.roomIds.toList(),
                roomSummaryEntity.userIds.toList()
        )
    }
}

fun GroupSummaryEntity.asDomain(): GroupSummary {
    return GroupSummaryMapper.map(this)
}