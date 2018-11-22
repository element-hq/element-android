package im.vector.matrix.android.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GetEventRequest(private val roomAPI: RoomAPI,
                               private val monarchy: Monarchy,
                               private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    fun execute(roomId: String,
                eventId: String,
                callback: MatrixCallback<Event>
    ): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val eventOrFailure = execute(roomId, eventId)
            eventOrFailure.fold({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

    private suspend fun execute(roomId: String,
                                eventId: String) = withContext(coroutineDispatchers.io) {

        executeRequest<Event> {
            apiCall = roomAPI.getEvent(roomId, eventId)
        }
    }

}