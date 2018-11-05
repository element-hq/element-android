package im.vector.matrix.android.api.session.group

import android.arch.lifecycle.LiveData
import im.vector.matrix.android.api.session.group.model.GroupSummary

interface GroupService {

    fun getGroup(groupId: String): Group?

    fun liveGroupSummaries(): LiveData<List<GroupSummary>>
}