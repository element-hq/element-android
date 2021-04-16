/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.airbnb.mvrx.Async
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.rx.asObservable

class SpaceRoomListSectionBuilder(
        val session: Session,
        val stringProvider: StringProvider,
        val appStateHandler: AppStateHandler,
        val viewModelScope: CoroutineScope,
        private val suggestedRoomJoiningState: LiveData<Map<String, Async<Unit>>>,
        val onDisposable: (Disposable) -> Unit,
        val onUdpatable: (UpdatableLivePageResult) -> Unit
) : RoomListSectionBuilder {

    override fun buildSections(mode: RoomListDisplayMode): List<RoomsSection> {
        val sections = mutableListOf<RoomsSection>()
        val activeSpaceAwareQueries = mutableListOf<RoomListViewModel.ActiveSpaceQueryUpdater>()
        when (mode) {
            RoomListDisplayMode.PEOPLE -> {
                buildDmSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.ROOMS -> {
                buildRoomsSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.FILTERED -> {
                withQueryParams(
                        {
                            it.memberships = Membership.activeMemberships()
                        },
                        { qpm ->
                            val name = stringProvider.getString(R.string.bottom_action_rooms)
                            session.getFilteredPagedRoomSummariesLive(qpm)
                                    .let { updatableFilterLivePageResult ->
                                        onUdpatable(updatableFilterLivePageResult)
                                        sections.add(RoomsSection(name, updatableFilterLivePageResult.livePagedList))
                                    }
                        }
                )
            }
            RoomListDisplayMode.NOTIFICATIONS -> {
                addSection(
                        sections,
                        activeSpaceAwareQueries,
                        R.string.invitations_header,
                        true,
                        RoomListViewModel.SpaceFilterStrategy.NORMAL
                ) {
                    it.memberships = listOf(Membership.INVITE)
                    it.roomCategoryFilter = RoomCategoryFilter.ALL
                }

                addSection(
                        sections,
                        activeSpaceAwareQueries,
                        R.string.bottom_action_rooms,
                        false,
                        RoomListViewModel.SpaceFilterStrategy.NORMAL
                ) {
                    it.memberships = listOf(Membership.JOIN)
                    it.roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
                }
            }
        }

        appStateHandler.selectedSpaceObservable
                .distinctUntilChanged()
                .subscribe { activeSpaceOption ->
                    val selectedSpace = activeSpaceOption.orNull()
                    activeSpaceAwareQueries.onEach { updater ->
                        updater.updateForSpaceId(selectedSpace?.roomId?.takeIf { MatrixPatterns.isRoomId(it) })
                    }
                }.also {
                    onDisposable.invoke(it)
                }

        return sections
    }

    private fun buildRoomsSections(sections: MutableList<RoomsSection>, activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
        addSection(
                sections, activeSpaceAwareQueries,
                R.string.invitations_header,
                true,
                RoomListViewModel.SpaceFilterStrategy.NONE
        ) {
            it.memberships = listOf(Membership.INVITE)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_favourites,
                false,
                RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_rooms,
                false,
                RoomListViewModel.SpaceFilterStrategy.NORMAL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(false, false, false)
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.low_priority_header,
                false,
                RoomListViewModel.SpaceFilterStrategy.NORMAL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.system_alerts_header,
                false,
                RoomListViewModel.SpaceFilterStrategy.NORMAL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, null, true)
        }

        // add suggested rooms
        val suggestedRoomsObservable = // MutableLiveData<List<SpaceChildInfo>>()
                appStateHandler.selectedSpaceObservable
                        .distinctUntilChanged()
                        .switchMap { activeSpaceOption ->
                            val selectedSpace = activeSpaceOption.orNull()
                            if (selectedSpace == null) {
                                Observable.just(emptyList())
                            } else {
                                liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                                    val spaceSum = tryOrNull { session.spaceService().querySpaceChildren(selectedSpace.roomId, suggestedOnly = true) }
                                    val value = spaceSum?.second ?: emptyList()
                                    // i need to check if it's already joined.
                                    val filtered = value.filter {
                                        session.getRoomSummary(it.childRoomId)?.membership?.isActive() != true
                                    }
                                    emit(filtered)
                                }.asObservable()
                            }
                        }

        val liveSuggestedRooms = MutableLiveData<SuggestedRoomInfo>()
        Observables.combineLatest(
                suggestedRoomsObservable,
                suggestedRoomJoiningState.asObservable()
        ) { rooms, joinStates ->
            SuggestedRoomInfo(
                    rooms,
                    joinStates
            )
        }.subscribe {
            liveSuggestedRooms.postValue(it)
        }.also {
            onDisposable.invoke(it)
        }
        sections.add(
                RoomsSection(
                        sectionName = stringProvider.getString(R.string.suggested_header),
                        liveSuggested = liveSuggestedRooms,
                        notifyOfLocalEcho = false
                )
        )
    }

    private fun buildDmSections(sections: MutableList<RoomsSection>, activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
        addSection(sections,
                activeSpaceAwareQueries,
                R.string.invitations_header,
                true,
                RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL
        ) {
            it.memberships = listOf(Membership.INVITE)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
        }

        addSection(sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_favourites,
                false,
                RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        addSection(sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_people_x,
                false,
                RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(false, null, null)
        }

//        // For DMs we still need some post query filter :/
//        // It's probably less important as home is not filtering at all
//        val dmList = MutableLiveData<List<RoomSummary>>()
//        Observables.combineLatest(
//                session.getRoomSummariesLive(
//                        roomSummaryQueryParams {
//                            memberships = listOf(Membership.JOIN)
//                            roomCategoryFilter = RoomCategoryFilter.ONLY_DM
//                        }
//                ).asObservable(),
//                appStateHandler.selectedSpaceDataSource.observe()
//
//        ) { rooms, currentSpaceOption ->
//            val currentSpace = currentSpaceOption.orNull()
//                    .takeIf {
//                        // the +ALL trick is annoying, should find a way to fix that at the source!
//                        MatrixPatterns.isRoomId(it?.roomId)
//                    }
//            if (currentSpace == null) {
//                rooms
//            } else {
//                rooms.filter {
//                    it.otherMemberIds
//                            .intersect(currentSpace.otherMemberIds)
//                            .isNotEmpty()
//                }
//            }
//        }.subscribe {
//            dmList.postValue(it)
//        }.also {
//            onDisposable.invoke(it)
//        }
//
//        sections.add(
//                RoomsSection(
//                        sectionName = stringProvider.getString(R.string.bottom_action_people_x),
//                        liveList = dmList,
//                        notifyOfLocalEcho = false
//                )
//        )
    }

    private fun addSection(sections: MutableList<RoomsSection>,
                           activeSpaceUpdaters: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>,
                           @StringRes nameRes: Int,
                           notifyOfLocalEcho: Boolean = false,
                           spaceFilterStrategy: RoomListViewModel.SpaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.NONE,
                           query: (RoomSummaryQueryParams.Builder) -> Unit) {
        withQueryParams(
                { query.invoke(it) },
                { roomQueryParams ->

                    val name = stringProvider.getString(nameRes)
                    session.getFilteredPagedRoomSummariesLive(
                            when (spaceFilterStrategy) {
                                RoomListViewModel.SpaceFilterStrategy.NORMAL     -> {
                                    roomQueryParams.copy(
                                            activeSpaceId = ActiveSpaceFilter.ActiveSpace(appStateHandler.safeActiveSpaceId())
                                    )
                                }
                                RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL -> {
                                    if (appStateHandler.safeActiveSpaceId() == null) {
                                        roomQueryParams
                                    } else {
                                        roomQueryParams.copy(
                                                activeSpaceId = ActiveSpaceFilter.ActiveSpace(appStateHandler.safeActiveSpaceId())
                                        )
                                    }
                                }
                                RoomListViewModel.SpaceFilterStrategy.NONE       -> roomQueryParams
                            }

                    ).also {
                        when (spaceFilterStrategy) {
                            RoomListViewModel.SpaceFilterStrategy.NORMAL     -> {
                                activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                                    override fun updateForSpaceId(roomId: String?) {
                                        it.updateQuery {
                                            it.copy(
                                                    activeSpaceId = ActiveSpaceFilter.ActiveSpace(roomId)
                                            )
                                        }
                                    }
                                })
                            }
                            RoomListViewModel.SpaceFilterStrategy.NOT_IF_ALL -> {
                                activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                                    override fun updateForSpaceId(roomId: String?) {
                                        if (roomId != null) {
                                            it.updateQuery {
                                                it.copy(
                                                        activeSpaceId = ActiveSpaceFilter.ActiveSpace(roomId)
                                                )
                                            }
                                        }
                                    }
                                })
                            }
                            RoomListViewModel.SpaceFilterStrategy.NONE       -> {
                                // we ignore current space for this one
                            }
                        }
                    }.livePagedList
                            .let { livePagedList ->

                                // use it also as a source to update count
                                livePagedList.asObservable()
                                        .observeOn(Schedulers.computation())
                                        .subscribe {
                                            sections.find { it.sectionName == name }
                                                    ?.notificationCount
                                                    ?.postValue(session.getNotificationCountForRooms(roomQueryParams))
                                        }.also {
                                            onDisposable.invoke(it)
                                        }

                                sections.add(
                                        RoomsSection(
                                                sectionName = name,
                                                livePages = livePagedList,
                                                notifyOfLocalEcho = notifyOfLocalEcho
                                        )
                                )
                            }
                }

        )
    }

    private fun withQueryParams(builder: (RoomSummaryQueryParams.Builder) -> Unit, block: (RoomSummaryQueryParams) -> Unit) {
        RoomSummaryQueryParams.Builder()
                .apply { builder.invoke(this) }
                .build()
                .let { block(it) }
    }
}
