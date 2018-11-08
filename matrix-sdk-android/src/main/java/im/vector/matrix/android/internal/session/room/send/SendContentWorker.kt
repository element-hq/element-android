package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.matrix.android.api.session.room.model.MessageType
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class SendContentWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), KoinComponent {

    private val roomAPI by inject<RoomAPI>()

    override fun doWork(): Result {

        val roomId = inputData.getString("roomId")
        val text = inputData.getString("text")

        val fakeId = roomId + "-" + System.currentTimeMillis()

        if (roomId == null || text == null) {
            return Result.FAILURE
        }

        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.send(fakeId, roomId, EventType.MESSAGE, MessageContent(MessageType.MSGTYPE_TEXT, text))
        }
        return result.fold({ Result.RETRY }, { Result.SUCCESS })
    }


}