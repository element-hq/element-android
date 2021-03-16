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

package im.vector.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import arrow.core.Option
import im.vector.app.features.grouplist.ALL_COMMUNITIES_GROUP_ID
import im.vector.app.features.grouplist.SelectedGroupDataSource
import im.vector.app.features.grouplist.SelectedSpaceDataSource
import im.vector.app.features.home.HomeRoomListDataSource
import im.vector.app.features.home.room.list.ChronologicalRoomComparator
import im.vector.app.features.settings.VectorPreferences
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.addTo
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.space.SpaceSummary
import org.matrix.android.sdk.rx.rx
import java.util.concurrent.TimeUnit
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
        private val selectedSpaceDataSource: SelectedSpaceDataSource,
        private val chronologicalRoomComparator: ChronologicalRoomComparator,
        private val vectorPreferences: VectorPreferences) : LifecycleObserver {

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
                .combineLatest<List<RoomSummary>, Option<GroupSummary>, Option<SpaceSummary>, List<RoomSummary>>(
                        sessionDataSource.observe()
                                .observeOn(AndroidSchedulers.mainThread())
                                .switchMap {
                                    val query = roomSummaryQueryParams {}
                                    it.orNull()?.rx()?.liveRoomSummaries(query)
                                            ?: Observable.just(emptyList())
                                }
                                .throttleLast(300, TimeUnit.MILLISECONDS),
                        selectedGroupDataSource.observe(),
                        selectedSpaceDataSource.observe(),
                        Function3 { rooms, selectedGroupOption, selectedSpace ->
                            if (vectorPreferences.labSpaces()) {
                                val selectedSpace = selectedSpace.orNull()
                                val filteredRooms = rooms.filter {
                                    if (selectedSpace == null || selectedSpace.spaceId == ALL_COMMUNITIES_GROUP_ID) {
                                        true
                                    } else if (it.isDirect) {
                                        it.otherMemberIds
                                                .intersect(selectedSpace.roomSummary.otherMemberIds)
                                                .isNotEmpty()
                                    } else {
                                        selectedSpace.children.indexOfFirst { child -> child.roomId == it.roomId } != -1
//                                        selectedGroup.roomIds.contains(it.roomId)
                                    }
                                }
                                filteredRooms.sortedWith(chronologicalRoomComparator)
                            } else {
                                val selectedGroup = selectedGroupOption.orNull()
                                val filteredRooms = rooms.filter {
                                    if (selectedGroup == null || selectedGroup.groupId == ALL_COMMUNITIES_GROUP_ID) {
                                        true
                                    } else if (it.isDirect) {
                                        it.otherMemberIds
                                                .intersect(selectedGroup.userIds)
                                                .isNotEmpty()
                                    } else {
                                        selectedGroup.roomIds.contains(it.roomId)
                                    }
                                }
                                filteredRooms.sortedWith(chronologicalRoomComparator)
                            }
                        }
                )
                .subscribe {
                    homeRoomListDataSource.post(it)
                }
                .addTo(compositeDisposable)
    }
}
