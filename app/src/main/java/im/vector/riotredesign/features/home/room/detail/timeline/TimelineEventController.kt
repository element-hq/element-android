package im.vector.riotredesign.features.home.room.detail.timeline

import android.arch.paging.PagedList
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.LoadingItemModel_

class TimelineEventController(private val roomId: String,
                              private val messageItemFactory: MessageItemFactory,
                              private val textItemFactory: TextItemFactory,
                              private val dateFormatter: TimelineDateFormatter
) : EpoxyController(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    private val pagedListCallback = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            buildSnapshotList()
        }

        override fun onInserted(position: Int, count: Int) {
            buildSnapshotList()
        }

        override fun onRemoved(position: Int, count: Int) {
            buildSnapshotList()
        }
    }

    private var snapshotList: List<EnrichedEvent>? = emptyList()
    var timeline: PagedList<EnrichedEvent>? = null
        set(value) {
            field?.removeWeakCallback(pagedListCallback)
            field = value
            field?.addWeakCallback(null, pagedListCallback)
            buildSnapshotList()
        }

    override fun buildModels() {
        buildModels(snapshotList)
    }

    private fun buildModels(data: List<EnrichedEvent?>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        for (index in 0 until data.size) {
            val event = data[index] ?: continue
            val nextEvent = if (index + 1 < data.size) data[index + 1] else null

            val date = event.root.localDateTime()
            val nextDate = nextEvent?.root?.localDateTime()
            val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

            val item = when (event.root.type) {
                EventType.MESSAGE -> messageItemFactory.create(event, nextEvent, addDaySeparator, date)
                else -> textItemFactory.create(event)
            }
            item
                    ?.onBind { timeline?.loadAround(index) }
                    ?.id(event.root.eventId)
                    ?.addTo(this)

            if (addDaySeparator) {
                val formattedDay = dateFormatter.formatMessageDay(date)
                DaySeparatorItem(formattedDay).id(roomId + formattedDay).addTo(this)
            }
        }

        //It's a hack at the moment
        val isLastEvent = data.last()?.root?.type == EventType.STATE_ROOM_CREATE
        LoadingItemModel_()
                .id(roomId + "backward_loading_item")
                .addIf(!isLastEvent, this)
    }

    private fun buildSnapshotList() {
        snapshotList = timeline?.snapshot() ?: emptyList()
        requestModelBuild()
    }

}