package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import arrow.core.failure
import im.vector.matrix.android.internal.Task
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.FilterUtil


internal interface PaginationTask : Task<PaginationTask.Params, TokenChunkEvent> {

    data class Params(
            val roomId: String,
            val from: String?,
            val direction: PaginationDirection,
            val limit: Int
    )

}

internal class DefaultPaginationTask(private val roomAPI: RoomAPI,
                                     private val tokenChunkEventPersistor: TokenChunkEventPersistor
) : PaginationTask {

    override fun execute(params: PaginationTask.Params): Try<TokenChunkEvent> {
        if (params.from == null) {
            return RuntimeException("From token shouldn't be null").failure()
        }
        val filter = FilterUtil.createRoomEventFilter(true)?.toJSONString()
        return executeRequest<PaginationResponse> {
            apiCall = roomAPI.getRoomMessagesFrom(params.roomId, params.from, params.direction.value, params.limit, filter)
        }.flatMap { chunk ->
            tokenChunkEventPersistor
                    .insertInDb(chunk, params.roomId, params.direction)
                    .map { chunk }
        }
    }
}