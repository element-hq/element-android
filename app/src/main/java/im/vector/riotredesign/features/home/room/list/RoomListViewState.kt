package im.vector.riotredesign.features.home.room.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary

data class RoomListViewState(
        val roomSummaries: Async<List<RoomSummary>> = Uninitialized,
        val selectedRoom: RoomSummary? = null
) : MvRxState {

    var showLastSelectedRoom: Boolean = true
        private set
        get() {
            if (field) {
                field = false
                return true
            }
            return false
        }


}