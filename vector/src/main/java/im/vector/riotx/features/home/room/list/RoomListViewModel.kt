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

package im.vector.riotx.features.home.room.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent
import im.vector.riotx.features.home.HomeRoomListObservableStore
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class RoomListViewModel @AssistedInject constructor(@Assisted initialState: RoomListViewState,
                                                    private val session: Session,
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
    private val roomListDisplayModeFilter = RoomListDisplayModeFilter(displayMode)

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    private val _invitationAnswerErrorLiveData = MutableLiveData<LiveEvent<Throwable>>()
    val invitationAnswerErrorLiveData: LiveData<LiveEvent<Throwable>>
        get() = _invitationAnswerErrorLiveData

    init {
        observeRoomSummaries()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom       -> handleSelectRoom(action)
            is RoomListActions.ToggleCategory   -> handleToggleCategory(action)
            is RoomListActions.AcceptInvitation -> handleAcceptInvitation(action)
            is RoomListActions.RejectInvitation -> handleRejectInvitation(action)
            is RoomListActions.FilterWith       -> handleFilter(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) {
        _openRoomLiveData.postLiveEvent(action.roomSummary.roomId)
    }

    private fun handleToggleCategory(action: RoomListActions.ToggleCategory) = setState {
        this.toggle(action.category)
    }

    private fun handleFilter(action: RoomListActions.FilterWith) {
        setState {
            copy(
                    roomFilter = action.filter
            )
        }
    }

    private fun observeRoomSummaries() {
        homeRoomListObservableSource
                .observe()
                .observeOn(Schedulers.computation())
                .map {
                    it.sortedWith(chronologicalRoomComparator)
                }
                .execute { asyncRooms ->
                    copy(asyncRooms = asyncRooms)
                }

        homeRoomListObservableSource
                .observe()
                .observeOn(Schedulers.computation())
                .map { buildRoomSummaries(it) }
                .execute { async ->
                    copy(asyncFilteredRooms = async)
                }
    }

    private fun handleAcceptInvitation(action: RoomListActions.AcceptInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId

        if (state.joiningRoomsIds.contains(roomId) || state.rejectingRoomsIds.contains(roomId)) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }

        setState {
            copy(
                    joiningRoomsIds = joiningRoomsIds.toMutableSet().apply { add(roomId) },
                    rejectingErrorRoomsIds = rejectingErrorRoomsIds.toMutableSet().apply { remove(roomId) }
            )
        }

        session.getRoom(roomId)?.join(emptyList(), object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _invitationAnswerErrorLiveData.postLiveEvent(failure)

                setState {
                    copy(
                            joiningRoomsIds = joiningRoomsIds.toMutableSet().apply { remove(roomId) },
                            joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableSet().apply { add(roomId) }
                    )
                }
            }
        })
    }

    private fun handleRejectInvitation(action: RoomListActions.RejectInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId

        if (state.joiningRoomsIds.contains(roomId) || state.rejectingRoomsIds.contains(roomId)) {
            // Request already sent, should not happen
            Timber.w("Try to reject an already rejecting room. Should not happen")
            return@withState
        }

        setState {
            copy(
                    rejectingRoomsIds = rejectingRoomsIds.toMutableSet().apply { add(roomId) },
                    joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableSet().apply { remove(roomId) }
            )
        }

        session.getRoom(roomId)?.leave(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the rejectingRoomsIds here, because, the room is not rejected yet regarding the sync data.
                // Instead, we wait for the room to be rejected
                // Known bug: if the user is invited again (after rejecting the first invitation), the loading will be displayed instead of the buttons.
                // If we update the state, the button will be displayed again, so it's not ideal...
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _invitationAnswerErrorLiveData.postLiveEvent(failure)

                setState {
                    copy(
                            rejectingRoomsIds = rejectingRoomsIds.toMutableSet().apply { remove(roomId) },
                            rejectingErrorRoomsIds = rejectingErrorRoomsIds.toMutableSet().apply { add(roomId) }
                    )
                }
            }
        })
    }

    private fun buildRoomSummaries(rooms: List<RoomSummary>): RoomSummaries {
        val invites = ArrayList<RoomSummary>()
        val favourites = ArrayList<RoomSummary>()
        val directChats = ArrayList<RoomSummary>()
        val groupRooms = ArrayList<RoomSummary>()
        val lowPriorities = ArrayList<RoomSummary>()
        val serverNotices = ArrayList<RoomSummary>()

        rooms
                .filter { roomListDisplayModeFilter.test(it) }
                .forEach { room ->
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
            RoomListFragment.DisplayMode.HOME     -> chronologicalRoomComparator
            RoomListFragment.DisplayMode.PEOPLE   -> chronologicalRoomComparator
            RoomListFragment.DisplayMode.ROOMS    -> chronologicalRoomComparator
            RoomListFragment.DisplayMode.FILTERED -> chronologicalRoomComparator
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