package im.vector.matrix.android.api.session.group

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.session.group.model.GroupSummary

interface GroupService {

    fun getGroup(groupId: String): Group?

    fun liveGroupSummaries(): LiveData<List<GroupSummary>>
}