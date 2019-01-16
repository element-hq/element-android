package im.vector.matrix.android.internal.session.group

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where

internal class DefaultGroupService(private val monarchy: Monarchy) : GroupService {

    override fun getGroup(groupId: String): Group? {
        return null
    }

    override fun liveGroupSummaries(): LiveData<List<GroupSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> GroupSummaryEntity.where(realm).isNotEmpty(GroupSummaryEntityFields.DISPLAY_NAME) },
                { it.asDomain() }
        )
    }

}