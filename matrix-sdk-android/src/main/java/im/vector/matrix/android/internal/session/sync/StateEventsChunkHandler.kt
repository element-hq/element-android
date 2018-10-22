package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findWithNextToken
import io.realm.Realm

class StateEventsChunkHandler {

    fun handle(realm: Realm, roomId: String, stateEvents: List<Event>): ChunkEntity {
        val chunkEntity = ChunkEntity.findWithNextToken(realm, roomId, DBConstants.STATE_EVENTS_CHUNK_TOKEN)
                          ?: ChunkEntity(prevToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN, nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN)

        stateEvents.forEach { event ->
            val eventEntity = event.asEntity().let {
                realm.copyToRealmOrUpdate(it)
            }
            if (!chunkEntity.events.contains(eventEntity)) {
                chunkEntity.events.add(eventEntity)
            }
        }
        return chunkEntity
    }


}