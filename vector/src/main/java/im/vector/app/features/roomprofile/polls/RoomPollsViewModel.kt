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
import im.vector.app.features.roomprofile.polls.list.domain.GetLoadedPollsStatusUseCase
import im.vector.app.features.roomprofile.polls.list.domain.GetPollsUseCase
import im.vector.app.features.roomprofile.polls.list.domain.LoadMorePollsUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RoomPollsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomPollsViewState,
        private val getPollsUseCase: GetPollsUseCase,
        private val getLoadedPollsStatusUseCase: GetLoadedPollsStatusUseCase,
        private val loadMorePollsUseCase: LoadMorePollsUseCase,
) : VectorViewModel<RoomPollsViewState, RoomPollsAction, RoomPollsViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomPollsViewModel, RoomPollsViewState> {
        override fun create(initialState: RoomPollsViewState): RoomPollsViewModel
    }

    companion object : MavericksViewModelFactory<RoomPollsViewModel, RoomPollsViewState> by hiltMavericksViewModelFactory()

    init {
        updateLoadedPollStatus(initialState.roomId)
        observePolls()
        // TODO
        //  call use case to sync polls until now = initial loading
    }

    private fun updateLoadedPollStatus(roomId: String) {
        val loadedPollsStatus = getLoadedPollsStatusUseCase.execute(roomId)
        setState {
            copy(
                    canLoadMore = loadedPollsStatus.canLoadMore,
                    nbLoadedDays = loadedPollsStatus.nbLoadedDays
            )
        }
    }

    private fun observePolls() = withState { viewState ->
        getPollsUseCase.execute(viewState.roomId)
                .onEach { setState { copy(polls = it) } }
                .launchIn(viewModelScope)
    }

    // TODO add unit tests
    override fun handle(action: RoomPollsAction) {
        when (action) {
            RoomPollsAction.LoadMorePolls -> handleLoadMore()
        }
    }

    private fun handleLoadMore() = withState { viewState ->
        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val result = loadMorePollsUseCase.execute(viewState.roomId)
            setState {
                copy(
                        isLoadingMore = false,
                        canLoadMore = result.canLoadMore,
                        nbLoadedDays = result.nbLoadedDays,
                )
            }
        }
    }
}
