/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home

import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import arrow.core.Option
import arrow.core.toOption
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
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.list.home.header.HomeRoomFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
import org.matrix.android.sdk.flow.flow

class HomeRoomListViewModel @AssistedInject constructor(
        @Assisted initialState: HomeRoomListViewState,
        private val session: Session,
        private val spaceStateHandler: SpaceStateHandler,
        private val preferencesStore: HomeLayoutPreferencesStore,
        private val stringProvider: StringProvider,
        private val drawableProvider: DrawableProvider,
) : VectorViewModel<HomeRoomListViewState, HomeRoomListAction, HomeRoomListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<HomeRoomListViewModel, HomeRoomListViewState> {
        override fun create(initialState: HomeRoomListViewState): HomeRoomListViewModel
    }

    companion object : MavericksViewModelFactory<HomeRoomListViewModel, HomeRoomListViewState> by hiltMavericksViewModelFactory()

    private var roomsFlow: Flow<Option<RoomSummary>>? = null
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

    private var currentFilter: HomeRoomFilter = HomeRoomFilter.ALL
    private val _emptyStateFlow = MutableSharedFlow<Optional<StateView.State.Empty>>(replay = 1)
    val emptyStateFlow = _emptyStateFlow.asSharedFlow()

    private var filteredPagedRoomSummariesLive: UpdatableLivePageResult? = null

    init {
        observeOrderPreferences()
        observeInvites()
        observeRecents()
        observeFilterTabs()
        observeRooms()
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
                        flow { emit(Optional.empty()) }
                    }.onEach { listOptional ->
                        setState { copy(headersData = headersData.copy(recents = listOptional.getOrNull())) }
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeFilterTabs() {
        preferencesStore.areFiltersEnabledFlow
                .distinctUntilChanged()
                .flatMapLatest { areEnabled ->
                    if (areEnabled) {
                        getFilterTabsFlow()
                    } else {
                        flow { emit(Optional.empty()) }
                    }.onEach { filtersOptional ->
                        setState {
                            validateCurrentFilter(filtersOptional.getOrNull())
                            copy(
                                    headersData = headersData.copy(
                                            filtersList = filtersOptional.getOrNull(),
                                            currentFilter = currentFilter
                                    )
                            )
                        }
                    }
                }.launchIn(viewModelScope)
    }

    private fun validateCurrentFilter(filtersList: List<HomeRoomFilter>?) {
        if (filtersList?.contains(currentFilter) != true) {
            handleChangeRoomFilter(HomeRoomFilter.ALL)
        }
    }

    private fun getFilterTabsFlow(): Flow<Optional<MutableList<HomeRoomFilter>>> {
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

    private fun observeRooms() = viewModelScope.launch {
        filteredPagedRoomSummariesLive?.livePagedList?.removeObserver(internalPagedListObserver)

        val builder = RoomSummaryQueryParams.Builder().also {
            it.memberships = listOf(Membership.JOIN)
        }

        val params = getFilteredQueryParams(currentFilter, builder.build())
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
            filteredPagedRoomSummariesLive = it
        }

        spaceStateHandler.getSelectedSpaceFlow()
                .distinctUntilChanged()
                .onStart {
                    emit(spaceStateHandler.getCurrentSpace().toOption())
                }
                .onEach { selectedSpaceOption ->
                    val selectedSpace = selectedSpaceOption.orNull()
                    filteredPagedRoomSummariesLive?.queryParams = liveResults.queryParams.copy(
                            spaceFilter = selectedSpace?.roomId.toActiveSpaceOrNoFilter()
                    )
                    emitEmptyState()
                }
                .also { roomsFlow = it }
                .launchIn(viewModelScope)

        liveResults.livePagedList.observeForever(internalPagedListObserver)
    }

    private fun observeOrderPreferences() {
        preferencesStore.isAZOrderingEnabledFlow.onEach {
            observeRooms()
        }.launchIn(viewModelScope)
    }

    private fun emitEmptyState() {
        viewModelScope.launch {
            val emptyState = getEmptyStateData(currentFilter, spaceStateHandler.getCurrentSpace())
            _emptyStateFlow.emit(Optional.from(emptyState))
        }
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
        super.onCleared()
        filteredPagedRoomSummariesLive?.livePagedList?.removeObserver(internalPagedListObserver)
    }

    private fun handleChangeRoomFilter(newFilter: HomeRoomFilter) {
        if (currentFilter == newFilter) {
            return
        }
        currentFilter = newFilter
        filteredPagedRoomSummariesLive?.let { liveResults ->
            liveResults.queryParams = getFilteredQueryParams(currentFilter, liveResults.queryParams)
        }

        setState { copy(headersData = headersData.copy(currentFilter = currentFilter)) }
        emitEmptyState()
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
                } catch (failure: Throwable) {
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

    private fun handleDeleteLocalRooms() = withState {
        val localRoomIds = session.roomService()
                .getRoomSummaries(roomSummaryQueryParams { roomId = QueryStringValue.Contains(RoomLocalEcho.PREFIX) })
                .map { it.roomId }

        viewModelScope.launch {
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
