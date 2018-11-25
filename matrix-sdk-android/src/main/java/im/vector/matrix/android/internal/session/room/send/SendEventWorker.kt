package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionSync
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class SendEventWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), KoinComponent {


    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val event: Event
    )

    private val roomAPI by inject<RoomAPI>()
    private val monarchy by inject<Monarchy>()

    override fun doWork(): Result {

        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.FAILURE

        if (params.event.eventId == null) {
            return Result.FAILURE
        }

        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.send(
                    params.event.eventId,
                    params.roomId,
                    params.event.type,
                    params.event.content
            )
        }
        result.flatMap { sendResponse ->
            monarchy.tryTransactionSync { realm ->
                val dummyEventEntity = EventEntity.where(realm, params.event.eventId).findFirst()
                dummyEventEntity?.eventId = sendResponse.eventId
            }
        }
        return result.fold({ Result.RETRY }, { Result.SUCCESS })
    }


}
