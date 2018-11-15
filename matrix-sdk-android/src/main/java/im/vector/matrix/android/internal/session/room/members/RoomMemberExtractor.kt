package im.vector.matrix.android.internal.session.room.members

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.DBConstants
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findMostSuitableStateEvent
import im.vector.matrix.android.internal.database.query.last
import io.realm.Realm
import io.realm.RealmQuery

internal class RoomMemberExtractor(private val realm: Realm,
                                   private val roomId: String) {

    fun extractFrom(event: EventEntity): RoomMember? {
        val stateIndex = event.stateIndex
        val chunkEntity = event.chunk?.firstOrNull()
                          ?: throw IllegalStateException("An event should be attached to a chunk")

        // First of all, try to get the most suitable state event coming from a chunk
        val roomMember = buildQuery(chunkEntity, event.sender)
                .findMostSuitableStateEvent(stateIndex = stateIndex)
                ?.asDomain()
                ?.pickContent<RoomMember>(stateIndex)

        if (roomMember != null) {
            return roomMember
        }

        // If the content is null, we try get the last state event coming from a state events chunk
        val stateChunkEntity = ChunkEntity.find(realm, roomId, nextToken = DBConstants.STATE_EVENTS_CHUNK_TOKEN)
                               ?: return null

        return buildQuery(stateChunkEntity, event.sender)
                .last()
                ?.asDomain()
                ?.content()
    }

    private fun buildQuery(chunk: ChunkEntity,
                           sender: String?): RealmQuery<EventEntity> {
        return chunk.events
                .where()
                .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_MEMBER)
                .equalTo(EventEntityFields.STATE_KEY, sender)

    }


}