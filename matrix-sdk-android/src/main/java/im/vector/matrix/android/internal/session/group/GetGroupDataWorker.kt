package im.vector.matrix.android.internal.session.group

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import arrow.core.Try
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class GetGroupDataWorker(context: Context,
                                  workerParameters: WorkerParameters
) : Worker(context, workerParameters), KoinComponent {

    private val getGroupDataRequest by inject<GetGroupDataRequest>()

    override fun doWork(): Result {
        val params = GetGroupDataWorkerParams.fromData(inputData) ?: return Result.FAILURE
        val results = params.updateIndexes.map { index ->
            val groupId = params.groupIds[index]
            fetchGroupData(groupId)
        }
        val isSuccessful = results.none { it.isFailure() }
        return if (isSuccessful) Result.SUCCESS else Result.RETRY
    }

    private fun fetchGroupData(groupId: String): Try<Unit> {
        return getGroupDataRequest.getGroupData(groupId)
    }

}