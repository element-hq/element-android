package im.vector.riotredesign.features.home

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class HomeViewState(
        val asyncRooms: Async<List<RoomSummary>> = Uninitialized,
        val directRooms: List<RoomSummary> = emptyList(),
        val groupRooms: List<RoomSummary> = emptyList(),
        val selectedRoomId: String? = null,
        val selectedEventId: String? = null,
        val shouldOpenRoomDetail: Boolean = true,
        val asyncGroups: Async<List<GroupSummary>> = Uninitialized,
        val selectedGroup: GroupSummary? = null
) : MvRxState