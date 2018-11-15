package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Realm
import io.realm.kotlin.createObject

internal class StateEventsChunkHandler {

    fun handle(realm: Realm, roomId: String, stateEvents: List<Event>): ChunkEntity {
        val chunkEntity = ChunkEntity.find(realm, roomId, nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN)
                          ?: realm.createObject<ChunkEntity>()
                                  .apply {
                                      prevToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                                      nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                                      nextStateIndex = Int.MIN_VALUE
                                      prevStateIndex = Int.MIN_VALUE
                                  }

        // We always consider going forwards as data from server are the most recent
        chunkEntity.addAll(stateEvents, direction = PaginationDirection.FORWARDS, updateStateIndex = false)
        return chunkEntity
    }


}


