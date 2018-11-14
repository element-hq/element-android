package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findWithNextToken
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Realm
import io.realm.kotlin.createObject

internal class StateEventsChunkHandler {

    fun handle(realm: Realm, roomId: String, stateEvents: List<Event>, direction: PaginationDirection): ChunkEntity {
        val chunkEntity = ChunkEntity.findWithNextToken(realm, roomId, DBConstants.STATE_EVENTS_CHUNK_TOKEN)
                ?: realm.createObject<ChunkEntity>()
                        .apply {
                            prevToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                            nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN
                            nextStateIndex = Int.MIN_VALUE / 2
                            prevStateIndex = Int.MIN_VALUE / 2
                        }

        val stateIndex = chunkEntity.stateIndex(direction) + direction.incrementStateIndex
        stateEvents.forEach { event ->
            chunkEntity.add(event, stateIndex, PaginationDirection.FORWARDS)
        }
        chunkEntity.updateStateIndex(stateIndex, direction)
        return chunkEntity
    }


}


