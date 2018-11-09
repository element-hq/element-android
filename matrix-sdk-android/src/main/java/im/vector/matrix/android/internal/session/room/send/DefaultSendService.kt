package im.vector.matrix.android.internal.session.room.send

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.addManagedToChunk
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
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

    override fun sendTextMessage(text: String): Cancelable {
        val event = eventFactory.createTextEvent(roomId, text)

        monarchy.tryTransactionAsync { realm ->
            val chunkEntity = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
                    ?: return@tryTransactionAsync
            listOf(event).addManagedToChunk(chunkEntity)
        }

        val sendContentWorkerParams = SendEventWorker.Params(roomId, event)
        val workData = WorkerParamsFactory.toData(sendContentWorkerParams)

        val sendWork = OneTimeWorkRequestBuilder<SendEventWorker>()
                .setConstraints(sendConstraints)
                .setInputData(workData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000, TimeUnit.MILLISECONDS)
                .build()

        val work = WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()

        return CancelableWork(work)

    }

}