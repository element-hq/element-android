package im.vector.riotredesign.features.home.room.detail.timeline

import android.arch.paging.PagedList
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineData
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

    init {
        setFilterDuplicates(true)
    }

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

    private var snapshotList: List<EnrichedEvent> = emptyList()
    private var timelineData: TimelineData? = null
    var callback: Callback? = null

    fun update(timelineData: TimelineData?) {
        timelineData?.events?.removeWeakCallback(pagedListCallback)
        this.timelineData = timelineData
        timelineData?.events?.addWeakCallback(null, pagedListCallback)
        buildSnapshotList()
    }

    override fun buildModels() {
        buildModelsWith(
                snapshotList,
                timelineData?.isLoadingForward ?: false,
                timelineData?.isLoadingBackward ?: false
        )
    }

    private fun buildModelsWith(events: List<EnrichedEvent?>,
                                isLoadingForward: Boolean,
                                isLoadingBackward: Boolean) {
        if (events.isEmpty()) {
            return
        }
        LoadingItemModel_()
                .id(roomId + "forward_loading_item")
                .addIf(isLoadingForward, this)

        for (index in 0 until events.size) {
            val event = events[index] ?: continue
            val nextEvent = if (index + 1 < events.size) events[index + 1] else null

            val date = event.root.localDateTime()
            val nextDate = nextEvent?.root?.localDateTime()
            val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

            val item = when (event.root.type) {
                EventType.MESSAGE -> messageItemFactory.create(event, nextEvent, addDaySeparator, date, callback)
                else              -> textItemFactory.create(event)
            }

            item
                    ?.onBind {
                        timelineData?.events?.loadAround(index)
                    }
                    ?.id(event.localId)
                    ?.addTo(this)

            if (addDaySeparator) {
                val formattedDay = dateFormatter.formatMessageDay(date)
                DaySeparatorItem(formattedDay).id(roomId + formattedDay).addTo(this)
            }
        }

        LoadingItemModel_()
                .id(roomId + "backward_loading_item")
                .addIf(isLoadingBackward, this)

    }

    private fun buildSnapshotList() {
        snapshotList = timelineData?.events?.snapshot() ?: emptyList()
        requestModelBuild()
    }

    interface Callback {
        fun onUrlClicked(url: String)
    }

}