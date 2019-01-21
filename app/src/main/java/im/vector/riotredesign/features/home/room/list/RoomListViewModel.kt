package im.vector.riotredesign.features.home.room.list

import arrow.core.Option
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.features.home.group.SelectedGroupHolder
import im.vector.riotredesign.features.home.room.VisibleRoomHolder
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get

typealias RoomListFilterName = CharSequence

class RoomListViewModel(initialState: RoomListViewState,
                        private val session: Session,
                        private val selectedGroupHolder: SelectedGroupHolder,
                        private val visibleRoomHolder: VisibleRoomHolder,
                        private val roomSelectionRepository: RoomSelectionRepository)
    : RiotViewModel<RoomListViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val currentSession = Matrix.getInstance().currentSession
            val roomSelectionRepository = viewModelContext.activity.get<RoomSelectionRepository>()
            val selectedGroupHolder = viewModelContext.activity.get<SelectedGroupHolder>()
            val visibleRoomHolder = viewModelContext.activity.get<VisibleRoomHolder>()
            return RoomListViewModel(state, currentSession, selectedGroupHolder, visibleRoomHolder, roomSelectionRepository)
        }
    }


    private val roomListFilter = BehaviorRelay.createDefault<Option<RoomListFilterName>>(Option.empty())

    init {
        observeRoomSummaries()
        observeVisibleRoom()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom  -> handleSelectRoom(action)
            is RoomListActions.FilterRooms -> handleFilterRooms(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) = withState { state ->
        if (state.selectedRoomId != action.roomSummary.roomId) {
            roomSelectionRepository.saveLastSelectedRoom(action.roomSummary.roomId)
        }
    }

    private fun handleFilterRooms(action: RoomListActions.FilterRooms) {
        val optionalFilter = Option.fromNullable(action.roomName)
        roomListFilter.accept(optionalFilter)
    }

    private fun observeVisibleRoom() {
        visibleRoomHolder.visibleRoom()
                .subscribeBy {
                    setState { copy(selectedRoomId = it) }
                }
                .disposeOnClear()
    }

    private fun observeRoomSummaries() {
        Observable.combineLatest<List<RoomSummary>, Option<GroupSummary>, Option<RoomListFilterName>, RoomSummaries>(
                session.rx().liveRoomSummaries(),
                selectedGroupHolder.selectedGroup(),
                roomListFilter,
                Function3 { rooms, selectedGroupOption, filterRoomOption ->
                    val filterRoom = filterRoomOption.orNull()
                    val filteredRooms = rooms.filter {
                        if (filterRoom.isNullOrBlank()) {
                            true
                        } else {
                            it.displayName.contains(other = filterRoom, ignoreCase = true)
                        }
                    }

                    val selectedGroup = selectedGroupOption.orNull()
                    val filteredDirectRooms = filteredRooms
                            .filter { it.isDirect }
                            .filter {
                                if (selectedGroup == null) {
                                    true
                                } else {
                                    it.otherMemberIds
                                            .intersect(selectedGroup.userIds)
                                            .isNotEmpty()
                                }
                            }

                    val filteredGroupRooms = filteredRooms
                            .filter { !it.isDirect }
                            .filter {
                                selectedGroup?.roomIds?.contains(it.roomId) ?: true
                            }
                    RoomSummaries(filteredDirectRooms, filteredGroupRooms)
                }
        )
                .execute { async ->
                    copy(
                            asyncRooms = async
                    )
                }
    }
}