package im.vector.riotredesign.features.home.group

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.group.model.GroupSummary

data class GroupListViewState(
        val asyncGroups: Async<List<GroupSummary>> = Uninitialized,
        val selectedGroup: GroupSummary? = null
) : MvRxState