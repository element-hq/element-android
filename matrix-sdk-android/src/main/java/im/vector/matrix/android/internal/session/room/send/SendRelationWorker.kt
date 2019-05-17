package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.annotation.ReactionContent
import im.vector.matrix.android.api.session.room.model.annotation.ReactionInfo
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject

class SendRelationWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), MatrixKoinComponent {


    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val event: Event,
            val relationType: String? = null
    )

    private val roomAPI by inject<RoomAPI>()

    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<SendRelationWorker.Params>(inputData)
                ?: return Result.failure()

        val localEvent = params.event
        if (localEvent.eventId == null) {
            return Result.failure()
        }
        val relationContent = localEvent.content.toModel<ReactionContent>()
                ?: return Result.failure()
        val relatedEventId = relationContent.relatesTo?.eventId ?: return Result.failure()
        val relationType = (relationContent.relatesTo as? ReactionInfo)?.type ?: params.relationType
        ?: return Result.failure()

        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.sendRelation(
                    roomId = params.roomId,
                    parent_id = relatedEventId,
                    relationType = relationType,
                    eventType = localEvent.type,
                    content = localEvent.content
            )
        }
        return result.fold({ Result.retry() }, { Result.success() })
    }
}