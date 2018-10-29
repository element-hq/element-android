package im.vector.riotredesign.features.home.list

import android.content.Context
import com.airbnb.epoxy.Typed2EpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.features.home.RoomSummaryViewHelper

class RoomSummaryController(private val context: Context,
                            private val callback: Callback? = null
) : Typed2EpoxyController<List<RoomSummary>, RoomSummary>() {

    override fun buildModels(summaries: List<RoomSummary>?, selected: RoomSummary?) {
        summaries?.forEach {
            val roomSummaryViewHelper = RoomSummaryViewHelper(it)
            RoomSummaryItem(
                    title = it.displayName,
                    avatarDrawable = roomSummaryViewHelper.avatarDrawable(context),
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