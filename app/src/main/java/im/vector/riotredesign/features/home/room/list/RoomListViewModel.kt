/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.room.list

import arrow.core.Option
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.features.home.group.SelectedGroupHolder
import im.vector.riotredesign.features.home.room.VisibleRoomHolder
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get

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

    init {
        observeRoomSummaries()
        observeVisibleRoom()
    }

    fun accept(action: RoomListActions) {
        when (action) {
            is RoomListActions.SelectRoom -> handleSelectRoom(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListActions.SelectRoom) = withState { state ->
        if (state.selectedRoomId != action.roomSummary.roomId) {
            roomSelectionRepository.saveLastSelectedRoom(action.roomSummary.roomId)
        }
    }

    private fun observeVisibleRoom() {
        visibleRoomHolder.visibleRoom()
                .subscribeBy {
                    setState { copy(selectedRoomId = it) }
                }
                .disposeOnClear()
    }

    private fun observeRoomSummaries() {
        Observable.combineLatest<List<RoomSummary>, Option<GroupSummary>, RoomSummaries>(
                session.rx().liveRoomSummaries(),
                selectedGroupHolder.selectedGroup(),
                BiFunction { rooms, selectedGroupOption ->
                    val selectedGroup = selectedGroupOption.orNull()

                    val filteredDirectRooms = rooms
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

                    val filteredGroupRooms = rooms
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