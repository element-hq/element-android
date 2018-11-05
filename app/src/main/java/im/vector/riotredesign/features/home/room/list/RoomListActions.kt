package im.vector.riotredesign.features.home.room.list

import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.features.home.group.GroupListActions

sealed class RoomListActions {

    data class SelectRoom(val roomSummary: RoomSummary) : RoomListActions()

    object RoomDisplayed : RoomListActions()

}