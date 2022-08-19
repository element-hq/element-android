/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home

import androidx.paging.PagedList
import arrow.core.toOption
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.SpaceStateHandler
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.list.home.filter.HomeRoomFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.query.toActiveSpaceOrNoFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.flow.flow

class HomeRoomListViewModel @AssistedInject constructor(
        @Assisted initialState: HomeRoomListViewState,
        private val session: Session,
        private val spaceStateHandler: SpaceStateHandler,
        private val preferencesStore: HomeLayoutPreferencesStore,
) : VectorViewModel<HomeRoomListViewState, HomeRoomListAction, HomeRoomListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<HomeRoomListViewModel, HomeRoomListViewState> {
        override fun create(initialState: HomeRoomListViewState): HomeRoomListViewModel
    }

    companion object : MavericksViewModelFactory<HomeRoomListViewModel, HomeRoomListViewState> by hiltMavericksViewModelFactory()

    private val pagedListConfig = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(20)
            .setEnablePlaceholders(true)
            .setPrefetchDistance(10)
            .build()

    private val _sections = MutableSharedFlow<Set<HomeRoomSection>>(replay = 1)
    val sections = _sections.asSharedFlow()

    private var filteredPagedRoomSummariesLive: UpdatableLivePageResult? = null

    init {
        configureSections()
        observePreferences()
    }

    private fun observePreferences() {
        preferencesStore.areRecentsEnabledFlow.onEach {
            configureSections()
        }.launchIn(viewModelScope)

        preferencesStore.isAZOrderingEnabledFlow.onEach {
            configureSections()
        }.launchIn(viewModelScope)
    }

    private fun configureSections() = viewModelScope.launch {
        val newSections = mutableSetOf<HomeRoomSection>()

        val areSettingsEnabled = preferencesStore.areRecentsEnabledFlow.first()

        if (areSettingsEnabled) {
            newSections.add(getRecentRoomsSection())
        }
        newSections.add(getFilteredRoomsSection())

        _sections.emit(newSections)

        setState {
            copy(state = StateView.State.Content)
        }
    }

    private fun getRecentRoomsSection(): HomeRoomSection {
        val liveList = session.roomService()
                .getBreadcrumbsLive(roomSummaryQueryParams {
                    displayName = QueryStringValue.NoCondition
                    memberships = listOf(Membership.JOIN)
                })

        return HomeRoomSection.RecentRoomsData(
                list = liveList
        )
    }

    private suspend fun getFilteredRoomsSection(): HomeRoomSection.RoomSummaryData {
        val builder = RoomSummaryQueryParams.Builder().also {
            it.memberships = listOf(Membership.JOIN)
        }

        val params = getFilteredQueryParams(HomeRoomFilter.ALL, builder.build())
        val sortOrder = if (preferencesStore.isAZOrderingEnabledFlow.first()) {
            RoomSortOrder.NAME
        } else {
            RoomSortOrder.ACTIVITY
        }

        val liveResults = session.roomService().getFilteredPagedRoomSummariesLive(
                params,
                pagedListConfig,
                sortOrder
        ).also {
            this.filteredPagedRoomSummariesLive = it
        }

        spaceStateHandler.getSelectedSpaceFlow()
                .distinctUntilChanged()
                .onStart {
                    emit(spaceStateHandler.getCurrentSpace().toOption())
                }
                .onEach { selectedSpaceOption ->
                    val selectedSpace = selectedSpaceOption.orNull()
                    liveResults.queryParams = liveResults.queryParams.copy(
                            spaceFilter = selectedSpace?.roomId.toActiveSpaceOrNoFilter()
                    )
                }.launchIn(viewModelScope)

        return HomeRoomSection.RoomSummaryData(
                list = liveResults.livePagedList,
                filtersData = getFiltersDataFlow()
        )
    }

    private fun getFiltersDataFlow(): SharedFlow<Optional<List<HomeRoomFilter>>> {
        val flow = MutableSharedFlow<Optional<List<HomeRoomFilter>>>(replay = 1)

        val favouritesFlow = session.flow()
                .liveRoomSummaries(
                        RoomSummaryQueryParams.Builder().also { builder ->
                            builder.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
                        }.build()
                )
                .map { it.isNotEmpty() }
                .distinctUntilChanged()

        val dmsFLow = session.flow()
                .liveRoomSummaries(
                        RoomSummaryQueryParams.Builder().also { builder ->
                            builder.memberships = listOf(Membership.JOIN)
                            builder.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                        }.build()
                )
                .map { it.isNotEmpty() }
                .distinctUntilChanged()

        combine(favouritesFlow, dmsFLow, preferencesStore.areFiltersEnabledFlow) { hasFavourite, hasDm, areFiltersEnabled ->
            Triple(hasFavourite, hasDm, areFiltersEnabled)
        }.onEach { (hasFavourite, hasDm, areFiltersEnabled) ->
            if (areFiltersEnabled) {
                val filtersData = mutableListOf(
                        HomeRoomFilter.ALL,
                        HomeRoomFilter.UNREADS
                )
                if (hasFavourite) {
                    filtersData.add(
                            HomeRoomFilter.FAVOURITES
                    )
                }
                if (hasDm) {
                    filtersData.add(
                            HomeRoomFilter.PEOPlE
                    )
                }
                flow.emit(Optional.from(filtersData))
            } else {
                flow.emit(Optional.empty())
            }
        }.launchIn(viewModelScope)

        return flow
    }

    private fun getFilteredQueryParams(filter: HomeRoomFilter, currentParams: RoomSummaryQueryParams): RoomSummaryQueryParams {
        return when (filter) {
            HomeRoomFilter.ALL -> currentParams.copy(
                    roomCategoryFilter = null,
                    roomTagQueryFilter = null
            )
            HomeRoomFilter.UNREADS -> currentParams.copy(
                    roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS,
                    roomTagQueryFilter = RoomTagQueryFilter(null, false, null)
            )
            HomeRoomFilter.FAVOURITES ->
                currentParams.copy(
                        roomCategoryFilter = null,
                        roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
                )
            HomeRoomFilter.PEOPlE -> currentParams.copy(
                    roomCategoryFilter = RoomCategoryFilter.ONLY_DM,
                    roomTagQueryFilter = null
            )
        }
    }

    override fun handle(action: HomeRoomListAction) {
        when (action) {
            is HomeRoomListAction.SelectRoom -> handleSelectRoom(action)
            is HomeRoomListAction.LeaveRoom -> handleLeaveRoom(action)
            is HomeRoomListAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is HomeRoomListAction.ToggleTag -> handleToggleTag(action)
            is HomeRoomListAction.ChangeRoomFilter -> handleChangeRoomFilter(action)
        }
    }

    private fun handleChangeRoomFilter(action: HomeRoomListAction.ChangeRoomFilter) {
        filteredPagedRoomSummariesLive?.let { liveResults ->
            liveResults.queryParams = getFilteredQueryParams(action.filter, liveResults.queryParams)
        }
    }

    fun isPublicRoom(roomId: String): Boolean {
        return session.getRoom(roomId)?.stateService()?.isPublic().orFalse()
    }

    private fun handleSelectRoom(action: HomeRoomListAction.SelectRoom) = withState {
        _viewEvents.post(HomeRoomListViewEvents.SelectRoom(action.roomSummary, false))
    }

    private fun handleLeaveRoom(action: HomeRoomListAction.LeaveRoom) {
        _viewEvents.post(HomeRoomListViewEvents.Loading(null))
        viewModelScope.launch {
            val value = runCatching { session.roomService().leaveRoom(action.roomId) }
                    .fold({ HomeRoomListViewEvents.Done }, { HomeRoomListViewEvents.Failure(it) })
            _viewEvents.post(value)
        }
    }

    private fun handleChangeNotificationMode(action: HomeRoomListAction.ChangeRoomNotificationState) {
        val room = session.getRoom(action.roomId)
        if (room != null) {
            viewModelScope.launch {
                try {
                    room.roomPushRuleService().setRoomNotificationState(action.notificationState)
                } catch (failure: Exception) {
                    _viewEvents.post(HomeRoomListViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun handleToggleTag(action: HomeRoomListAction.ToggleTag) {
        session.getRoom(action.roomId)?.let { room ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (room.roomSummary()?.hasTag(action.tag) == false) {
                        // Favorite and low priority tags are exclusive, so maybe delete the other tag first
                        action.tag.otherTag()
                                ?.takeIf { room.roomSummary()?.hasTag(it).orFalse() }
                                ?.let { tagToRemove ->
                                    room.tagsService().deleteTag(tagToRemove)
                                }

                        // Set the tag. We do not handle the order for the moment
                        room.tagsService().addTag(action.tag, 0.5)
                    } else {
                        room.tagsService().deleteTag(action.tag)
                    }
                } catch (failure: Throwable) {
                    _viewEvents.post(HomeRoomListViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun String.otherTag(): String? {
        return when (this) {
            RoomTag.ROOM_TAG_FAVOURITE -> RoomTag.ROOM_TAG_LOW_PRIORITY
            RoomTag.ROOM_TAG_LOW_PRIORITY -> RoomTag.ROOM_TAG_FAVOURITE
            else -> null
        }
    }
}
