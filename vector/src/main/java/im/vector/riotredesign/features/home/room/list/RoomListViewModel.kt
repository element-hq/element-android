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
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.home.HomeRoomListObservableStore
import io.reactivex.Observable
import org.koin.android.ext.android.get

typealias RoomListFilterName = CharSequence

class RoomListViewModel(initialState: RoomListViewState,
                        private val session: Session,
                        private val homeRoomListObservableSource: HomeRoomListObservableStore,
                        private val alphabeticalRoomComparator: AlphabeticalRoomComparator,
                        private val chronologicalRoomComparator: ChronologicalRoomComparator)
    : VectorViewModel<RoomListViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            val homeRoomListObservableSource = viewModelContext.activity.get<HomeRoomListObservableStore>()
            val chronologicalRoomComparator = viewModelContext.activity.get<ChronologicalRoomComparator>()
            val alphabeticalRoomComparator = viewModelContext.activity.get<AlphabeticalRoomComparator>()
            return RoomListViewModel(state, currentSession, homeRoomListObservableSource, alphabeticalRoomComparator, chronologicalRoomComparator)
        }
    }

    private val displayMode = initialState.displayMode
    private val roomListFilter = BehaviorRelay.createDefault<Option<RoomListFilterName>>(Option.empty())

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    init {
        observeRoomSummaries()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom     -> handleSelectRoom(action)
            is RoomListActions.FilterRooms    -> handleFilterRooms(action)
            is RoomListActions.ToggleCategory -> handleToggleCategory(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) {
        _openRoomLiveData.postValue(LiveEvent(action.roomSummary.roomId))
    }

    private fun handleFilterRooms(action: RoomListActions.FilterRooms) {
        val optionalFilter = Option.fromNullable(action.roomName)
        roomListFilter.accept(optionalFilter)
    }

    private fun handleToggleCategory(action: RoomListActions.ToggleCategory) = setState {
        this.toggle(action.category)
    }


    private fun observeRoomSummaries() {
        homeRoomListObservableSource
                .observe()
                .execute { asyncRooms ->
                    copy(asyncRooms = asyncRooms)
                }

        homeRoomListObservableSource
                .observe()
                .flatMapSingle {
                    Observable.fromIterable(it)
                            .filter(filterByDisplayMode(displayMode))
                            .toList()
                }
                .map { buildRoomSummaries(it) }
                .execute { async ->
                    copy(asyncFilteredRooms = async)
                }
    }

    private fun filterByDisplayMode(displayMode: RoomListFragment.DisplayMode) = { roomSummary: RoomSummary ->
        when (displayMode) {
            RoomListFragment.DisplayMode.HOME   -> roomSummary.notificationCount > 0
            RoomListFragment.DisplayMode.PEOPLE -> roomSummary.isDirect
            RoomListFragment.DisplayMode.ROOMS  -> !roomSummary.isDirect
        }
    }

    private fun buildRoomSummaries(rooms: List<RoomSummary>): RoomSummaries {
        val invites = ArrayList<RoomSummary>()
        val favourites = ArrayList<RoomSummary>()
        val directChats = ArrayList<RoomSummary>()
        val groupRooms = ArrayList<RoomSummary>()
        val lowPriorities = ArrayList<RoomSummary>()
        val serverNotices = ArrayList<RoomSummary>()

        for (room in rooms) {
            if (room.membership.isLeft()) continue
            val tags = room.tags.map { it.name }
            when {
                room.membership == Membership.INVITE          -> invites.add(room)
                tags.contains(RoomTag.ROOM_TAG_SERVER_NOTICE) -> serverNotices.add(room)
                tags.contains(RoomTag.ROOM_TAG_FAVOURITE)     -> favourites.add(room)
                tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY)  -> lowPriorities.add(room)
                room.isDirect                                 -> directChats.add(room)
                else                                          -> groupRooms.add(room)
            }
        }

        val roomComparator = when (displayMode) {
            RoomListFragment.DisplayMode.HOME   -> chronologicalRoomComparator
            RoomListFragment.DisplayMode.PEOPLE -> chronologicalRoomComparator
            RoomListFragment.DisplayMode.ROOMS  -> alphabeticalRoomComparator
        }

        return RoomSummaries().apply {
            put(RoomCategory.INVITE, invites.sortedWith(roomComparator))
            put(RoomCategory.FAVOURITE, favourites.sortedWith(roomComparator))
            put(RoomCategory.DIRECT, directChats.sortedWith(roomComparator))
            put(RoomCategory.GROUP, groupRooms.sortedWith(roomComparator))
            put(RoomCategory.LOW_PRIORITY, lowPriorities.sortedWith(roomComparator))
            put(RoomCategory.SERVER_NOTICE, serverNotices.sortedWith(roomComparator))
        }
    }


}