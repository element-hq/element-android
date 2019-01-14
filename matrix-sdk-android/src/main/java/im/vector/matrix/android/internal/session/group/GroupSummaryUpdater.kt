package im.vector.matrix.android.internal.session.group

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.WorkerParamsFactory

private const val GET_GROUP_DATA_WORKER = "GET_GROUP_DATA_WORKER"

internal class GroupSummaryUpdater(monarchy: Monarchy
) : RealmLiveEntityObserver<GroupEntity>(monarchy) {

    override val query = Monarchy.Query<GroupEntity> { GroupEntity.where(it) }

    private val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun process(inserted: List<GroupEntity>, updated: List<GroupEntity>, deleted: List<GroupEntity>) {
        val newGroupIds = inserted.map { it.groupId }
        val getGroupDataWorkerParams = GetGroupDataWorker.Params(newGroupIds)
        val workData = WorkerParamsFactory.toData(getGroupDataWorkerParams)

        val sendWork = OneTimeWorkRequestBuilder<GetGroupDataWorker>()
                .setInputData(workData)
                .setConstraints(workConstraints)
                .build()

        WorkManager.getInstance()
                .beginUniqueWork(GET_GROUP_DATA_WORKER, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()
    }


}