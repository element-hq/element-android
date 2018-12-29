package im.vector.riotredesign.features.home.room.detail

sealed class RoomDetailActions {

    data class SendMessage(val text: String) : RoomDetailActions()

}