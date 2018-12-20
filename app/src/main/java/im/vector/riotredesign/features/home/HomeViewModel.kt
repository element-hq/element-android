package im.vector.riotredesign.features.home

import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx

class HomeViewModel(initialState: HomeViewState, private val session: Session) : BaseMvRxViewModel<HomeViewState>(initialState) {

    companion object : MvRxViewModelFactory<HomeViewState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: HomeViewState): HomeViewModel {
            val currentSession = Matrix.getInstance().currentSession
            return HomeViewModel(state, currentSession)
        }
    }

    init {
        observeRoomSummaries()
        observeGroupSummaries()
    }

    fun accept(action: HomeActions) {
        when (action) {
            is HomeActions.SelectRoom    -> handleSelectRoom(action)
            is HomeActions.SelectGroup   -> handleSelectGroup(action)
            is HomeActions.RoomDisplayed -> setState { copy(shouldOpenRoomDetail = false) }
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: HomeActions.SelectRoom) {
        withState { state ->
            if (state.selectedRoom?.roomId != action.roomSummary.roomId) {
                session.saveLastSelectedRoom(action.roomSummary)
                setState { copy(selectedRoom = action.roomSummary, shouldOpenRoomDetail = true) }
            }
        }
    }

    private fun handleSelectGroup(action: HomeActions.SelectGroup) {
        withState { state ->
            if (state.selectedGroup?.groupId != action.groupSummary.groupId) {
                setState { copy(selectedGroup = action.groupSummary) }
            } else {
                setState { copy(selectedGroup = null) }
            }
        }
    }

    private fun observeRoomSummaries() {
        session
                .rx().liveRoomSummaries()
                .execute { async ->

                    val summaries = async()
                    val directRooms = summaries?.filter { it.isDirect } ?: emptyList()
                    val groupRooms = summaries?.filter { !it.isDirect } ?: emptyList()

                    val selectedRoom = selectedRoom
                                       ?: session.lastSelectedRoom()
                                       ?: directRooms.firstOrNull()
                                       ?: groupRooms.firstOrNull()

                    copy(
                            asyncRooms = async,
                            directRooms = directRooms,
                            groupRooms = groupRooms,
                            selectedRoom = selectedRoom
                    )
                }
    }

    private fun observeGroupSummaries() {
        session
                .rx().liveGroupSummaries()
                .execute { async ->
                    copy(asyncGroups = async)
                }
    }
}