package im.vector.riotredesign.features.home.room.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class RoomListViewState(
        val async: Async<List<RoomSummary>> = Uninitialized,
        val directRooms: List<RoomSummary> = emptyList(),
        val groupRooms: List<RoomSummary> = emptyList(),
        val selectedRoom: RoomSummary? = null,
        val shouldOpenRoomDetail: Boolean = true
) : MvRxState