package im.vector.riotredesign.features.home.room.list

import im.vector.matrix.android.api.session.room.model.RoomSummary

sealed class RoomListActions {

    data class SelectRoom(val roomSummary: RoomSummary) : RoomListActions()


}