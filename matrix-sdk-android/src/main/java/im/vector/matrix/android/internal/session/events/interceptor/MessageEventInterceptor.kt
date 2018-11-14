package im.vector.matrix.android.internal.session.events.interceptor

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor


internal class MessageEventInterceptor(private val monarchy: Monarchy,
                                       private val roomId: String) : EnrichedEventInterceptor {

    override fun canEnrich(event: EnrichedEvent): Boolean {
        return event.root.type == EventType.MESSAGE
    }

    override fun enrich(event: EnrichedEvent) {
        monarchy.doWithRealm { realm ->

            if (event.root.eventId == null) {
                return@doWithRealm
            }

            val rootEntity = EventEntity.where(realm, eventId = event.root.eventId).findFirst()
                    ?: return@doWithRealm

            val roomMember = RoomMemberExtractor(realm, roomId).extractFrom(rootEntity)
            event.enrichWith(EventType.STATE_ROOM_MEMBER, roomMember)
        }
    }


}