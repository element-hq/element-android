/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.paging.PagedList
import com.airbnb.mvrx.Async
import im.vector.app.SpaceStateHandler
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.showInvites
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.query.toActiveSpaceOrNoFilter
import org.matrix.android.sdk.api.query.toActiveSpaceOrOrphanRooms
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import timber.log.Timber

class RoomListSectionBuilder(
        private val session: Session,
        private val stringProvider: StringProvider,
        private val spaceStateHandler: SpaceStateHandler,
        private val viewModelScope: CoroutineScope,
        private val autoAcceptInvites: AutoAcceptInvites,
        private val onUpdatable: (UpdatableLivePageResult) -> Unit,
        private val suggestedRoomJoiningState: LiveData<Map<String, Async<Unit>>>,
        private val onlyOrphansInHome: Boolean = false
) {

    private val pagedListConfig = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(20)
            .setEnablePlaceholders(true)
            .setPrefetchDistance(10)
            .build()

    fun buildSections(mode: RoomListDisplayMode): List<RoomsSection> {
        val sections = mutableListOf<RoomsSection>()
        val activeSpaceAwareQueries = mutableListOf<RoomListViewModel.ActiveSpaceQueryUpdater>()
        when (mode) {
            RoomListDisplayMode.PEOPLE -> {
                // 4 sections Invites / Fav / Dms / Low Priority
                buildDmSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.ROOMS -> {
                // 6 sections invites / Fav / Rooms / Low Priority / Server notice / Suggested rooms
                buildRoomsSections(sections, activeSpaceAwareQueries)
            }
            RoomListDisplayMode.FILTERED -> {
                // Used when searching for rooms
                buildFilteredSection(sections)
            }
            RoomListDisplayMode.NOTIFICATIONS -> {
                buildNotificationsSection(sections, activeSpaceAwareQueries)
            }
        }

        spaceStateHandler.getSelectedSpaceFlow()
                .distinctUntilChanged()
                .onEach { selectedSpaceOption ->
                    val selectedSpace = selectedSpaceOption.orNull()
                    activeSpaceAwareQueries.onEach { updater ->
                        updater.updateForSpaceId(selectedSpace?.roomId)
                    }
                }.launchIn(viewModelScope)

        return sections
    }

    private fun buildRoomsSections(
            sections: MutableList<RoomsSection>,
            activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>
    ) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections = sections,
                    activeSpaceUpdaters = activeSpaceAwareQueries,
                    nameRes = CommonStrings.invitations_header,
                    notifyOfLocalEcho = true,
                    spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL,
                    countRoomAsNotif = true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            }
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                CommonStrings.bottom_action_favourites,
                false,
                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = CommonStrings.bottom_action_rooms,
                notifyOfLocalEcho = false,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(isFavorite = false, isLowPriority = false, isServerNotice = false)
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = CommonStrings.low_priority_header,
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
                nameRes = CommonStrings.system_alerts_header,
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
        val suggestedRoomsFlow = // MutableLiveData<List<SpaceChildInfo>>()
                spaceStateHandler.getSelectedSpaceFlow()
                        .distinctUntilChanged()
                        .flatMapLatest { selectedSpaceOption ->
                            val selectedSpace = selectedSpaceOption.orNull()
                            if (selectedSpace == null) {
                                flowOf(emptyList())
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
                                }.asFlow()
                            }
                        }

        val liveSuggestedRooms = MutableLiveData<SuggestedRoomInfo>()
        combine(
                suggestedRoomsFlow,
                suggestedRoomJoiningState.asFlow()
        ) { rooms, joinStates ->
            SuggestedRoomInfo(
                    rooms,
                    joinStates
            )
        }.onEach {
            liveSuggestedRooms.postValue(it)
        }.launchIn(viewModelScope)

        sections.add(
                RoomsSection(
                        sectionName = stringProvider.getString(CommonStrings.suggested_header),
                        liveSuggested = liveSuggestedRooms,
                        notifyOfLocalEcho = false,
                        itemCount = suggestedRoomsFlow.map { suggestions -> suggestions.size }
                )
        )
    }

    private fun buildDmSections(
            sections: MutableList<RoomsSection>,
            activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>
    ) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections = sections,
                    activeSpaceUpdaters = activeSpaceAwareQueries,
                    nameRes = CommonStrings.invitations_header,
                    notifyOfLocalEcho = true,
                    spaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL,
                    countRoomAsNotif = true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            }
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                CommonStrings.bottom_action_favourites,
                false,
                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                CommonStrings.bottom_action_people_x,
                false,
                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(isFavorite = false, isLowPriority = false, isServerNotice = null)
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                CommonStrings.low_priority_header,
                false,
                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(isFavorite = false, isLowPriority = true, isServerNotice = null)
        }
    }

    private fun buildNotificationsSection(
            sections: MutableList<RoomsSection>,
            activeSpaceAwareQueries: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>
    ) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections = sections,
                    activeSpaceUpdaters = activeSpaceAwareQueries,
                    nameRes = CommonStrings.invitations_header,
                    notifyOfLocalEcho = true,
                    spaceFilterStrategy = if (onlyOrphansInHome) {
                        RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                    } else {
                        RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                    },
                    countRoomAsNotif = true
            ) {
                it.memberships = listOf(Membership.INVITE)
            }
        }

        addSection(
                sections = sections,
                activeSpaceUpdaters = activeSpaceAwareQueries,
                nameRes = CommonStrings.bottom_action_rooms,
                notifyOfLocalEcho = false,
                spaceFilterStrategy = if (onlyOrphansInHome) {
                    RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL
                } else {
                    RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL
                }
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
        }
    }

    private fun buildFilteredSection(sections: MutableList<RoomsSection>) {
        // Used when searching for rooms
        withQueryParams(
                {
                    it.memberships = Membership.activeMemberships()
                },
                { queryParams ->
                    val name = stringProvider.getString(CommonStrings.bottom_action_rooms)
                    val updatableFilterLivePageResult = session.roomService().getFilteredPagedRoomSummariesLive(queryParams)
                    onUpdatable(updatableFilterLivePageResult)

                    val itemCountFlow = updatableFilterLivePageResult.livePagedList.asFlow()
                            .flatMapLatest { session.roomService().getRoomCountLive(updatableFilterLivePageResult.queryParams).asFlow() }
                            .distinctUntilChanged()

                    sections.add(
                            RoomsSection(
                                    sectionName = name,
                                    livePages = updatableFilterLivePageResult.livePagedList,
                                    itemCount = itemCountFlow
                            )
                    )
                }
        )
    }

    private fun addSection(
            sections: MutableList<RoomsSection>,
            activeSpaceUpdaters: MutableList<RoomListViewModel.ActiveSpaceQueryUpdater>,
            @StringRes nameRes: Int,
            notifyOfLocalEcho: Boolean = false,
            spaceFilterStrategy: RoomListViewModel.SpaceFilterStrategy = RoomListViewModel.SpaceFilterStrategy.NONE,
            countRoomAsNotif: Boolean = false,
            query: (RoomSummaryQueryParams.Builder) -> Unit
    ) {
        withQueryParams(query) { roomQueryParams ->
            val updatedQueryParams = roomQueryParams.process(spaceFilterStrategy, spaceStateHandler.getSafeActiveSpaceId())
            val liveQueryParams = MutableStateFlow(updatedQueryParams)
            val itemCountFlow = liveQueryParams
                    .flatMapLatest {
                        session.roomService().getRoomCountLive(it).asFlow()
                    }
                    .flowOn(Dispatchers.Main)
                    .distinctUntilChanged()

            val name = stringProvider.getString(nameRes)
            val filteredPagedRoomSummariesLive = session.roomService().getFilteredPagedRoomSummariesLive(
                    roomQueryParams.process(spaceFilterStrategy, spaceStateHandler.getSafeActiveSpaceId()),
                    pagedListConfig
            )
            when (spaceFilterStrategy) {
                RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL -> {
                    activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                        override fun updateForSpaceId(roomId: String?) {
                            filteredPagedRoomSummariesLive.queryParams = roomQueryParams.copy(
                                    spaceFilter = roomId.toActiveSpaceOrOrphanRooms()
                            )
                            liveQueryParams.update { filteredPagedRoomSummariesLive.queryParams }
                        }
                    })
                }
                RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL -> {
                    activeSpaceUpdaters.add(object : RoomListViewModel.ActiveSpaceQueryUpdater {
                        override fun updateForSpaceId(roomId: String?) {
                            if (roomId != null) {
                                filteredPagedRoomSummariesLive.queryParams = roomQueryParams.copy(
                                        spaceFilter = SpaceFilter.ActiveSpace(roomId)
                                )
                            } else {
                                filteredPagedRoomSummariesLive.queryParams = roomQueryParams.copy(
                                        spaceFilter = SpaceFilter.NoFilter
                                )
                            }
                            liveQueryParams.update { filteredPagedRoomSummariesLive.queryParams }
                        }
                    })
                }
                RoomListViewModel.SpaceFilterStrategy.NONE -> {
                    // we ignore current space for this one
                }
            }

            val livePagedList = filteredPagedRoomSummariesLive.livePagedList
            // use it also as a source to update count
            livePagedList.asFlow()
                    .onEach {
                        Timber.v("Thread space list: ${Thread.currentThread()}")
                        sections.find { it.sectionName == name }
                                ?.notificationCount
                                ?.postValue(
                                        if (countRoomAsNotif) {
                                            RoomAggregateNotificationCount(it.size, it.size)
                                        } else {
                                            session.roomService().getNotificationCountForRooms(
                                                    roomQueryParams.process(spaceFilterStrategy, spaceStateHandler.getSafeActiveSpaceId())
                                            )
                                        }
                                )
                    }
                    .flowOn(Dispatchers.Default)
                    .launchIn(viewModelScope)

            sections.add(
                    RoomsSection(
                            sectionName = name,
                            livePages = livePagedList,
                            notifyOfLocalEcho = notifyOfLocalEcho,
                            itemCount = itemCountFlow
                    )
            )
        }
    }

    private fun withQueryParams(builder: (RoomSummaryQueryParams.Builder) -> Unit, block: (RoomSummaryQueryParams) -> Unit) {
        block(roomSummaryQueryParams { builder.invoke(this) })
    }

    internal fun RoomSummaryQueryParams.process(spaceFilter: RoomListViewModel.SpaceFilterStrategy, currentSpace: String?): RoomSummaryQueryParams {
        return when (spaceFilter) {
            RoomListViewModel.SpaceFilterStrategy.ORPHANS_IF_SPACE_NULL -> {
                copy(
                        spaceFilter = currentSpace.toActiveSpaceOrOrphanRooms()
                )
            }
            RoomListViewModel.SpaceFilterStrategy.ALL_IF_SPACE_NULL -> {
                copy(
                        spaceFilter = currentSpace.toActiveSpaceOrNoFilter()
                )
            }
            RoomListViewModel.SpaceFilterStrategy.NONE -> this
        }
    }
}
