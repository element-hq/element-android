package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.FilterUtil

internal interface GetContextOfEventTask : Task<GetContextOfEventTask.Params, TokenChunkEvent> {

    data class Params(
            val roomId: String,
            val eventId: String
    )

}

internal class DefaultGetContextOfEventTask(private val roomAPI: RoomAPI,
                                            private val tokenChunkEventPersistor: TokenChunkEventPersistor
) : GetContextOfEventTask {

    override fun execute(params: GetContextOfEventTask.Params): Try<EventContextResponse> {
        val filter = FilterUtil.createRoomEventFilter(true)?.toJSONString()
        return executeRequest<EventContextResponse> {
            apiCall = roomAPI.getContextOfEvent(params.roomId, params.eventId, 0, filter)
        }.flatMap { response ->
            tokenChunkEventPersistor.insertInDb(response, params.roomId, PaginationDirection.BACKWARDS).map { response }
        }
    }

}