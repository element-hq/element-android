package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.failure
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.FilterUtil
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PaginationRequest(private val roomAPI: RoomAPI,
                                 private val tokenChunkEventPersistor: TokenChunkEventPersistor,
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
        executeRequest<PaginationResponse> {
            apiCall = roomAPI.getRoomMessagesFrom(roomId, from, direction.value, limit, filter)
        }.flatMap { chunk ->
            tokenChunkEventPersistor
                    .insertInDb(chunk, roomId, direction)
                    .map { chunk }
        }
    }

}