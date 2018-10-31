package im.vector.matrix.android.internal.session.events.interceptor

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.interceptor.EnrichedEventInterceptor
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import io.realm.Sort
import java.util.*


class IsLastEventInterceptor(val monarchy: Monarchy) : EnrichedEventInterceptor {

    override fun canEnrich(event: EnrichedEvent): Boolean {
        return true
    }

    override fun enrich(roomId: String, event: EnrichedEvent) {
        monarchy.doWithRealm { realm ->
            if (event.root.eventId == null) {
                return@doWithRealm
            }
            val eventEntity = EventEntity.where(realm, event.root.eventId).findFirst()
            val chunkEntity = ChunkEntity.findAllIncludingEvents(realm, Collections.singletonList(event.root.eventId)).firstOrNull()
            if (eventEntity == null || chunkEntity == null) {
                return@doWithRealm
            }
            val sortedChunkEvents = chunkEntity.events.where().sort(EventEntityFields.ORIGIN_SERVER_TS, Sort.ASCENDING).findAll()
            val isLastEvent = chunkEntity.prevToken == null && sortedChunkEvents?.indexOf(eventEntity) == 0
            event.enrichWith(EnrichedEvent.IS_LAST_EVENT, isLastEvent)
        }
    }

}