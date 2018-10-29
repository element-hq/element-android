package im.vector.riotredesign.features.home.room.list

import android.content.Context
import com.airbnb.epoxy.Typed2EpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.RoomSummaryViewHelper

class RoomSummaryController(private val context: Context,
                            private val callback: Callback? = null
) : Typed2EpoxyController<List<RoomSummary>, RoomSummary>() {

    override fun buildModels(summaries: List<RoomSummary>?, selected: RoomSummary?) {
        RoomCategoryItem(
                title = "DIRECT MESSAGES",
                expandDrawable = R.drawable.ic_expand_more_white
        )
                .id("direct_messages")
                .addTo(this)

        summaries?.forEach {
            val roomSummaryViewHelper = RoomSummaryViewHelper(it)
            RoomSummaryItem(
                    title = it.displayName,
                    avatarDrawable = roomSummaryViewHelper.avatarDrawable(context),
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
