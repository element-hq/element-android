package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.helper.*
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.tryTransactionSync


internal class TokenChunkEventPersistor(private val monarchy: Monarchy) {

    fun insertInDb(receivedChunk: TokenChunkEvent,
                   roomId: String,
                   direction: PaginationDirection): Try<Unit> {

        return monarchy
                .tryTransactionSync { realm ->
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                            ?: throw IllegalStateException("You shouldn't use this method without a room")

                    val nextToken: String?
                    val prevToken: String?
                    if (direction == PaginationDirection.FORWARDS) {
                        nextToken = receivedChunk.end
                        prevToken = receivedChunk.start
                    } else {
                        nextToken = receivedChunk.start
                        prevToken = receivedChunk.end
                    }
                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = nextToken)

                    // The current chunk is the one we will keep all along the merge process.
                    // We try to look for a chunk next to the token,
                    // otherwise we create a whole new one

                    var currentChunk = if (direction == PaginationDirection.FORWARDS) {
                        prevChunk?.apply { this.nextToken = nextToken }
                                ?: ChunkEntity.create(realm, prevToken, nextToken)
                    } else {
                        nextChunk?.apply { this.prevToken = prevToken }
                                ?: ChunkEntity.create(realm, prevToken, nextToken)
                    }

                    currentChunk.addAll(roomId, receivedChunk.events, direction, isUnlinked = currentChunk.isUnlinked())

                    // Then we merge chunks if needed
                    if (currentChunk != prevChunk && prevChunk != null) {
                        currentChunk = handleMerge(roomEntity, direction, currentChunk, prevChunk)
                    } else if (currentChunk != nextChunk && nextChunk != null) {
                        currentChunk = handleMerge(roomEntity, direction, currentChunk, nextChunk)
                    } else {
                        val newEventIds = receivedChunk.events.mapNotNull { it.eventId }
                        ChunkEntity
                                .findAllIncludingEvents(realm, newEventIds)
                                .filter { it != currentChunk }
                                .forEach { overlapped ->
                                    currentChunk = handleMerge(roomEntity, direction, currentChunk, overlapped)
                                }
                    }
                    roomEntity.addOrUpdate(currentChunk)
                    roomEntity.addStateEvents(receivedChunk.stateEvents, isUnlinked = currentChunk.isUnlinked())
                }
    }

    private fun handleMerge(roomEntity: RoomEntity,
                            direction: PaginationDirection,
                            currentChunk: ChunkEntity,
                            otherChunk: ChunkEntity): ChunkEntity {

        // We always merge the bottom chunk into top chunk, so we are always merging backwards
        return if (direction == PaginationDirection.BACKWARDS) {
            currentChunk.merge(otherChunk, PaginationDirection.BACKWARDS)
            roomEntity.deleteOnCascade(otherChunk)
            currentChunk
        } else {
            otherChunk.merge(currentChunk, PaginationDirection.BACKWARDS)
            roomEntity.deleteOnCascade(currentChunk)
            otherChunk
        }
    }

}