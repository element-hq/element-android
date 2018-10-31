package im.vector.riotredesign.features.home.room.detail

import android.arch.paging.PagedList
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.riotredesign.features.home.LoadingItemModel_

private const val PREFETCH_DISTANCE = 5

class TimelineEventController : EpoxyController(
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
        data.forEachIndexed { index, enrichedEvent ->
            val item = if (enrichedEvent.root.type == EventType.MESSAGE) {
                val messageContent = enrichedEvent.root.content<MessageContent>()
                val roomMember = enrichedEvent.getMetadata<Event>(EventType.STATE_ROOM_MEMBER)?.content<RoomMember>()
                val title = "${roomMember?.displayName} : ${messageContent?.body}"
                TimelineEventItem(title = title)
            } else {
                TimelineEventItem(title = enrichedEvent.toString())
            }
            item
                    .onBind { timeline?.loadAround(index) }
                    .id(enrichedEvent.root.eventId)
                    .addTo(this)
        }

        val isLastEvent = data.last().getMetadata<Boolean>(EnrichedEvent.IS_LAST_EVENT) ?: false
        LoadingItemModel_()
                .id("backward_loading_item")
                .addIf(!isLastEvent, this)
    }

}