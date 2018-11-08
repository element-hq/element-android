package im.vector.matrix.android.internal.session.room.send

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.util.CancelableWork
import java.util.concurrent.TimeUnit

private const val SEND_WORK_NAME = "SEND_WORK_NAME"

internal class DefaultSendService(private val roomId: String) : SendService {

    private val sendConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun sendTextMessage(text: String): Cancelable {

        val data = mapOf(
                "roomId" to roomId,
                "text" to text
        )
        val workData = Data.Builder().putAll(data).build()

        val sendWork = OneTimeWorkRequestBuilder<SendContentWorker>()
                .setConstraints(sendConstraints)
                .setInputData(workData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000, TimeUnit.MILLISECONDS)
                .build()

        val work = WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK_NAME, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()

        return CancelableWork(work)

    }

}