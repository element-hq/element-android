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
import im.vector.app.features.home.CurrentSpaceSuggestedRoomListDataSource
import im.vector.app.features.home.HomeRoomListDataSource
import im.vector.app.features.home.room.list.ChronologicalRoomComparator
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.UiStateRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
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
        private val currentSpaceSuggestedDataSource: CurrentSpaceSuggestedRoomListDataSource,
        private val chronologicalRoomComparator: ChronologicalRoomComparator,
        private val vectorPreferences: VectorPreferences,
        private val uiStateRepository: UiStateRepository) : LifecycleObserver {

    private val compositeDisposable = CompositeDisposable()

    init {
        // restore current space from ui state
        sessionDataSource.currentValue?.orNull()?.let { session ->
            uiStateRepository.getSelectedSpace(session.sessionId)?.let { selectedSpaceId ->
                session.getRoomSummary(selectedSpaceId)?.let {
                    selectedSpaceDataSource.post(Option.just(it))
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
        observeRoomsAndGroup()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        compositeDisposable.clear()
    }

    private fun observeRoomsAndGroup() {
        vectorPreferences.labSpacesLive()
                .asObservable()
                .switchMap { useSpaces ->
                    if (useSpaces) {
                        spaceFilterObservable()
                    } else {
                        groupFilterObservable()
                    }
                }
                .subscribe {
//                    Timber.w("VAL: subscribe")
                    homeRoomListDataSource.post(it)
                }
                .addTo(compositeDisposable)

        selectedSpaceDataSource.observe()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe { currentSpaceOptional ->
                    GlobalScope.launch {
                        sessionDataSource.currentValue?.orNull()?.let { session ->
                            val currentSpace = currentSpaceOptional.orNull()
                                    .takeIf { it?.roomId != im.vector.app.features.spaces.ALL_COMMUNITIES_GROUP_ID }
                            if (currentSpace != null) {
                                val childInfo = withContext(Dispatchers.IO) {
                                    tryOrNull {
                                        session.spaceService().querySpaceChildren(currentSpace.roomId, suggestedOnly = true)
                                    }
                                }
                                childInfo?.second?.let { currentSpaceSuggestedDataSource.post(it) } ?: kotlin.run {
                                    currentSpaceSuggestedDataSource.post(emptyList())
                                }
                            } else {
                                currentSpaceSuggestedDataSource.post(emptyList())
                            }
                        }
                    }
                }
                .addTo(compositeDisposable)
    }

    // XXX this is very inefficient.. temporary
    private fun spaceFilterObservable() = sessionDataSource.observe()
            .observeOn(Schedulers.computation())
            .switchMap {
                Timber.w("VAL: switchmap session")
                val currentSession = it.orNull()
                if (currentSession == null) {
                    Observable.just(emptyList())
                } else {
                    selectedSpaceDataSource.observe()
                            .distinctUntilChanged()
                            .switchMap { currentSpaceOption ->
//                                Timber.w("VAL: switchmap selected space ${currentSpaceOption.orNull()?.name}")
                                val currentSpace = currentSpaceOption.orNull()
                                currentSession.rx()
                                        .liveRoomSummaries(roomSummaryQueryParams {
//                                                            roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                        })
                                        .throttleFirst(300, TimeUnit.MILLISECONDS)
                                        .startWith(Observable.just(emptyList()))
                                        .observeOn(Schedulers.newThread())
                                        .map { summaries ->
//                                            Timber.w("VAL: live summaries update ${Thread.currentThread().name}")
//                                            val startime = System.currentTimeMillis()
                                            val filteredDm = summaries.filter {
                                                if (currentSpace == null || currentSpace.roomId == ALL_COMMUNITIES_GROUP_ID) {
                                                    it.isDirect
                                                } else if (it.isDirect) {
                                                    it.otherMemberIds
                                                            .intersect(currentSpace.otherMemberIds)
                                                            .isNotEmpty()
                                                } else {
                                                    false
                                                }
                                            }

                                            val rooms = currentSession.getFlattenRoomSummaryChildOf(
                                                    currentSpace?.roomId?.takeIf { it != ALL_COMMUNITIES_GROUP_ID }
                                            )
                                            (filteredDm + rooms).sortedWith(chronologicalRoomComparator)
//                                                    .also {
//                                                Timber.w("VAL: live summaries update filter done ${System.currentTimeMillis() - startime}")
//                                            }
                                        }
//                                Observable
//                                        .combineLatest<List<RoomSummary>, List<RoomSummary>, List<RoomSummary>>(
//                                                // could be nice to only observe DMs...
//                                                currentSession.rx()
//                                                        .liveRoomSummaries(roomSummaryQueryParams {
//                                                            roomCategoryFilter = RoomCategoryFilter.ONLY_DM
//                                                        })
//                                                        // throttle first to quickly react to space change
//                                                        .throttleFirst(300, TimeUnit.MILLISECONDS),
//                                                currentSession.rx()
//                                                        .liveFlattenRoomSummaryChildOf(
//                                                                (currentSpace?.roomId?.takeIf { it != ALL_COMMUNITIES_GROUP_ID })
//                                                        ),
//                                                { dms, rooms ->
//                                                    val filteredDms = dms
//                                                            .filter {
//                                                                if (currentSpace == null || currentSpace.roomId == ALL_COMMUNITIES_GROUP_ID) {
//                                                                    it.isDirect // always true
//                                                                } else if (it.isDirect) {
//                                                                    it.otherMemberIds
//                                                                            .intersect(currentSpace.otherMemberIds)
//                                                                            .isNotEmpty()
//                                                                } else {
//                                                                    false
//                                                                }
//                                                            }
//                                                    (filteredDms + rooms).sortedWith(chronologicalRoomComparator)
//                                                }
//                                        )
                            }
//                            .startWith(Observable.just(emptyList()))
                }
            }

    private fun groupFilterObservable() = Observable
            .combineLatest<List<RoomSummary>, Option<GroupSummary>, List<RoomSummary>>(
                    sessionDataSource.observe()
                            .observeOn(AndroidSchedulers.mainThread())
                            .switchMap {
                                val query = roomSummaryQueryParams {}
                                it.orNull()?.rx()?.liveRoomSummaries(query)
                                        ?: Observable.just(emptyList())
                            }
                            .throttleLast(300, TimeUnit.MILLISECONDS),
                    selectedGroupDataSource.observe(),
                    { rooms, selectedGroupOption ->
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
            )
}
