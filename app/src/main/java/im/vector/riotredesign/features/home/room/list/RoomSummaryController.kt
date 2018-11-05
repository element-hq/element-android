package im.vector.riotredesign.features.home.room.list

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.features.home.HomeViewState

class RoomSummaryController(private val callback: Callback? = null
) : TypedEpoxyController<HomeViewState>() {


    private var isDirectRoomsExpanded = true
    private var isGroupRoomsExpanded = true

    override fun buildModels(viewState: HomeViewState) {

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
            buildRoomModels(viewState.directRooms, viewState.selectedRoom)
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
            buildRoomModels(viewState.groupRooms, viewState.selectedRoom)
        }

    }

    private fun buildRoomModels(summaries: List<RoomSummary>, selected: RoomSummary?) {
        summaries.forEach { roomSummary ->
            val isSelected = roomSummary.roomId == selected?.roomId
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
