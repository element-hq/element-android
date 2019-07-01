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
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.home.HomeRoomListObservableStore

class RoomListViewModel @AssistedInject constructor(@Assisted initialState: RoomListViewState,
                                                    private val homeRoomListObservableSource: HomeRoomListObservableStore,
                                                    private val alphabeticalRoomComparator: AlphabeticalRoomComparator,
                                                    private val chronologicalRoomComparator: ChronologicalRoomComparator)
    : VectorViewModel<RoomListViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomListViewState): RoomListViewModel
    }

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val fragment: RoomListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomListViewModelFactory.create(state)
        }
    }

    private val displayMode = initialState.displayMode

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    init {
        observeRoomSummaries()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom     -> handleSelectRoom(action)
            is RoomListActions.ToggleCategory -> handleToggleCategory(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) {
        _openRoomLiveData.postValue(LiveEvent(action.roomSummary.roomId))
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

        homeRoomListObservableSource.observeFilteredBy(displayMode)
                .map { buildRoomSummaries(it) }
                .execute { async ->
                    copy(asyncFilteredRooms = async)
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