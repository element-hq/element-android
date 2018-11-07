package im.vector.matrix.android.internal.session.group

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.RealmResults

internal class GroupSummaryUpdater(monarchy: Monarchy,
                                   private val getGroupDataRequest: GetGroupDataRequest
) : RealmLiveEntityObserver<GroupEntity>(monarchy) {

    override val query = Monarchy.Query<GroupEntity> { GroupEntity.where(it) }

    override fun process(results: RealmResults<GroupEntity>, indexes: IntArray) {
        indexes.forEach { index ->
            val data = results[index]
            fetchGroupData(data)
        }
    }

    private fun fetchGroupData(data: GroupEntity?) {
        if (data == null) {
            return
        }
        getGroupDataRequest.execute(data.groupId, object : MatrixCallback<Boolean> {})
    }

}