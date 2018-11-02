package im.vector.riotredesign.features.home.room.detail.timeline

import android.arch.paging.PagedList
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.LoadingItemModel_

class TimelineEventController(private val messageItemFactory: MessageItemFactory,
                              private val textItemFactory: TextItemFactory,
                              private val dateFormatter: TimelineDateFormatter
) : EpoxyController(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    private val pagedListCallback = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            requestModelBuild()
        }

        override fun onInserted(position: Int, count: Int) {
            requestModelBuild()
        }

        override fun onRemoved(position: Int, count: Int) {
            requestModelBuild()
        }
    }

    var timeline: PagedList<EnrichedEvent>? = null
        set(value) {
            field?.removeWeakCallback(pagedListCallback)
            field = value
            field?.addWeakCallback(null, pagedListCallback)
        }

    override fun buildModels() {
        buildModels(timeline)
    }

    private fun buildModels(data: List<EnrichedEvent>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        for (index in 0 until data.size) {
            val event = data[index]
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
                DaySeparatorItem(formattedDay).id(formattedDay).addTo(this)
            }
        }

        //It's a hack at the moment
        val isLastEvent = data.last().root.type == EventType.STATE_ROOM_CREATE
        LoadingItemModel_()
                .id("backward_loading_item")
                .addIf(!isLastEvent, this)
    }

}