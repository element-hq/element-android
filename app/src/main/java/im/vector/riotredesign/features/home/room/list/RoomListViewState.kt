package im.vector.riotredesign.features.home.room.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class RoomListViewState(
        val asyncRooms: Async<RoomSummaries> = Uninitialized,
        val selectedRoomId: String? = null
) : MvRxState

data class RoomSummaries(
        val directRooms: List<RoomSummary>,
        val groupRooms: List<RoomSummary>
)

fun RoomSummaries?.isNullOrEmpty(): Boolean {
    return this == null || (directRooms.isEmpty() && groupRooms.isEmpty())
}