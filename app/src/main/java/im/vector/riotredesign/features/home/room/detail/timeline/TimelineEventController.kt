package im.vector.riotredesign.features.home.room.detail.timeline

import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyModel
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineData
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.LoadingItemModel_
import im.vector.riotredesign.features.home.room.detail.timeline.paging.PagedListEpoxyController

class TimelineEventController(private val roomId: String,
                              private val messageItemFactory: MessageItemFactory,
                              private val textItemFactory: TextItemFactory,
                              private val dateFormatter: TimelineDateFormatter
) : PagedListEpoxyController<EnrichedEvent>(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {
    init {
        setFilterDuplicates(true)
    }

    private var isLoadingForward: Boolean = false
    private var isLoadingBackward: Boolean = false
    private var hasReachedEnd: Boolean = false

    var callback: Callback? = null

    fun update(timelineData: TimelineData?) {
        timelineData?.let {
            isLoadingForward = it.isLoadingForward
            isLoadingBackward = it.isLoadingBackward
            hasReachedEnd = it.events.lastOrNull()?.root?.type == EventType.STATE_ROOM_CREATE
            submitList(it.events)
            requestModelBuild()
        }
    }


    override fun buildItemModels(currentPosition: Int, items: List<EnrichedEvent?>): List<EpoxyModel<*>> {
        if (items.isNullOrEmpty()) {
            return emptyList()
        }
        val epoxyModels = ArrayList<EpoxyModel<*>>()
        val event = items[currentPosition] ?: return emptyList()
        val nextEvent = if (currentPosition + 1 < items.size) items[currentPosition + 1] else null

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        val item = when (event.root.type) {
            EventType.MESSAGE -> messageItemFactory.create(event, nextEvent, addDaySeparator, date, callback)
            else              -> textItemFactory.create(event)
        }
        item?.also {
            it.id(event.localId)
            epoxyModels.add(it)
        }

        if (addDaySeparator) {
            val formattedDay = dateFormatter.formatMessageDay(date)
            val daySeparatorItem = DaySeparatorItem(formattedDay).id(roomId + formattedDay)
            epoxyModels.add(daySeparatorItem)
        }
        return epoxyModels
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        LoadingItemModel_()
                .id(roomId + "forward_loading_item")
                .addIf(isLoadingForward, this)

        super.add(models)

        LoadingItemModel_()
                .id(roomId + "backward_loading_item")
                .addIf(!hasReachedEnd, this)
    }


    interface Callback {
        fun onUrlClicked(url: String)
    }

}