package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.find
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

internal class GetContextOfEventRequest(private val roomAPI: RoomAPI,
                                        private val monarchy: Monarchy,
                                        private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    fun execute(roomId: String,
                eventId: String,
                callback: MatrixCallback<EventContextResponse>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val filter = FilterUtil.createRoomEventFilter(true)?.toJSONString()
            val contextOrFailure = execute(roomId, eventId, filter)
            contextOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(roomId: String,
                                eventId: String,
                                filter: String?) = withContext(coroutineDispatchers.io) {

        executeRequest<EventContextResponse> {
            apiCall = roomAPI.getContextOfEvent(roomId, eventId, 0, filter)
        }.flatMap { response ->
            insertInDb(response, roomId)
        }
    }

    private fun insertInDb(response: EventContextResponse, roomId: String): Try<EventContextResponse> {
        return monarchy
                .tryTransactionSync { realm ->
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                            ?: throw IllegalStateException("You shouldn't use this method without a room")

                    val currentChunk = realm.createObject<ChunkEntity>().apply {
                        prevToken = response.prevToken
                        nextToken = response.nextToken
                    }

                    currentChunk.addOrUpdate(response.event, PaginationDirection.FORWARDS, isUnlinked = true)
                    // Now, handles chunk merge
                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = response.prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = response.nextToken)

                    if (prevChunk != null) {
                        currentChunk.merge(prevChunk, PaginationDirection.BACKWARDS)
                        roomEntity.deleteOnCascade(prevChunk)
                    }
                    if (nextChunk != null) {
                        currentChunk.merge(nextChunk, PaginationDirection.FORWARDS)
                        roomEntity.deleteOnCascade(nextChunk)
                    }
                    roomEntity.addOrUpdate(currentChunk)
                    roomEntity.addStateEvents(response.stateEvents, stateIndex = Int.MIN_VALUE, isUnlinked = true)
                }
                .map { response }
    }


}