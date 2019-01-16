package im.vector.riotredesign.features.home.room.detail.timeline

import com.airbnb.epoxy.EpoxyModel
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.TimelineEvent

class TimelineItemFactory(private val messageItemFactory: MessageItemFactory,
                          private val roomNameItemFactory: RoomNameItemFactory,
                          private val roomTopicItemFactory: RoomTopicItemFactory,
                          private val roomMemberItemFactory: RoomMemberItemFactory,
                          private val defaultItemFactory: DefaultItemFactory) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?): EpoxyModel<*>? {

        return when (event.root.type) {
            EventType.MESSAGE           -> messageItemFactory.create(event, nextEvent, callback)
            EventType.STATE_ROOM_NAME   -> roomNameItemFactory.create(event)
            EventType.STATE_ROOM_TOPIC  -> roomTopicItemFactory.create(event)
            EventType.STATE_ROOM_MEMBER -> roomMemberItemFactory.create(event)
            else                        -> defaultItemFactory.create(event)
        }
    }

}