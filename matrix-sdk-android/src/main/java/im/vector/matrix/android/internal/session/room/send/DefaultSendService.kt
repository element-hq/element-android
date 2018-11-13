package im.vector.matrix.android.internal.session.room.send

import androidx.work.*
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import java.util.concurrent.TimeUnit

private const val SEND_WORK = "SEND_WORK"

internal class DefaultSendService(private val roomId: String) : SendService {

    private val sendConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun sendTextMessage(text: String): Cancelable {

        val sendContentWorkerParams = SendEventWorker.Params(roomId, text)
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