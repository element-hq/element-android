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

package im.vector.riotx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import arrow.core.Option
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.rx.rx
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.home.HomeRoomListObservableStore
import im.vector.riotx.features.home.group.ALL_COMMUNITIES_GROUP_ID
import im.vector.riotx.features.home.group.SelectedGroupStore
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


/**
 * This class handles the main room list at the moment. It will dispatch the results to the store.
 * It requires to be added with ProcessLifecycleOwner.get().lifecycle.addObserver
 */
@Singleton
class AppStateHandler @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val homeRoomListStore: HomeRoomListObservableStore,
        private val selectedGroupStore: SelectedGroupStore) : LifecycleObserver {

    private val compositeDisposable = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
        observeRoomsAndGroup()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        compositeDisposable.clear()
    }

    private fun observeRoomsAndGroup() {
        Observable
                .combineLatest<List<RoomSummary>, Option<GroupSummary>, List<RoomSummary>>(
                        activeSessionHolder.getActiveSession()
                                .rx()
                                .liveRoomSummaries()
                                .throttleLast(300, TimeUnit.MILLISECONDS),
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
                .addTo(compositeDisposable)
    }
}
