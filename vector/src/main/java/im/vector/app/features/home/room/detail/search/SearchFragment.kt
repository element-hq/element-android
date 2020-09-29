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

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_search.*
import org.matrix.android.sdk.api.session.events.model.Event
import javax.inject.Inject

@Parcelize
data class SearchArgs(
        val roomId: String
) : Parcelable

class SearchFragment @Inject constructor(
        val viewModelFactory: SearchViewModel.Factory,
        val controller: SearchResultController,
        val stringProvider: StringProvider
) : VectorBaseFragment(), StateView.EventCallback, SearchResultController.Listener {

    private val fragmentArgs: SearchArgs by args()
    private val searchViewModel: SearchViewModel by fragmentViewModel()

    private var pendingScrollToPosition: Int? = null

    override fun getLayoutResId() = R.layout.fragment_search

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stateView.contentView = searchResultRecycler
        stateView.eventCallback = this

        configureRecyclerView()

        searchViewModel.observeViewEvents {
            when (it) {
                is SearchViewEvents.Failure -> {
                    stateView.state = StateView.State.Error(errorFormatter.toHumanReadable(it.throwable))
                }
                is SearchViewEvents.Loading -> {
                    stateView.state = StateView.State.Loading
                }
            }.exhaustive
        }
    }

    private fun configureRecyclerView() {
        searchResultRecycler.trackItemsVisibilityChange()
        searchResultRecycler.configureWith(controller, showDivider = false)
        controller.listener = this

        controller.addModelBuildListener {
            pendingScrollToPosition?.let {
                searchResultRecycler.scrollToPosition(it)
            }
        }

        searchResultRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // Load next batch when scrolled to the top
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && (searchResultRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0) {
                    searchViewModel.handle(SearchAction.ScrolledToTop)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        searchResultRecycler?.cleanup()
        controller.listener = null
    }

    override fun invalidate() = withState(searchViewModel) { state ->
        if (state.searchResult?.results?.isNotEmpty() == true) {
            stateView.state = StateView.State.Content
            controller.setData(state)

            val lastBatchSize = state.lastBatch?.results?.size ?: 0
            val scrollPosition = if (lastBatchSize > 0) lastBatchSize - 1 else 0
            pendingScrollToPosition = scrollPosition
        } else {
            stateView.state = StateView.State.Empty(
                    title = stringProvider.getString(R.string.search_no_results),
                    image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search_no_results)
            )
        }
    }

    fun search(query: String) {
        view?.hideKeyboard()
        searchViewModel.handle(SearchAction.SearchWith(query))
    }

    override fun onRetryClicked() {
        searchViewModel.handle(SearchAction.Retry)
    }

    override fun onItemClicked(event: Event) {
        event.roomId ?: return

        navigator.openRoom(requireContext(), event.roomId!!, event.eventId)
    }
}
