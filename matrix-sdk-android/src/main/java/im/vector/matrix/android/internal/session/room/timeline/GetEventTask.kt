package im.vector.matrix.android.internal.session.room.timeline

import arrow.core.Try
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.Task
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI

internal class GetEventTask(private val roomAPI: RoomAPI
) : Task<GetEventTask.Params, Event> {

    internal data class Params(
            val roomId: String,
            val eventId: String
    )

    override fun execute(params: Params): Try<Event> {
        return executeRequest {
            apiCall = roomAPI.getEvent(params.roomId, params.eventId)
        }
    }
}