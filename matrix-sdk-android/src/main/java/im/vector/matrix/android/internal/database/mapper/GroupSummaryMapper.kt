package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity


internal object GroupSummaryMapper {

    fun map(roomSummaryEntity: GroupSummaryEntity): GroupSummary {
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

internal fun GroupSummaryEntity.asDomain(): GroupSummary {
    return GroupSummaryMapper.map(this)
}