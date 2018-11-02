package im.vector.riotredesign.features.home.room.list

import com.airbnb.epoxy.Typed2EpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryController(private val callback: Callback? = null
) : Typed2EpoxyController<List<RoomSummary>, RoomSummary>() {


    private var directRoomsExpanded = true
    private var groupRoomsExpanded = true

    override fun buildModels(summaries: List<RoomSummary>?, selected: RoomSummary?) {

        val directRooms = summaries?.filter { it.isDirect } ?: emptyList()
        val groupRooms = summaries?.filter { !it.isDirect } ?: emptyList()

        RoomCategoryItem(
                title = "DIRECT MESSAGES",
                isExpanded = directRoomsExpanded,
                listener = {
                    directRoomsExpanded = !directRoomsExpanded
                    setData(summaries, selected)
                }
        )
                .id("direct_messages")
                .addTo(this)

        if (directRoomsExpanded) {
            buildRoomModels(directRooms, selected)
        }

        RoomCategoryItem(
                title = "GROUPS",
                isExpanded = groupRoomsExpanded,
                listener = {
                    groupRoomsExpanded = !groupRoomsExpanded
                    setData(summaries, selected)
                }
        )
                .id("group_messages")
                .addTo(this)

        if (groupRoomsExpanded) {
            buildRoomModels(groupRooms, selected)
        }

    }

    private fun buildRoomModels(summaries: List<RoomSummary>, selected: RoomSummary?) {

        summaries.forEach {
            RoomSummaryItem(
                    roomName = it.displayName,
                    avatarUrl = it.avatarUrl,
                    isSelected = it.roomId == selected?.roomId,
                    listener = { callback?.onRoomSelected(it) }
            )
                    .id(it.roomId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onRoomSelected(room: RoomSummary)
    }

}
