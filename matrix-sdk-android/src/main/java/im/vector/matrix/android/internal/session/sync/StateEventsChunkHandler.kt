package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.helper.addManagedToChunk
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findWithNextToken
import io.realm.Realm
import io.realm.kotlin.createObject

class StateEventsChunkHandler {

    fun handle(realm: Realm, roomId: String, stateEvents: List<Event>): ChunkEntity {
        val chunkEntity = ChunkEntity.findWithNextToken(realm, roomId, DBConstants.STATE_EVENTS_CHUNK_TOKEN)
                          ?: realm.createObject<ChunkEntity>()
                                  .apply {
                                      prevToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                                      nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                                  }

        stateEvents.addManagedToChunk(chunkEntity)
        return chunkEntity
    }


}


