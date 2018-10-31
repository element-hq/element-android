package im.vector.riotredesign.features.home.room.detail

import android.arch.paging.PagedList
import android.content.Context
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.roomMember
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.riotredesign.core.extensions.avatarDrawable
import im.vector.riotredesign.features.home.LoadingItemModel_

class TimelineEventController(private val context: Context) : EpoxyController(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    private val messagesLoadedWithInformation = HashSet<String?>()

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

            if (event.root.type == EventType.MESSAGE) {
                val messageContent = event.root.content<MessageContent>()
                val roomMember = event.roomMember()
                if (messageContent == null || roomMember == null) {
                    continue
                }
                val nextRoomMember = nextEvent?.roomMember()
                if (nextRoomMember != roomMember) {
                    messagesLoadedWithInformation.add(event.root.eventId)
                }
                val showInformation = messagesLoadedWithInformation.contains(event.root.eventId)

                val avatarDrawable = context.avatarDrawable(roomMember.displayName ?: "")
                TimelineMessageItem(
                        message = messageContent.body,
                        showInformation = showInformation,
                        avatarDrawable = avatarDrawable,
                        memberName = roomMember.displayName
                )
                        .onBind { timeline?.loadAround(index) }
                        .id(event.root.eventId)
                        .addTo(this)
            }
        }

        //It's a hack at the moment
        val isLastEvent = data.last().root.type == EventType.STATE_ROOM_CREATE
        LoadingItemModel_()
                .id("backward_loading_item")
                .addIf(!isLastEvent, this)
    }

}