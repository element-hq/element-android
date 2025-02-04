/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.roomprofile.polls.list.domain.DisposePollHistoryUseCase
import im.vector.app.features.roomprofile.polls.list.domain.GetPollsUseCase
import im.vector.app.features.roomprofile.polls.list.domain.LoadMorePollsUseCase
import im.vector.app.features.roomprofile.polls.list.domain.SyncPollsUseCase
import im.vector.app.features.roomprofile.polls.list.ui.PollSummaryMapper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RoomPollsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomPollsViewState,
        private val getPollsUseCase: GetPollsUseCase,
        private val loadMorePollsUseCase: LoadMorePollsUseCase,
        private val syncPollsUseCase: SyncPollsUseCase,
        private val disposePollHistoryUseCase: DisposePollHistoryUseCase,
        private val pollSummaryMapper: PollSummaryMapper,
) : VectorViewModel<RoomPollsViewState, RoomPollsAction, RoomPollsViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomPollsViewModel, RoomPollsViewState> {
        override fun create(initialState: RoomPollsViewState): RoomPollsViewModel
    }

    companion object : MavericksViewModelFactory<RoomPollsViewModel, RoomPollsViewState> by hiltMavericksViewModelFactory()

    init {
        val roomId = initialState.roomId
        syncPolls(roomId)
        observePolls(roomId)
    }

    override fun onCleared() {
        withState { disposePollHistoryUseCase.execute(it.roomId) }
        super.onCleared()
    }

    private fun syncPolls(roomId: String) {
        viewModelScope.launch {
            setState { copy(isSyncing = true) }
            val result = runCatching {
                val loadedPollsStatus = syncPollsUseCase.execute(roomId)
                setState {
                    copy(
                            canLoadMore = loadedPollsStatus.canLoadMore,
                            nbSyncedDays = loadedPollsStatus.daysSynced,
                    )
                }
            }
            if (result.isFailure) {
                _viewEvents.post(RoomPollsViewEvent.LoadingError)
            }
            setState { copy(isSyncing = false) }
        }
    }

    private fun observePolls(roomId: String) {
        getPollsUseCase.execute(roomId)
                .map { it.mapNotNull { event -> pollSummaryMapper.map(event) } }
                .onEach { setState { copy(polls = it) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: RoomPollsAction) {
        when (action) {
            RoomPollsAction.LoadMorePolls -> handleLoadMore()
        }
    }

    private fun handleLoadMore() = withState { viewState ->
        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val result = runCatching {
                val status = loadMorePollsUseCase.execute(viewState.roomId)
                setState {
                    copy(
                            canLoadMore = status.canLoadMore,
                            nbSyncedDays = status.daysSynced,
                    )
                }
            }
            if (result.isFailure) {
                _viewEvents.post(RoomPollsViewEvent.LoadingError)
            }
            setState { copy(isLoadingMore = false) }
        }
    }
}
