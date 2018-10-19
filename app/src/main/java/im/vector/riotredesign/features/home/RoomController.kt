package im.vector.riotredesign.features.home

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.Room

class RoomController(private val callback: Callback? = null) : TypedEpoxyController<List<Room>>() {

    override fun buildModels(data: List<Room>?) {
        data?.forEach {
            RoomItem(it.roomId, listener = { callback?.onRoomSelected(it) })
                    .id(it.roomId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onRoomSelected(room: Room)
    }

}