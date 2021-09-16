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
import androidx.paging.PagedList
import com.airbnb.mvrx.Async
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.showInvites
import im.vector.app.space
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.rx.asObservable

class RoomListSectionBuilderSpace(
        private val session: Session,
        private val stringProvider: StringProvider,
        private val appStateHandler: AppStateHandler,
        private val viewModelScope: CoroutineScope,
        private val autoAcceptInvites: AutoAcceptInvites,
        private val onUpdatable: (UpdatableLivePageResult) -> Unit,
        private val suggestedRoomJoiningState: LiveData<Map<String, Async<Unit>>>,
        private val onlyOrphansInHome: Boolean = false
) : RoomListSectionBuilder {

    private val disposables = CompositeDisposable()

    private val pagedListConfig = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(20)
            .setEnablePlaceholders(true)
            .setPrefetchDistance(10)
            .build()

    override fun buildSections(mode: RoomListDisplayMode): List<RoomsSection> {
        val sections = mutableListOf<RoomsSection>()
        val activeSpaceAwareQueries = mutableListOf<RoomListViewModel.ActiveSpaceQueryUpdater>()
        when (mode) {
            RoomListDisplayMode.PEOPLE        -> {
                // 2 sections  Dms / Low Priority
                buildDmSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.ROOMS         -> {
                // 4 sections  Rooms / Low Priority / Server notice / Suggested rooms
                buildRoomsSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.FILTERED -> {
                // Used when searching for rooms
                withQueryParams(
                        {
                            it.memberships = Membership.activeMemberships()
                        },
                        { qpm ->
                            val name = stringProvider.getString(R.string.bottom_action_rooms)
                            session.getFilteredPagedRoomSummariesLive(qpm)
                                    .let { updatableFilterLivePageResult ->
                                        onUpdatable(updatableFilterLivePageResult)
                                        sections.add(RoomsSection(name, updatableFilterLivePageResult.livePagedList))
                                    }
                        }
                )
            }
            RoomListDisplayMode.HOME     -> {
                buildHomeSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.ALL_IN_ONE     -> {
                buildAllInOneSections(sections, activeSpaceAwareQueries)
            }
        }

        appStateHandler.selectedRoomGroupingObservable
                .distinctUntilChanged()
                .subscribe { groupingMethod ->
                    val selectedSpace = groupingMethod.orNull()?.space()
                    activeSpaceAwareQueries.onEach { updater ->
                        updater.updateForSpaceId(selectedSpace?.roomId)
                    }
                }.also {
                    disposables.add(it)
                }

        return sections
    }

    private fun buildHomeSections(sections: MutableList<RoomsSection>, activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections = sections,
                    activeSpaceUpdaters = activeSpaceAwareQueries,
                    nameRes = R.string.invitations_header,
                    notifyOfLocalEcho = true,
                    spaceFilterStrategy = if (onlyOrphansInHome) {
                        RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                    } else {
                        RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                    },
                    countRoomAsNotif = true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ALL
            }
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_favourites,
                notifyOfLocalEcho = false,
                isCompact = true,
                spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.room_list_section_unread_dm,
                notifyOfLocalEcho = false,
                isCompact = true,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.UNREAD_NOTIFICATION_DMS
            it.roomTagQueryFilter = RoomTagQueryFilter(false, null, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.room_list_section_unread_rooms,
                notifyOfLocalEcho = false,
                isCompact = true,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.UNREAD_NOTIFICATION_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(false, null, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_rooms,
                notifyOfLocalEcho = false,
                isCompact = true,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                },
                sortOrder = RoomSortOrder.UNREAD
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ROOMS_WITH_NO_NOTIFICATION
            it.roomTagQueryFilter = RoomTagQueryFilter(false, null, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_people_x,
                notifyOfLocalEcho = false,
                isCompact = true,
                spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.DMS_WITH_NO_NOTIFICATION
        }

        val suggestedRoomsObservable = // MutableLiveData<List<SpaceChildInfo>>()
                appStateHandler.selectedRoomGroupingObservable
                        .distinctUntilChanged()
                        .switchMap { groupingMethod ->
                            val selectedSpace = groupingMethod.orNull()?.space()
                            if (selectedSpace == null) {
                                Observable.just(emptyList())
                            } else {
                                liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                                    val spaceSum = tryOrNull {
                                        session.spaceService()
                                                .querySpaceChildren(selectedSpace.roomId, suggestedOnly = true, null, null)
                                    }
                                    val value = spaceSum?.children.orEmpty().distinctBy { it.childRoomId }
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
            disposables.add(it)
        }
        sections.add(
                RoomsSection(
                        sectionName = stringProvider.getString(R.string.suggested_header),
                        liveSuggested = liveSuggestedRooms,
                        notifyOfLocalEcho = false
                )
        )
    }

    private fun buildAllInOneSections(sections: MutableList<RoomsSection>, activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections = sections,
                    activeSpaceUpdaters = activeSpaceAwareQueries,
                    nameRes = R.string.invitations_header,
                    notifyOfLocalEcho = true,
                    spaceFilterStrategy = if (onlyOrphansInHome) {
                        RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                    } else {
                        RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                    },
                    countRoomAsNotif = true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ALL
            }
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_favourites,
                notifyOfLocalEcho = false,
                isCompact = false,
                spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ALL
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        val filterStrat = if (onlyOrphansInHome) {
            RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
        } else {
            RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        }
        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_rooms,
                notifyOfLocalEcho = false,
                spaceFilterStrategy =  filterStrat
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomTagQueryFilter = RoomTagQueryFilter(null, false, false)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.low_priority_header,
                notifyOfLocalEcho = false,
                isCompact = false,
                spaceFilterStrategy = filterStrat

        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ROOMS_WITH_NO_NOTIFICATION
            it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.system_alerts_header,
                notifyOfLocalEcho = false,
                isCompact = false,
                spaceFilterStrategy = filterStrat
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ALL
            it.roomTagQueryFilter = RoomTagQueryFilter(null, null, true)
        }

        val suggestedRoomsObservable = // MutableLiveData<List<SpaceChildInfo>>()
                appStateHandler.selectedRoomGroupingObservable
                        .distinctUntilChanged()
                        .switchMap { groupingMethod ->
                            val selectedSpace = groupingMethod.orNull()?.space()
                            if (selectedSpace == null) {
                                Observable.just(emptyList())
                            } else {
                                liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                                    val spaceSum = tryOrNull {
                                        session.spaceService()
                                                .querySpaceChildren(selectedSpace.roomId, suggestedOnly = true, null, null)
                                    }
                                    val value = spaceSum?.children.orEmpty().distinctBy { it.childRoomId }
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
            disposables.add(it)
        }
        sections.add(
                RoomsSection(
                        sectionName = stringProvider.getString(R.string.suggested_header),
                        liveSuggested = liveSuggestedRooms,
                        notifyOfLocalEcho = false
                )
        )
    }

    private fun buildRoomsSections(sections: MutableList<RoomsSection>,
                                   activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
//        if (autoAcceptInvites.showInvites()) {
//            addSection(
//                    sections = sections,
//                    activeSpaceUpdaters = activeSpaceAwareQueries,
//                    nameRes = R.string.invitations_header,
//                    notifyOfLocalEcho = true,
//                    spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL,
//                    countRoomAsNotif = true
//            ) {
//                it.memberships = listOf(Membership.INVITE)
//                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
//            }
//        }

//        addSection(
//                sections,
//                activeSpaceAwareQueries,
//                R.string.bottom_action_favourites,
//                false,
//                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
//        ) {
//            it.memberships = listOf(Membership.JOIN)
//            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
//            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
//        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_rooms,
                notifyOfLocalEcho = false,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, false, false)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.low_priority_header,
                notifyOfLocalEcho = false,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.system_alerts_header,
                notifyOfLocalEcho = false,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, null, true)
        }

        // add suggested rooms
        val suggestedRoomsObservable = // MutableLiveData<List<SpaceChildInfo>>()
                appStateHandler.selectedRoomGroupingObservable
                        .distinctUntilChanged()
                        .switchMap { groupingMethod ->
                            val selectedSpace = groupingMethod.orNull()?.space()
                            if (selectedSpace == null) {
                                Observable.just(emptyList())
                            } else {
                                liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                                    val spaceSum = tryOrNull {
                                        session.spaceService()
                                                .querySpaceChildren(selectedSpace.roomId, suggestedOnly = true, null, null)
                                    }
                                    val value = spaceSum?.children.orEmpty().distinctBy { it.childRoomId }
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
            disposables.add(it)
        }
        sections.add(
                RoomsSection(
                        sectionName = stringProvider.getString(R.string.suggested_header),
                        liveSuggested = liveSuggestedRooms,
                        notifyOfLocalEcho = false
                )
        )
    }

    private fun buildDmSections(sections: MutableList<RoomsSection>,
                                activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>) {
//        if (autoAcceptInvites.showInvites()) {
//            addSection(
//                    sections = sections,
//                    activeSpaceUpdaters = activeSpaceAwareQueries,
//                    nameRes = R.string.invitations_header,
//                    notifyOfLocalEcho = true,
//                    spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL,
//                    countRoomAsNotif = true
//            ) {
//                it.memberships = listOf(Membership.INVITE)
//                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
//            }
//        }

//        addSection(
//                sections,
//                activeSpaceAwareQueries,
//                R.string.bottom_action_favourites,
//                false,
//                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
//        ) {
//            it.memberships = listOf(Membership.JOIN)
//            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
//            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
//        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.bottom_action_people_x,
                notifyOfLocalEcho = false,
                isCompact = false,
                spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(null, false, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = R.string.low_priority_header,
                notifyOfLocalEcho = false,
                isCompact = false,
                spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
        }
    }

    private fun addSection(sections: MutableList<RoomsSection>,
                           activeSpaceUpdaters: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>,
                           @StringRes nameRes: Int,
                           notifyOfLocalEcho: Boolean = false,
                           isCompact: Boolean = false,
                           spaceFilterStrategy: RoomListViewModel.SpaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.NONE,
                           sortOrder: RoomSortOrder = RoomSortOrder.ACTIVITY,
                           countRoomAsNotif: Boolean = false,
                           query: (RoomSummaryQueryParams.Builder) -> Unit) {
        withQueryParams(
                { query.invoke(it) },
                { roomQueryParams ->
                    val name = stringProvider.getString(nameRes)
                    session.getFilteredPagedRoomSummariesLive(
                            roomQueryParams.process(spaceFilterStrategy, appStateHandler.safeActiveSpaceId()),
                            pagedListConfig,
                            sortOrder
                    ).also {
                        when (spaceFilterStrategy) {
                            RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL -> {
                                activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                                    override fun updateForSpaceId(roomId: String?) {
                                        it.updateQuery {
                                            it.copy(
                                                    activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(roomId)
                                            )
                                        }
                                    }
                                })
                            }
                            RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL     -> {
                                activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                                    override fun updateForSpaceId(roomId: String?) {
                                        if (roomId != null) {
                                            it.updateQuery {
                                                it.copy(
                                                        activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(roomId)
                                                )
                                            }
                                        } else {
                                            it.updateQuery {
                                                it.copy(
                                                        activeSpaceFilter = ActiveSpaceFilter.None
                                                )
                                            }
                                        }
                                    }
                                })
                            }
                            RoomListViewModel.SpaceFilterStrategy.NONE                  -> {
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
                                                    ?.postValue(
                                                            if (countRoomAsNotif) {
                                                                RoomAggregateNotificationCount(it.size, it.size)
                                                            } else {
                                                                session.getNotificationCountForRooms(
                                                                        roomQueryParams.process(spaceFilterStrategy, appStateHandler.safeActiveSpaceId())
                                                                )
                                                            }
                                                    )
                                        }.also {
                                            disposables.add(it)
                                        }

                                sections.add(
                                        RoomsSection(
                                                sectionName = name,
                                                livePages = livePagedList,
                                                isCompact = isCompact,
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

    internal fun RoomSummaryQueryParams.process(spaceFilter: RoomListViewModel.SpaceFilterStrategy, currentSpace: String?): RoomSummaryQueryParams {
        return when (spaceFilter) {
            RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL -> {
                copy(
                        activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(currentSpace)
                )
            }
            RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL     -> {
                if (currentSpace == null) {
                    copy(
                            activeSpaceFilter = ActiveSpaceFilter.None
                    )
                } else {
                    copy(
                            activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(currentSpace)
                    )
                }
            }
            RoomListViewModel.SpaceFilterStrategy.NONE                  -> this
        }
    }

    override fun dispose() {
        disposables.dispose()
    }
}
