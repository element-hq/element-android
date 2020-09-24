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

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.search.SearchResult

class SearchViewModel @AssistedInject constructor(
        @Assisted private val initialState: SearchViewState,
        private val session: Session
) : VectorViewModel<SearchViewState, SearchAction, SearchViewEvents>(initialState) {

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
            is SearchAction.SearchWith    -> handleSearchWith(action)
            is SearchAction.ScrolledToTop -> handleScrolledToTop()
            is SearchAction.Retry         -> handleRetry()
        }.exhaustive
    }

    private fun handleSearchWith(action: SearchAction.SearchWith) {
        if (action.searchTerm.length > 1) {
            setState {
                copy(searchTerm = action.searchTerm, roomId = action.roomId, isNextBatch = false)
            }

            startSearching()
        }
    }

    private fun handleScrolledToTop() {
        setState {
            copy(isNextBatch = true)
        }
        startSearching(true)
    }

    private fun handleRetry() {
        startSearching()
    }

    private fun startSearching(scrolledToTop: Boolean = false) = withState { state ->
        if (state.roomId == null || state.searchTerm == null) return@withState

        // There is no batch to retrieve
        if (scrolledToTop && state.searchResult?.nextBatch == null) return@withState

        _viewEvents.post(SearchViewEvents.Loading())

        session
                .getRoom(state.roomId)
                ?.search(
                        searchTerm = state.searchTerm,
                        nextBatch = state.searchResult?.nextBatch,
                        orderByRecent = true,
                        beforeLimit = 0,
                        afterLimit = 0,
                        includeProfile = true,
                        limit = 20,
                        callback = object : MatrixCallback<SearchResult> {
                            override fun onFailure(failure: Throwable) {
                                onSearchFailure(failure)
                            }

                            override fun onSuccess(data: SearchResult) {
                                onSearchResultSuccess(data)
                            }
                        }
                )
    }

    private fun onSearchFailure(failure: Throwable) {
        setState {
            copy(searchResult = null)
        }
        _viewEvents.post(SearchViewEvents.Failure(failure))
    }

    private fun onSearchResultSuccess(searchResult: SearchResult) = withState { state ->
        val accumulatedResult = SearchResult(
                nextBatch = searchResult.nextBatch,
                results = searchResult.results,
                highlights = searchResult.highlights
        )

        // Accumulate results if it is the next batch
        if (state.isNextBatch) {
            if (state.searchResult != null) {
                accumulatedResult.results = accumulatedResult.results?.plus(state.searchResult.results!!)
            }
            if (state.searchResult?.highlights != null) {
                accumulatedResult.highlights = accumulatedResult.highlights?.plus(state.searchResult.highlights!!)
            }
        }

        setState {
            copy(searchResult = accumulatedResult, lastBatch = searchResult)
        }
    }
}
