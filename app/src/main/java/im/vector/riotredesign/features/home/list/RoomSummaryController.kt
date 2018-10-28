package im.vector.riotredesign.features.home.list

import com.airbnb.epoxy.Typed2EpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryController(private val callback: Callback? = null

) : Typed2EpoxyController<List<RoomSummary>, RoomSummary>() {

    override fun buildModels(summaries: List<RoomSummary>?, selected: RoomSummary?) {
        summaries?.forEach {
            RoomSummaryItem(
                    it.displayName,
                    isSelected = it == selected,
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