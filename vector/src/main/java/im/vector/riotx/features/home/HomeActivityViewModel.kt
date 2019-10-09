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

package im.vector.riotx.features.home

import arrow.core.Option
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.home.group.ALL_COMMUNITIES_GROUP_ID
import im.vector.riotx.features.home.group.SelectedGroupStore
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

data class EmptyState(val isEmpty: Boolean = true) : MvRxState

class HomeActivityViewModel @AssistedInject constructor(@Assisted initialState: EmptyState,
                                                        private val session: Session,
                                                        private val selectedGroupStore: SelectedGroupStore,
                                                        private val homeRoomListStore: HomeRoomListObservableStore
) : VectorViewModel<EmptyState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: EmptyState): HomeActivityViewModel
    }

    companion object : MvRxViewModelFactory<HomeActivityViewModel, EmptyState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: EmptyState): HomeActivityViewModel? {
            val homeActivity: HomeActivity = (viewModelContext as ActivityViewModelContext).activity()
            return homeActivity.homeActivityViewModelFactory.create(state)
        }
    }

    init {
        observeRoomAndGroup()
    }

    private fun observeRoomAndGroup() {
        Observable
                .combineLatest<List<RoomSummary>, Option<GroupSummary>, List<RoomSummary>>(
                        session.rx().liveRoomSummaries().throttleLast(300, TimeUnit.MILLISECONDS),
                        selectedGroupStore.observe(),
                        BiFunction { rooms, selectedGroupOption ->
                            val selectedGroup = selectedGroupOption.orNull()
                            val filteredDirectRooms = rooms
                                    .filter { it.isDirect }
                                    .filter {
                                        if (selectedGroup == null || selectedGroup.groupId == ALL_COMMUNITIES_GROUP_ID) {
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
                                        selectedGroup?.groupId == ALL_COMMUNITIES_GROUP_ID
                                                || selectedGroup?.roomIds?.contains(it.roomId) ?: true
                                    }
                            filteredDirectRooms + filteredGroupRooms
                        }
                )
                .subscribe {
                    homeRoomListStore.post(it)
                }
                .disposeOnClear()
    }
}
