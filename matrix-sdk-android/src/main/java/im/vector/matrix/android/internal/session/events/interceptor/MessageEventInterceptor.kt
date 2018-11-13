package im.vector.matrix.android.internal.session.events.interceptor

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.where


internal class MessageEventInterceptor(val monarchy: Monarchy) : EnrichedEventInterceptor {

    override fun canEnrich(event: EnrichedEvent): Boolean {
        return event.root.type == EventType.MESSAGE
    }

    override fun enrich(roomId: String, event: EnrichedEvent) {
        monarchy.doWithRealm { realm ->

            if (event.root.eventId == null) {
                return@doWithRealm
            }

            val rootEntity = EventEntity.where(realm, eventId = event.root.eventId).findFirst()
                             ?: return@doWithRealm

            val roomMember = EventEntity
                    .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                    .equalTo(EventEntityFields.STATE_KEY, event.root.sender)
                    .last(from = rootEntity.stateIndex)
                    ?.asDomain()
            event.enrichWith(roomMember)
        }
    }


}