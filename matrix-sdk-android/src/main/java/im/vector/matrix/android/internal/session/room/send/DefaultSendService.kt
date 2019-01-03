package im.vector.matrix.android.internal.session.room.send

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.session.room.send.EventFactory
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.helper.updateDisplayIndexes
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionAsync
import java.util.concurrent.TimeUnit

private const val SEND_WORK = "SEND_WORK"

internal class DefaultSendService(private val roomId: String,
                                  private val eventFactory: EventFactory,
                                  private val monarchy: Monarchy) : SendService {

    private val sendConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable {
        val event = eventFactory.createTextEvent(roomId, text)

        monarchy.tryTransactionAsync { realm ->
            val chunkEntity = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
                    ?: return@tryTransactionAsync
            chunkEntity.add(roomId, event, PaginationDirection.FORWARDS)
            chunkEntity.updateDisplayIndexes()
        }

        val sendContentWorkerParams = SendEventWorker.Params(roomId, event)
        val workData = WorkerParamsFactory.toData(sendContentWorkerParams)

        val sendWork = OneTimeWorkRequestBuilder<SendEventWorker>()
                .setConstraints(sendConstraints)
                .setInputData(workData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()

        return CancelableWork(sendWork.id)

    }

}
