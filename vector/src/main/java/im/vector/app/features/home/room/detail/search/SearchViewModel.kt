/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.search.SearchResult

class SearchViewModel @AssistedInject constructor(
        @Assisted private val initialState: SearchViewState,
        session: Session
) : VectorViewModel<SearchViewState, SearchAction, SearchViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)

    private var currentTask: Job? = null

    private var nextBatch: String? = null

    @AssistedFactory
    interface Factory {
        fun create(initialState: SearchViewState): SearchViewModel
    }

    companion object : MvRxViewModelFactory<SearchViewModel, SearchViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SearchViewState): SearchViewModel? {
            val fragment: SearchFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    override fun handle(action: SearchAction) {
        when (action) {
            is SearchAction.SearchWith -> handleSearchWith(action)
            is SearchAction.LoadMore   -> handleLoadMore()
            is SearchAction.Retry      -> handleRetry()
        }.exhaustive
    }

    private fun handleSearchWith(action: SearchAction.SearchWith) {
        if (action.searchTerm.isNotEmpty()) {
            setState {
                copy(
                        searchResult = emptyList(),
                        hasMoreResult = false,
                        lastBatchSize = 0,
                        searchTerm = action.searchTerm
                )
            }
            startSearching(false)
        }
    }

    private fun handleLoadMore() {
        startSearching(true)
    }

    private fun handleRetry() {
        startSearching(false)
    }

    private fun startSearching(isNextBatch: Boolean) = withState { state ->
        if (room == null) return@withState
        if (state.searchTerm == null) return@withState

        // There is no batch to retrieve
        if (isNextBatch && nextBatch == null) return@withState

        // Show full screen loading just for the clean search
        if (!isNextBatch) {
            setState {
                copy(
                        asyncSearchRequest = Loading()
                )
            }
        }

        currentTask?.cancel()

        currentTask = viewModelScope.launch {
            try {
                val result = room.search(
                        searchTerm = state.searchTerm,
                        nextBatch = nextBatch,
                        orderByRecent = true,
                        beforeLimit = 0,
                        afterLimit = 0,
                        includeProfile = true,
                        limit = 20
                )
                onSearchResultSuccess(result)
            } catch (failure: Throwable) {
                if (failure is CancellationException) return@launch

                _viewEvents.post(SearchViewEvents.Failure(failure))
                setState {
                    copy(
                            asyncSearchRequest = Fail(failure)
                    )
                }
            }
        }
    }

    private fun onSearchResultSuccess(searchResult: SearchResult) = withState { state ->
        val accumulatedResult = searchResult.results.orEmpty().plus(state.searchResult)

        // Note: We do not care about the highlights for the moment, but it will be the same algorithm

        nextBatch = searchResult.nextBatch

        setState {
            copy(
                    searchResult = accumulatedResult,
                    highlights = searchResult.highlights.orEmpty(),
                    hasMoreResult = !nextBatch.isNullOrEmpty(),
                    lastBatchSize = searchResult.results.orEmpty().size,
                    asyncSearchRequest = Success(Unit)
            )
        }
    }

    override fun onCleared() {
        currentTask?.cancel()
        super.onCleared()
    }
}
