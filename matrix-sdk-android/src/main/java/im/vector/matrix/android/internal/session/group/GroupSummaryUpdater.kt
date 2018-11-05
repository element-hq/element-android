package im.vector.matrix.android.internal.session.group

import android.arch.lifecycle.Observer
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal class GroupSummaryUpdater(private val monarchy: Monarchy,
                                   private val getGroupSummaryRequest: GetGroupSummaryRequest
) : Observer<Monarchy.ManagedChangeSet<GroupEntity>> {

    private var isStarted = AtomicBoolean(false)
    private val liveResults = monarchy.findAllManagedWithChanges { GroupEntity.where(it) }

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            liveResults.observeForever(this)
        }
    }

    fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            liveResults.removeObserver(this)
        }
    }

    // PRIVATE

    override fun onChanged(changeSet: Monarchy.ManagedChangeSet<GroupEntity>?) {
        if (changeSet == null) {
            return
        }
        val groups = changeSet.realmResults.map { it.asDomain() }
        val indexesToUpdate = changeSet.orderedCollectionChangeSet.changes + changeSet.orderedCollectionChangeSet.insertions
        updateGroupList(groups, indexesToUpdate)
    }


    private fun updateGroupList(groups: List<Group>, indexes: IntArray) {
        indexes.forEach {
            val group = groups[it]
            try {
                updateGroup(group)
            } catch (e: Exception) {
                Timber.e(e, "An error occured when updating room summaries")
            }
        }
    }

    private fun updateGroup(group: Group?) {
        if (group == null) {
            return
        }
        getGroupSummaryRequest.execute(group.groupId, object : MatrixCallback<GroupSummaryResponse> {})
    }

}