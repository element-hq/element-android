package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.sqldelight.session.RoomTagEntity
import javax.inject.Inject

internal class RoomTagMapper @Inject constructor() {

    fun map(tag_name: String, tag_order: Double?): RoomTag {
        return RoomTag(
                name = tag_name,
                order = tag_order
        )
    }

    fun map(roomTagEntity: RoomTagEntity): RoomTag {
        return RoomTag(
                name = roomTagEntity.tag_name,
                order = roomTagEntity.tag_order
        )
    }

}
