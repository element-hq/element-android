package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.matrix.android.api.session.room.model.MessageType
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class SendEventWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), KoinComponent {


    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val text: String
    )

    private val roomAPI by inject<RoomAPI>()

    override fun doWork(): Result {

        val sendWorkerParameters = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.FAILURE

        val fakeId = sendWorkerParameters.roomId + "-" + System.currentTimeMillis()
        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.send(
                    fakeId,
                    sendWorkerParameters.roomId,
                    EventType.MESSAGE,
                    MessageContent(MessageType.MSGTYPE_TEXT, sendWorkerParameters.text)
            )
        }
        return result.fold({ Result.RETRY }, { Result.SUCCESS })
    }


}