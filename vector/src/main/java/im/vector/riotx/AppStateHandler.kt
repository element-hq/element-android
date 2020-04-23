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
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.roomSummaryQueryParams
import im.vector.matrix.rx.rx
import im.vector.riotx.features.grouplist.ALL_COMMUNITIES_GROUP_ID
import im.vector.riotx.features.grouplist.SelectedGroupDataSource
import im.vector.riotx.features.home.HomeRoomListDataSource
import im.vector.riotx.features.home.room.list.ChronologicalRoomComparator
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles the global app state. At the moment, it only manages room list.
 * It requires to be added to ProcessLifecycleOwner.get().lifecycle
 */
@Singleton
class AppStateHandler @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val homeRoomListDataSource: HomeRoomListDataSource,
        private val selectedGroupDataSource: SelectedGroupDataSource,
        private val chronologicalRoomComparator: ChronologicalRoomComparator) : LifecycleObserver {

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
                .combineLatest<Option<Session>, Option<GroupSummary>, Pair<Option<Session>, Option<GroupSummary>>>(
                        sessionDataSource.observe(),
                        selectedGroupDataSource.observe(),
                        BiFunction { sessionOption, selectedGroupOption ->
                            Pair(sessionOption, selectedGroupOption)
                        }
                ).switchMap {
                    val selectedGroup = it.second.orNull()
                    val session = it.first.orNull()
                    val queryParams = if (selectedGroup?.groupId == null || selectedGroup.groupId == ALL_COMMUNITIES_GROUP_ID) {
                        roomSummaryQueryParams()
                    } else {
                        roomSummaryQueryParams {
                            fromGroupId = selectedGroup.groupId
                        }
                    }
                    session
                            ?.rx()
                            ?.liveRoomSummaries(queryParams)
                            ?: Observable.empty()
                }
                .map {
                    it.sortedWith(chronologicalRoomComparator)
                }
                .subscribe {
                    homeRoomListDataSource.post(it)
                }
                .addTo(compositeDisposable)
    }
}
