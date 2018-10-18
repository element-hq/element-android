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
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.findWithNextToken
import im.vector.matrix.android.internal.database.query.findWithPrevToken
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
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
                from: String? = null,
                direction: String,
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
                                from: String? = null,
                                direction: String,
                                limit: Int = 10,
                                filter: String? = null) = withContext(coroutineDispatchers.io) {

        if (from == null) {
            return@withContext Either.left(Failure.Unknown(RuntimeException("From token can't be null")))
        }
        executeRequest<TokenChunkEvent> {
            apiCall = roomAPI.getRoomMessagesFrom(roomId, from, direction, limit, filter)
        }.leftIfNull {
            Failure.Unknown(RuntimeException("TokenChunkEvent shouldn't be null"))
        }.flatMap {
            try {
                insertInDb(it, roomId)
                Either.right(it)
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

            val currentChunk: ChunkEntity
            if (nextChunk != null) {
                currentChunk = nextChunk
            } else {
                currentChunk = ChunkEntity()
            }
            currentChunk.prevToken = chunkEvent.prevToken
            mergedEvents.forEach { event ->
                val eventEntity = event.asEntity().let {
                    realm.copyToRealmOrUpdate(it)
                }
                if (!currentChunk.events.contains(eventEntity)) {
                    currentChunk.events.add(eventEntity)
                }
            }

            if (prevChunk != null) {
                currentChunk.events.addAll(prevChunk.events)
                roomEntity.chunks.remove(prevChunk)

            } else if (chunksOverlapped.isNotEmpty()) {
                chunksOverlapped.forEach { chunk ->
                    chunk.events.forEach { event ->
                        if (!currentChunk.events.contains(event)) {
                            currentChunk.events.add(event)
                        }
                    }
                    roomEntity.chunks.remove(chunk)
                }
            }

            if (!roomEntity.chunks.contains(currentChunk)) {
                roomEntity.chunks.add(currentChunk)
            }
        }
    }

}