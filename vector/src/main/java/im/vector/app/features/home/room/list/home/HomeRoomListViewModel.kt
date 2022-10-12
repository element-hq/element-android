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

import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.SpaceStateHandler
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toTrackingValue
import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.list.home.header.HomeRoomFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toOption
import org.matrix.android.sdk.flow.flow

class HomeRoomListViewModel @AssistedInject constructor(
        @Assisted initialState: HomeRoomListViewState,
        private val session: Session,
        private val spaceStateHandler: SpaceStateHandler,
        private val preferencesStore: HomeLayoutPreferencesStore,
        private val stringProvider: StringProvider,
        private val drawableProvider: DrawableProvider,
        private val analyticsTracker: AnalyticsTracker,
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
            .build()

    private val _roomsLivePagedList = MutableLiveData<PagedList<RoomSummary>>()
    val roomsLivePagedList: LiveData<PagedList<RoomSummary>> = _roomsLivePagedList

    private val internalPagedListObserver = Observer<PagedList<RoomSummary>> {
        _roomsLivePagedList.postValue(it)
    }

    private var filteredPagedRoomSummariesLive: UpdatableLivePageResult? = null

    init {
        observeOrderPreferences()
        observeInvites()
        observeRecents()
        observeFilterTabs()
        observeSpaceChanges()
    }

    private fun observeSpaceChanges() {
        spaceStateHandler.getSelectedSpaceFlow()
                .distinctUntilChanged()
                .onStart {
                    emit(spaceStateHandler.getCurrentSpace().toOption())
                }
                .onEach { selectedSpaceOption ->
                    val selectedSpace = selectedSpaceOption.orNull()
                    updateEmptyState()
                    filteredPagedRoomSummariesLive?.let { liveResults ->
                        liveResults.queryParams = liveResults.queryParams.copy(
                                spaceFilter = selectedSpace?.roomId.toActiveSpaceOrNoFilter()
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun observeInvites() {
        session.flow()
                .liveRoomSummaries(
                        roomSummaryQueryParams {
                            memberships = listOf(Membership.INVITE)
                        },
                        RoomSortOrder.ACTIVITY
                ).onEach { list ->
                    setState { copy(headersData = headersData.copy(invitesCount = list.size)) }
                }.launchIn(viewModelScope)
    }

    private fun observeRecents() {
        preferencesStore.areRecentsEnabledFlow
                .distinctUntilChanged()
                .flatMapLatest { areEnabled ->
                    if (areEnabled) {
                        session.flow()
                                .liveBreadcrumbs(roomSummaryQueryParams {
                                    memberships = listOf(Membership.JOIN)
                                })
                                .map { Optional.from(it) }
                    } else {
                        flowOf(Optional.empty())
                    }.onEach { listOptional ->
                        setState { copy(headersData = headersData.copy(recents = listOptional.getOrNull())) }
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeFilterTabs() {
        preferencesStore.areFiltersEnabledFlow
                .distinctUntilChanged()
                .flatMapLatest { areEnabled ->
                    getFilterTabsFlow(areEnabled)
                }.onEach { filtersOptional ->
                    val filters = filtersOptional.getOrNull()
                    if (!isCurrentFilterStillValid(filters)) {
                        changeRoomFilter(HomeRoomFilter.ALL)
                    }
                    setState {
                        copy(
                                headersData = headersData.copy(
                                        filtersList = filters,
                                )
                        )
                    }
                }.launchIn(viewModelScope)
    }

    private suspend fun isCurrentFilterStillValid(filtersList: List<HomeRoomFilter>?): Boolean {
        if (filtersList.isNullOrEmpty()) return false
        val currentFilter = awaitState().headersData.currentFilter
        return filtersList.contains(currentFilter)
    }

    private fun getFilterTabsFlow(isEnabled: Boolean): Flow<Optional<MutableList<HomeRoomFilter>>> {
        if (!isEnabled) return flowOf(Optional.empty())
        val spaceFLow = spaceStateHandler.getSelectedSpaceFlow()
                .distinctUntilChanged()
                .onStart {
                    emit(spaceStateHandler.getCurrentSpace().toOption())
                }
        val favouritesFlow =
                spaceFLow.flatMapLatest { selectedSpace ->
                    session.flow()
                            .liveRoomSummaries(
                                    RoomSummaryQueryParams.Builder().also { builder ->
                                        builder.spaceFilter = selectedSpace.orNull()?.roomId.toActiveSpaceOrNoFilter()
                                        builder.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
                                    }.build()
                            )
                }
                        .map { it.isNotEmpty() }
                        .distinctUntilChanged()

        val dmsFLow =
                spaceFLow.flatMapLatest { selectedSpace ->
                    session.flow()
                            .liveRoomSummaries(
                                    RoomSummaryQueryParams.Builder().also { builder ->
                                        builder.spaceFilter = selectedSpace.orNull()?.roomId.toActiveSpaceOrNoFilter()
                                        builder.memberships = listOf(Membership.JOIN)
                                        builder.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                    }.build()
                            )
                }
                        .map { it.isNotEmpty() }
                        .distinctUntilChanged()

        return combine(favouritesFlow, dmsFLow) { hasFavourite, hasDm ->
            hasFavourite to hasDm
        }.map { (hasFavourite, hasDm) ->
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
            Optional.from(filtersData)
        }
    }

    private fun observeRooms(currentFilter: HomeRoomFilter, isAZOrdering: Boolean) {
        filteredPagedRoomSummariesLive?.livePagedList?.removeObserver(internalPagedListObserver)
        val builder = RoomSummaryQueryParams.Builder().also {
            it.memberships = listOf(Membership.JOIN)
            it.spaceFilter = spaceStateHandler.getCurrentSpace()?.roomId.toActiveSpaceOrNoFilter()
        }
        val params = getFilteredQueryParams(currentFilter, builder.build())
        val sortOrder = if (isAZOrdering) {
            RoomSortOrder.NAME
        } else {
            RoomSortOrder.ACTIVITY
        }
        val liveResults = session.roomService().getFilteredPagedRoomSummariesLive(
                params,
                pagedListConfig,
                sortOrder
        ).also {
            filteredPagedRoomSummariesLive = it
        }
        liveResults.livePagedList.observeForever(internalPagedListObserver)
    }

    private fun observeOrderPreferences() {
        preferencesStore.isAZOrderingEnabledFlow
                .onEach { isAZOrdering ->
                    val currentFilter = awaitState().headersData.currentFilter
                    observeRooms(currentFilter, isAZOrdering)
                }.launchIn(viewModelScope)
    }

    private suspend fun updateEmptyState() {
        val currentFilter = awaitState().headersData.currentFilter
        val emptyState = getEmptyStateData(currentFilter, spaceStateHandler.getCurrentSpace())
        setState { copy(emptyState = emptyState) }
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

    private fun getEmptyStateData(filter: HomeRoomFilter, selectedSpace: RoomSummary?): StateView.State.Empty? {
        return when (filter) {
            HomeRoomFilter.ALL ->
                if (selectedSpace != null) {
                    StateView.State.Empty(
                            title = stringProvider.getString(R.string.home_empty_space_no_rooms_title, selectedSpace.displayName),
                            message = stringProvider.getString(R.string.home_empty_space_no_rooms_message),
                            image = drawableProvider.getDrawable(R.drawable.ill_empty_space),
                            isBigImage = true
                    )
                } else {
                    val userName = session.getUserOrDefault(session.myUserId).toMatrixItem().getBestName()
                    StateView.State.Empty(
                            title = stringProvider.getString(R.string.home_empty_no_rooms_title, userName),
                            message = stringProvider.getString(R.string.home_empty_no_rooms_message),
                            image = drawableProvider.getDrawable(R.drawable.ill_empty_all_chats),
                            isBigImage = true
                    )
                }
            HomeRoomFilter.UNREADS ->
                StateView.State.Empty(
                        title = stringProvider.getString(R.string.home_empty_no_unreads_title),
                        message = stringProvider.getString(R.string.home_empty_no_unreads_message),
                        image = drawableProvider.getDrawable(R.drawable.ill_empty_unreads),
                        isBigImage = true,
                        imageScaleType = ImageView.ScaleType.CENTER_INSIDE
                )
            else ->
                null
        }
    }

    override fun handle(action: HomeRoomListAction) {
        when (action) {
            is HomeRoomListAction.SelectRoom -> handleSelectRoom(action)
            is HomeRoomListAction.LeaveRoom -> handleLeaveRoom(action)
            is HomeRoomListAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is HomeRoomListAction.ToggleTag -> handleToggleTag(action)
            is HomeRoomListAction.ChangeRoomFilter -> handleChangeRoomFilter(action.filter)
            HomeRoomListAction.DeleteAllLocalRoom -> handleDeleteLocalRooms()
        }
    }

    override fun onCleared() {
        filteredPagedRoomSummariesLive?.livePagedList?.removeObserver(internalPagedListObserver)
        super.onCleared()
    }

    private fun handleChangeRoomFilter(newFilter: HomeRoomFilter) {
        viewModelScope.launch {
            changeRoomFilter(newFilter)
        }
    }

    private suspend fun changeRoomFilter(newFilter: HomeRoomFilter) {
        val currentFilter = awaitState().headersData.currentFilter
        if (currentFilter == newFilter) {
            return
        }
        setState { copy(headersData = headersData.copy(currentFilter = newFilter)) }
        updateEmptyState()
        analyticsTracker.updateUserProperties(UserProperties(allChatsActiveFilter = newFilter.toTrackingValue()))
        filteredPagedRoomSummariesLive?.let { liveResults ->
            liveResults.queryParams = getFilteredQueryParams(newFilter, liveResults.queryParams)
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
        viewModelScope.launch {
            val room = session.getRoom(action.roomId)
            if (room != null) {
                try {
                    room.roomPushRuleService().setRoomNotificationState(action.notificationState)
                } catch (failure: Throwable) {
                    _viewEvents.post(HomeRoomListViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun handleToggleTag(action: HomeRoomListAction.ToggleTag) {
        viewModelScope.launch {
            session.getRoom(action.roomId)?.let { room ->
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

    private fun handleDeleteLocalRooms() = withState {
        viewModelScope.launch {
            val localRoomIds = session.roomService()
                    .getRoomSummaries(roomSummaryQueryParams { roomId = QueryStringValue.Contains(RoomLocalEcho.PREFIX) })
                    .map { it.roomId }

            localRoomIds.forEach {
                session.roomService().deleteLocalRoom(it)
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
