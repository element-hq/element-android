package im.vector.riotredesign.features.home

import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary

sealed class HomeActions {

    data class SelectRoom(val roomSummary: RoomSummary) : HomeActions()

    data class SelectGroup(val groupSummary: GroupSummary) : HomeActions()

    data class PermalinkClicked(val permalinkData: PermalinkData) : HomeActions()

    object RoomDisplayed : HomeActions()

}