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
