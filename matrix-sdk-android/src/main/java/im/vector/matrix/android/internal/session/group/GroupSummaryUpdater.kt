package im.vector.matrix.android.internal.session.group

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.RealmResults

private const val GET_GROUP_DATA_WORKER = "GET_GROUP_DATA_WORKER"

internal class GroupSummaryUpdater(monarchy: Monarchy
) : RealmLiveEntityObserver<GroupEntity>(monarchy) {

    override val query = Monarchy.Query<GroupEntity> { GroupEntity.where(it) }

    private val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun process(results: RealmResults<GroupEntity>, updateIndexes: IntArray, deletionIndexes: IntArray) {
        val groupIds = results.map { it.groupId }
        val getGroupDataWorkerParams = GetGroupDataWorkerParams(groupIds, updateIndexes.toList(), deletionIndexes.toList())
        val workData = getGroupDataWorkerParams.toData()

        val sendWork = OneTimeWorkRequestBuilder<GetGroupDataWorker>()
                .setInputData(workData)
                .setConstraints(workConstraints)
                .build()

        WorkManager.getInstance()
                .beginUniqueWork(GET_GROUP_DATA_WORKER, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()
    }


}