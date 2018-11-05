package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.session.group.DefaultGroup


object GroupMapper {

    internal fun map(groupEntity: GroupEntity): Group {
        return DefaultGroup(
                groupEntity.groupId
        )
    }
}

fun GroupEntity.asDomain(): Group {
    return GroupMapper.map(this)
}