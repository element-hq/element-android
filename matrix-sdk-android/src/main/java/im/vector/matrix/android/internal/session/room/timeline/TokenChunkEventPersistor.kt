package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.helper.*
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject


internal class TokenChunkEventPersistor(private val monarchy: Monarchy) {

    fun insertInDb(receivedChunk: TokenChunkEvent,
                   roomId: String,
                   direction: PaginationDirection): Try<Unit> {

        return monarchy
                .tryTransactionSync { realm ->
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                            ?: throw IllegalStateException("You shouldn't use this method without a room")

                    // We create a new chunk with prev and next token as a base
                    // In case of permalink, we may not encounter other chunks, so it can be added
                    // By default, it's an unlinked chunk
                    val newChunk = realm.createObject<ChunkEntity>().apply {
                        prevToken = receivedChunk.prevToken
                        nextToken = receivedChunk.nextToken
                    }
                    newChunk.addAll(receivedChunk.events, direction, isUnlinked = true)

                    // The current chunk is the one we will keep all along the merge process.
                    var currentChunk = newChunk
                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = receivedChunk.prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = receivedChunk.nextToken)

                    // We always merge the bottom chunk into top chunk, so we are always merging backwards
                    if (prevChunk != null) {
                        newChunk.merge(prevChunk, PaginationDirection.BACKWARDS)
                        roomEntity.deleteOnCascade(prevChunk)
                    }
                    if (nextChunk != null) {
                        nextChunk.merge(newChunk, PaginationDirection.BACKWARDS)
                        roomEntity.deleteOnCascade(newChunk)
                        currentChunk = nextChunk
                    }
                    val newEventIds = receivedChunk.events.mapNotNull { it.eventId }
                    ChunkEntity
                            .findAllIncludingEvents(realm, newEventIds)
                            .filter { it != currentChunk }
                            .forEach { overlapped ->
                                if (direction == PaginationDirection.BACKWARDS) {
                                    currentChunk.merge(overlapped, PaginationDirection.BACKWARDS)
                                    roomEntity.deleteOnCascade(overlapped)
                                } else {
                                    overlapped.merge(currentChunk, PaginationDirection.BACKWARDS)
                                    roomEntity.deleteOnCascade(currentChunk)
                                    currentChunk = overlapped
                                }
                            }
                    roomEntity.addOrUpdate(currentChunk)

                    // TODO : there is an issue with the pagination sending unwanted room member events
                    val isUnlinked = currentChunk.isUnlinked()
                    roomEntity.addStateEvents(receivedChunk.stateEvents, isUnlinked = isUnlinked)
                }
    }


}