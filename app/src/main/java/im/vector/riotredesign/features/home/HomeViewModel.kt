package im.vector.riotredesign.features.home

import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.features.home.room.list.RoomSelectionRepository
import org.koin.android.ext.android.get

class HomeViewModel(initialState: HomeViewState,
                    private val session: Session,
                    private val roomSelectionRepository: RoomSelectionRepository) : BaseMvRxViewModel<HomeViewState>(initialState) {

    companion object : MvRxViewModelFactory<HomeViewState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: HomeViewState): HomeViewModel {
            val currentSession = Matrix.getInstance().currentSession
            val roomSelectionRepository = activity.get<RoomSelectionRepository>()
            return HomeViewModel(state, currentSession, roomSelectionRepository)
        }
    }

    init {
        observeRoomSummaries()
        observeGroupSummaries()
    }

    fun accept(action: HomeActions) {
        when (action) {
            is HomeActions.SelectRoom -> handleSelectRoom(action)
            is HomeActions.SelectGroup -> handleSelectGroup(action)
            is HomeActions.RoomDisplayed -> setState { copy(shouldOpenRoomDetail = false) }
            is HomeActions.PermalinkClicked -> handlePermalinkClicked(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handlePermalinkClicked(action: HomeActions.PermalinkClicked) {
        withState { state ->
            when (action.permalinkData) {
                is PermalinkData.EventLink -> {

                }
                is PermalinkData.RoomLink -> {

                }
                is PermalinkData.GroupLink -> {

                }
                is PermalinkData.UserLink -> {

                }
                is PermalinkData.FallbackLink -> {

                }
            }
        }
    }

    private fun handleSelectRoom(action: HomeActions.SelectRoom) {
        withState { state ->
            if (state.selectedRoomId != action.roomSummary.roomId) {
                roomSelectionRepository.saveLastSelectedRoom(action.roomSummary.roomId)
                setState { copy(selectedRoomId = action.roomSummary.roomId, shouldOpenRoomDetail = true) }
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

                    val selectedRoomId = selectedRoomId
                            ?: roomSelectionRepository.lastSelectedRoom()
                            ?: directRooms.firstOrNull()?.roomId
                            ?: groupRooms.firstOrNull()?.roomId

                    copy(
                            asyncRooms = async,
                            directRooms = directRooms,
                            groupRooms = groupRooms,
                            selectedRoomId = selectedRoomId
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