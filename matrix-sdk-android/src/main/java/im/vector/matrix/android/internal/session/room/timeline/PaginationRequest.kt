package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import arrow.core.failure
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.*
import im.vector.matrix.android.internal.legacy.util.FilterUtil
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.sync.StateEventsChunkHandler
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PaginationRequest(private val roomAPI: RoomAPI,
                                 private val monarchy: Monarchy,
                                 private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                 private val stateEventsChunkHandler: StateEventsChunkHandler) {

    fun execute(roomId: String,
                from: String?,
                direction: PaginationDirection,
                limit: Int = 10,
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
                                limit: Int = 10,
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

    private fun insertInDb(receivedChunk: TokenChunkEvent, roomId: String, direction: PaginationDirection): Try<TokenChunkEvent> {
        return monarchy
                .tryTransactionSync { realm ->
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                            ?: throw IllegalStateException("You shouldn't use this method without a room")

                    val currentChunk = ChunkEntity.findWithPrevToken(realm, roomId, receivedChunk.nextToken)
                            ?: realm.createObject()

                    currentChunk.prevToken = receivedChunk.prevToken

                    val prevChunk = ChunkEntity.findWithNextToken(realm, roomId, receivedChunk.prevToken)

                    val eventIds = receivedChunk.events.filter { it.eventId != null }.map { it.eventId!! }
                    val chunksOverlapped = realm.copyFromRealm(ChunkEntity.findAllIncludingEvents(realm, eventIds))
                    val hasOverlapped = chunksOverlapped.isNotEmpty()

                    var currentStateIndex = currentChunk.stateIndex(direction)
                    val incrementStateIndex = direction.incrementStateIndex

                    receivedChunk.events.forEach { event ->
                        currentChunk.add(event, currentStateIndex, direction)
                        if (EventType.isStateEvent(event.type)) {
                            currentStateIndex += incrementStateIndex
                        }
                    }

                    if (prevChunk != null) {
                        currentChunk.events.addAll(prevChunk.events)
                        roomEntity.chunks.remove(prevChunk)

                    } else if (hasOverlapped) {
                        chunksOverlapped.forEach { overlapped ->
                            overlapped.events.forEach { event ->
                                if (!currentChunk.events.fastContains(event)) {
                                    currentChunk.events.add(event)
                                }
                                if (EventType.isStateEvent(event.type)) {
                                    currentStateIndex += incrementStateIndex
                                }
                            }
                            currentChunk.prevToken = overlapped.prevToken
                            roomEntity.chunks.remove(overlapped)
                        }
                    }

                    if (!roomEntity.chunks.contains(currentChunk)) {
                        roomEntity.chunks.add(currentChunk)
                    }

                    currentChunk.updateStateIndex(currentStateIndex, direction)


                    val stateEventsChunk = stateEventsChunkHandler.handle(realm, roomId, receivedChunk.stateEvents, direction)
                    if (!roomEntity.chunks.contains(stateEventsChunk)) {
                        roomEntity.chunks.add(stateEventsChunk)
                    }
                }
                .map { receivedChunk }
    }

}