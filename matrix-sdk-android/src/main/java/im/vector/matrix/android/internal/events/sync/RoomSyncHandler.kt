package im.vector.matrix.android.internal.events.sync

import im.vector.matrix.android.api.events.Event
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.getForId
import im.vector.matrix.android.internal.events.sync.data.RoomSync
import io.objectbox.Box
import io.objectbox.BoxStore


class RoomSyncHandler(
        boxStore: BoxStore
) {

    private val eventBox: Box<EventEntity> = boxStore.boxFor(EventEntity::class.java)
    private val chunkBox: Box<ChunkEntity> = boxStore.boxFor(ChunkEntity::class.java)
    private val roomBox: Box<RoomEntity> = boxStore.boxFor(RoomEntity::class.java)

    fun handleJoinedRooms(roomSyncByRoom: Map<String, RoomSync>?) {
        if (roomSyncByRoom == null) {
            return
        }
        val roomEntities = ArrayList<RoomEntity>()
        roomSyncByRoom.forEach {
            val roomEntity = handleJoinedRoom(it.key, it.value)
            roomEntities.add(roomEntity)
        }
        roomBox.put(roomEntities)
    }

    private fun handleJoinedRoom(roomId: String, roomSync: RoomSync): RoomEntity {
        val roomEntity = RoomEntity.getForId(roomBox, roomId) ?: RoomEntity().apply { this.roomId = roomId }
        if (roomEntity.membership == RoomEntity.Membership.INVITED) {
            roomEntity.chunks
                    .map { it.events }
                    .forEach { eventBox.remove(it) }
            chunkBox.remove(roomEntity.chunks)
        }
        roomEntity.membership = RoomEntity.Membership.JOINED
        if (roomSync.timeline != null) {
            val chunkEntity = eventListToChunk(roomSync.timeline.events, roomSync.timeline.prevBatch)
            roomEntity.chunks.add(chunkEntity)
        }

        if (roomSync.state != null) {
            val chunkEntity = eventListToChunk(roomSync.state.events)
            roomEntity.chunks.add(chunkEntity)
        }
        return roomEntity
    }

    private fun eventListToChunk(eventList: List<Event>,
                                 prevToken: String? = null,
                                 nextToken: String? = null): ChunkEntity {
        val chunkEntity = ChunkEntity()
        chunkEntity.prevToken = prevToken
        chunkEntity.nextToken = nextToken
        eventList
                .map { event -> EventMapper.map(event) }
                .forEach { chunkEntity.events.add(it) }
        return chunkEntity
    }

}