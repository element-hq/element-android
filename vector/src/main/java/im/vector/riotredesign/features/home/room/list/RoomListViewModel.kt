/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Option
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.home.group.SelectedGroupStore
import im.vector.riotredesign.features.home.room.VisibleRoomStore
import io.reactivex.Observable
import io.reactivex.functions.Function3
import org.koin.android.ext.android.get
import java.util.concurrent.TimeUnit

typealias RoomListFilterName = CharSequence

class RoomListViewModel(initialState: RoomListViewState,
                        private val session: Session,
                        private val selectedGroupHolder: SelectedGroupStore,
                        private val visibleRoomHolder: VisibleRoomStore,
                        private val roomSelectionRepository: RoomSelectionRepository,
                        private val roomSummaryComparator: RoomSummaryComparator)
    : RiotViewModel<RoomListViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            val roomSelectionRepository = viewModelContext.activity.get<RoomSelectionRepository>()
            val selectedGroupHolder = viewModelContext.activity.get<SelectedGroupStore>()
            val visibleRoomHolder = viewModelContext.activity.get<VisibleRoomStore>()
            val roomSummaryComparator = viewModelContext.activity.get<RoomSummaryComparator>()
            return RoomListViewModel(state, currentSession, selectedGroupHolder, visibleRoomHolder, roomSelectionRepository, roomSummaryComparator)
        }
    }


    private val roomListFilter = BehaviorRelay.createDefault<Option<RoomListFilterName>>(Option.empty())

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    init {
        observeRoomSummaries()
        observeVisibleRoom()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom     -> handleSelectRoom(action)
            is RoomListActions.FilterRooms    -> handleFilterRooms(action)
            is RoomListActions.ToggleCategory -> handleToggleCategory(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) = withState { state ->
        if (state.visibleRoomId != action.roomSummary.roomId) {
            roomSelectionRepository.saveLastSelectedRoom(action.roomSummary.roomId)
            _openRoomLiveData.postValue(LiveEvent(action.roomSummary.roomId))
        }
    }

    private fun handleFilterRooms(action: RoomListActions.FilterRooms) {
        val optionalFilter = Option.fromNullable(action.roomName)
        roomListFilter.accept(optionalFilter)
    }

    private fun handleToggleCategory(action: RoomListActions.ToggleCategory) = setState {
        this.toggle(action.category)
    }

    private fun observeVisibleRoom() {
        visibleRoomHolder.observe()
                .doOnNext {
                    setState { copy(visibleRoomId = it) }
                }
                .subscribe()
                .disposeOnClear()
    }

    private fun observeRoomSummaries() {
        Observable.combineLatest<List<RoomSummary>, Option<GroupSummary>, Option<RoomListFilterName>, RoomSummaries>(
                session.rx().liveRoomSummaries().throttleLast(300, TimeUnit.MILLISECONDS),
                selectedGroupHolder.observe(),
                roomListFilter.throttleLast(300, TimeUnit.MILLISECONDS),
                Function3 { rooms, selectedGroupOption, filterRoomOption ->
                    val filteredRooms = filterRooms(rooms, filterRoomOption)
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
                    buildRoomSummaries(filteredDirectRooms + filteredGroupRooms)
                }
        )
                .execute { async ->
                    copy(
                            asyncRooms = async
                    )
                }
    }

    private fun filterRooms(rooms: List<RoomSummary>, filterRoomOption: Option<RoomListFilterName>): List<RoomSummary> {
        val filterRoom = filterRoomOption.orNull()
        return rooms.filter {
            if (filterRoom.isNullOrBlank()) {
                true
            } else {
                it.displayName.contains(other = filterRoom, ignoreCase = true)
            }
        }
    }

    private fun buildRoomSummaries(rooms: List<RoomSummary>): RoomSummaries {
        val favourites = ArrayList<RoomSummary>()
        val directChats = ArrayList<RoomSummary>()
        val groupRooms = ArrayList<RoomSummary>()
        val lowPriorities = ArrayList<RoomSummary>()
        val serverNotices = ArrayList<RoomSummary>()

        for (room in rooms) {
            val tags = room.tags.map { it.name }
            when {
                tags.contains(RoomTag.ROOM_TAG_SERVER_NOTICE) -> serverNotices.add(room)
                tags.contains(RoomTag.ROOM_TAG_FAVOURITE)     -> favourites.add(room)
                tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY)  -> lowPriorities.add(room)
                room.isDirect                                 -> directChats.add(room)
                else                                          -> groupRooms.add(room)
            }
        }

        return RoomSummaries().apply {
            put(RoomCategory.FAVOURITE, favourites.sortedWith(roomSummaryComparator))
            put(RoomCategory.DIRECT, directChats.sortedWith(roomSummaryComparator))
            put(RoomCategory.GROUP, groupRooms.sortedWith(roomSummaryComparator))
            put(RoomCategory.LOW_PRIORITY, lowPriorities.sortedWith(roomSummaryComparator))
            put(RoomCategory.SERVER_NOTICE, serverNotices.sortedWith(roomSummaryComparator))
        }
    }


}