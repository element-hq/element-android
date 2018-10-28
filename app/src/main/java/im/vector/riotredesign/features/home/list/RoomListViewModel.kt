package im.vector.riotredesign.features.home.list

import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import org.koin.android.ext.android.get

class RoomListViewModel(initialState: RoomListViewState,
                        private val session: Session
) : BaseMvRxViewModel<RoomListViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomListViewState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: RoomListViewState): RoomListViewModel {
            val matrix = activity.get<Matrix>()
            val currentSession = matrix.currentSession!!
            return RoomListViewModel(state, currentSession)
        }
    }

    init {
        observeRoomSummaries()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom -> handleSelectRoom(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) {
        session.saveLastSelectedRoom(action.roomSummary)
        setState { copy(selectedRoom = action.roomSummary) }
    }

    private fun observeRoomSummaries() {
        session
                .rx().liveRoomSummaries()
                .execute {
                    val selectedRoom = selectedRoom
                            ?: session.lastSelectedRoom()
                            ?: it.invoke()?.firstOrNull()

                    copy(roomSummaries = it, selectedRoom = selectedRoom)
                }
    }

}