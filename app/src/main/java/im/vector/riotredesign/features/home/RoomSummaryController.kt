package im.vector.riotredesign.features.home

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryController(private val callback: Callback? = null
) : TypedEpoxyController<List<RoomSummary>>() {

    override fun buildModels(data: List<RoomSummary>?) {
        data?.forEach {
            RoomItem(it.displayName, listener = { callback?.onRoomSelected(it) })
                    .id(it.roomId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onRoomSelected(room: RoomSummary)
    }

}