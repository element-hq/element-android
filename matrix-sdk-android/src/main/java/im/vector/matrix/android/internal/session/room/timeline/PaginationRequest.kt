package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import arrow.core.failure
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.isUnlinked
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.legacy.util.FilterUtil
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PaginationRequest(private val roomAPI: RoomAPI,
                                 private val monarchy: Monarchy,
                                 private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    fun execute(roomId: String,
                from: String?,
                direction: PaginationDirection,
                limit: Int,
                callback: MatrixCallback<TokenChunkEvent>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val filter = FilterUtil.createRoomEventFilter(true)?.toJSONString()
            val chunkOrFailure = execute(roomId, from, direction, limit, filter)
            chunkOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(roomId: String,
                                from: String?,
                                direction: PaginationDirection,
                                limit: Int,
                                filter: String?) = withContext(coroutineDispatchers.io) {

        if (from == null) {
            return@withContext RuntimeException("From token shouldn't be null").failure<TokenChunkEvent>()
        }
        executeRequest<TokenChunkEvent> {
            apiCall = roomAPI.getRoomMessagesFrom(roomId, from, direction.value, limit, filter)
        }.flatMap { chunk ->
            insertInDb(chunk, roomId, direction)
        }
    }

    private fun insertInDb(receivedChunk: TokenChunkEvent,
                           roomId: String,
                           direction: PaginationDirection): Try<TokenChunkEvent> {
        return monarchy
                .tryTransactionSync { realm ->
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                                     ?: throw IllegalStateException("You shouldn't use this method without a room")

                    // We create a new chunk with prev and next token as a base
                    // In case of permalink, we may not encounter other chunks, so it can be added
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
                        newChunk.deleteOnCascade()
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
                                    currentChunk = overlapped
                                }
                            }

                    roomEntity.addOrUpdate(currentChunk)

                    // TODO : there is an issue with the pagination sending unwanted room member events
                    val isUnlinked = currentChunk.isUnlinked()
                    roomEntity.addStateEvents(receivedChunk.stateEvents, isUnlinked = isUnlinked)
                }
                .map { receivedChunk }
    }


}