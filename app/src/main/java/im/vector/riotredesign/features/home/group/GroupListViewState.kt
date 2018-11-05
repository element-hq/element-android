package im.vector.riotredesign.features.home.group

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class GroupListViewState(
        val async: Async<List<GroupSummary>> = Uninitialized,
        val selectedGroup: GroupSummary? = null
) : MvRxState