package im.vector.riotredesign.features.home.room.list

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryController(private val callback: Callback? = null
) : TypedEpoxyController<RoomListViewState>() {

    private var isDirectRoomsExpanded = true
    private var isGroupRoomsExpanded = true

    override fun buildModels(viewState: RoomListViewState) {
        val roomSummaries = viewState.asyncRooms()
        RoomCategoryItem(
                title = "DIRECT MESSAGES",
                isExpanded = isDirectRoomsExpanded,
                listener = {
                    isDirectRoomsExpanded = !isDirectRoomsExpanded
                    setData(viewState)
                }
        )
                .id("direct_messages")
                .addTo(this)

        if (isDirectRoomsExpanded) {
            buildRoomModels(roomSummaries?.directRooms ?: emptyList(), viewState.selectedRoomId)
        }

        RoomCategoryItem(
                title = "GROUPS",
                isExpanded = isGroupRoomsExpanded,
                listener = {
                    isGroupRoomsExpanded = !isGroupRoomsExpanded
                    setData(viewState)
                }
        )
                .id("group_messages")
                .addTo(this)

        if (isGroupRoomsExpanded) {
            buildRoomModels(roomSummaries?.groupRooms ?: emptyList(), viewState.selectedRoomId)
        }

    }

    private fun buildRoomModels(summaries: List<RoomSummary>, selectedRoomId: String?) {
        summaries.forEach { roomSummary ->
            val isSelected = roomSummary.roomId == selectedRoomId
            RoomSummaryItem(
                    roomName = roomSummary.displayName,
                    avatarUrl = roomSummary.avatarUrl,
                    isSelected = isSelected,
                    listener = { callback?.onRoomSelected(roomSummary) }
            )
                    .id(roomSummary.roomId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onRoomSelected(room: RoomSummary)
    }

}
