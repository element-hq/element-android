package im.vector.riotredesign.features.home.list

import im.vector.matrix.android.api.session.room.model.RoomSummary

sealed class RoomListActions {

    data class SelectRoom(val roomSummary: RoomSummary) : RoomListActions()


}