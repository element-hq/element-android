package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.legacy.util.FilterUtil
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GetContextOfEventRequest(private val roomAPI: RoomAPI,
                                        private val tokenChunkEventPersistor: TokenChunkEventPersistor,
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
            tokenChunkEventPersistor.insertInDb(response, roomId, PaginationDirection.BACKWARDS).map { response }
        }
    }


}