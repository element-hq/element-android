package im.vector.riotredesign.features.home

interface HomeNavigator {

    fun openRoomDetail(roomId: String, eventId: String?)

    fun openGroupDetail(groupId: String)

    fun openUserDetail(userId: String)

}