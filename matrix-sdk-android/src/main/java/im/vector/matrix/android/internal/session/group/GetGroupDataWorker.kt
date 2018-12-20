package im.vector.matrix.android.internal.session.group

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import arrow.core.Try
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject

internal class GetGroupDataWorker(context: Context,
                                  workerParameters: WorkerParameters
) : Worker(context, workerParameters), MatrixKoinComponent {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val groupIds: List<String>,
            val updateIndexes: List<Int>,
            val deletionIndexes: List<Int>
    )

    private val getGroupDataTask by inject<GetGroupDataTask>()

    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.failure()

        val results = params.updateIndexes.map { index ->
            val groupId = params.groupIds[index]
            fetchGroupData(groupId)
        }
        val isSuccessful = results.none { it.isFailure() }
        return if (isSuccessful) Result.success() else Result.retry()
    }

    private fun fetchGroupData(groupId: String): Try<Unit> {
        return getGroupDataTask.execute(GetGroupDataTask.Params(groupId))
    }

}