package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.leftIfNull
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.findWithNextToken
import im.vector.matrix.android.internal.database.query.findWithPrevToken
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.model.PaginationDirection
import im.vector.matrix.android.internal.session.room.model.TokenChunkEvent
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaginationRequest(private val roomAPI: RoomAPI,
                        private val monarchy: Monarchy,
                        private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun execute(roomId: String,
                from: String?,
                direction: PaginationDirection,
                limit: Int = 10,
                filter: String? = null,
                callback: MatrixCallback<TokenChunkEvent>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val chunkOrFailure = execute(roomId, from, direction, limit, filter)
            chunkOrFailure.bimap({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(roomId: String,
                                from: String?,
                                direction: PaginationDirection,
                                limit: Int = 10,
                                filter: String?) = withContext(coroutineDispatchers.io) {

        if (from == null) {
            return@withContext Either.left(
                    Failure.Unknown(RuntimeException("From token shouldn't be null"))
            )
        }
        return@withContext executeRequest<TokenChunkEvent> {
            apiCall = roomAPI.getRoomMessagesFrom(roomId, from, direction.value, limit, filter)
        }.leftIfNull {
            Failure.Unknown(RuntimeException("TokenChunkEvent shouldn't be null"))
        }.flatMap { chunk ->
            try {
                insertInDb(chunk, roomId)
                Either.right(chunk)
            } catch (exception: Exception) {
                Either.Left(Failure.Unknown(exception))
            }
        }
    }

    private fun insertInDb(chunkEvent: TokenChunkEvent, roomId: String) {
        monarchy.runTransactionSync { realm ->
            val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                    ?: return@runTransactionSync

            val nextChunk = ChunkEntity.findWithPrevToken(realm, roomId, chunkEvent.nextToken)
            val prevChunk = ChunkEntity.findWithNextToken(realm, roomId, chunkEvent.prevToken)

            val mergedEvents = chunkEvent.chunk + chunkEvent.stateEvents
            val mergedEventIds = mergedEvents.filter { it.eventId != null }.map { it.eventId!! }
            val chunksOverlapped = ChunkEntity.findAllIncludingEvents(realm, mergedEventIds)
            val hasOverlapped = chunksOverlapped.isNotEmpty()

            val currentChunk = if (nextChunk != null) {
                nextChunk
            } else {
                ChunkEntity()
            }


            val eventsToAdd = ArrayList<EventEntity>()

            currentChunk.prevToken = chunkEvent.prevToken
            mergedEvents.forEach { event ->
                val eventEntity = event.asEntity().let {
                    realm.copyToRealmOrUpdate(it)
                }
                if (!currentChunk.events.contains(eventEntity)) {
                    eventsToAdd.add(0, eventEntity)
                }
            }

            if (prevChunk != null) {
                eventsToAdd.addAll(0, prevChunk.events)
                roomEntity.chunks.remove(prevChunk)

            } else if (hasOverlapped) {
                chunksOverlapped.forEach { chunk ->
                    chunk.events.forEach { event ->
                        if (!currentChunk.events.contains(event)) {
                            eventsToAdd.add(0, event)
                        }
                    }
                    roomEntity.chunks.remove(chunk)
                }
            }
            currentChunk.events.addAll(0, eventsToAdd)
            if (!roomEntity.chunks.contains(currentChunk)) {
                roomEntity.chunks.add(currentChunk)
            }
        }
    }

}