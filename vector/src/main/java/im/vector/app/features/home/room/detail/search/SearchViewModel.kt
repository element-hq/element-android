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
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.internal.util.awaitCallback

class SearchViewModel @AssistedInject constructor(
        @Assisted private val initialState: SearchViewState,
        private val session: Session
) : VectorViewModel<SearchViewState, SearchAction, SearchViewEvents>(initialState) {

    private var room: Room? = null

    init {
        room = initialState.roomId?.let { session.getRoom(it) }
    }

    @AssistedInject.Factory
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
        if (action.searchTerm.length > 1) {
            setState {
                copy(searchTerm = action.searchTerm)
            }
            startSearching()
        }
    }

    private fun handleLoadMore() {
        startSearching(true)
    }

    private fun handleRetry() {
        startSearching()
    }

    private fun startSearching(isNextBatch: Boolean = false) = withState { state ->
        if (state.roomId == null || state.searchTerm == null) return@withState

        // There is no batch to retrieve
        if (isNextBatch && state.searchResult?.nextBatch == null) return@withState

        // Show full screen loading just for the clean search
        if (!isNextBatch) {
            setState {
                copy(
                        asyncEventsRequest = Loading()
                )
            }
        }

        viewModelScope.launch {
            try {
                val result = awaitCallback<SearchResult> {
                    room?.search(
                            searchTerm = state.searchTerm,
                            nextBatch = state.searchResult?.nextBatch,
                            orderByRecent = true,
                            beforeLimit = 0,
                            afterLimit = 0,
                            includeProfile = true,
                            limit = 20,
                            callback = it
                    )
                }
                onSearchResultSuccess(result, isNextBatch)
            } catch (failure: Throwable) {
                _viewEvents.post(SearchViewEvents.Failure(failure))
                setState {
                    copy(
                            asyncEventsRequest = Fail(failure),
                            searchResult = null
                    )
                }
            }
        }
    }

    private fun onSearchResultSuccess(searchResult: SearchResult, isNextBatch: Boolean) = withState { state ->
        val accumulatedResult = SearchResult(
                nextBatch = searchResult.nextBatch,
                results = searchResult.results,
                highlights = searchResult.highlights
        )

        // Accumulate results if it is the next batch
        if (isNextBatch) {
            if (state.searchResult != null) {
                accumulatedResult.results = accumulatedResult.results?.plus(state.searchResult.results!!)
            }
            if (state.searchResult?.highlights != null) {
                accumulatedResult.highlights = accumulatedResult.highlights?.plus(state.searchResult.highlights!!)
            }
        }

        setState {
            copy(
                    searchResult = accumulatedResult,
                    lastBatch = searchResult,
                    asyncEventsRequest = Success(Unit)
            )
        }
    }
}
