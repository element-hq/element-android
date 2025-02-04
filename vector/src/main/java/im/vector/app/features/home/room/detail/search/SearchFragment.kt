/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.search

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSearchBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import javax.inject.Inject

@Parcelize
data class SearchArgs(
        val roomId: String,
        val roomDisplayName: String?,
        val roomAvatarUrl: String?
) : Parcelable

@AndroidEntryPoint
class SearchFragment :
        VectorBaseFragment<FragmentSearchBinding>(),
        StateView.EventCallback,
        SearchResultController.Listener {

    @Inject lateinit var controller: SearchResultController
    private val fragmentArgs: SearchArgs by args()
    private val searchViewModel: SearchViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.stateView.contentView = views.searchResultRecycler
        views.stateView.eventCallback = this

        configureRecyclerView()
    }

    private fun configureRecyclerView() {
        views.searchResultRecycler.trackItemsVisibilityChange()
        views.searchResultRecycler.configureWith(controller)
        (views.searchResultRecycler.layoutManager as? LinearLayoutManager)?.stackFromEnd = true
        controller.listener = this
    }

    override fun onDestroyView() {
        views.searchResultRecycler.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(searchViewModel) { state ->
        if (state.searchResult.isNullOrEmpty()) {
            when (state.asyncSearchRequest) {
                is Loading -> {
                    views.stateView.state = StateView.State.Loading
                }
                is Fail -> {
                    views.stateView.state = StateView.State.Error(errorFormatter.toHumanReadable(state.asyncSearchRequest.error))
                }
                is Success -> {
                    views.stateView.state = StateView.State.Empty(
                            title = getString(CommonStrings.search_no_results),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search_no_results)
                    )
                }
                else -> Unit
            }
        } else {
            controller.setData(state)
            views.stateView.state = StateView.State.Content
        }
    }

    fun search(query: String) {
        view?.hideKeyboard()
        searchViewModel.handle(SearchAction.SearchWith(query))
    }

    override fun onRetryClicked() {
        searchViewModel.handle(SearchAction.Retry)
    }

    override fun onItemClicked(event: Event) =
            navigateToEvent(event)

    override fun onThreadSummaryClicked(event: Event) {
        navigateToEvent(event, true)
    }

    /**
     * Navigate and highlight the event. If this is a thread event,
     * user will be redirected to the appropriate thread room
     * @param event the event to navigate and highlight
     * @param forceNavigateToThread force navigate within the thread (ex. when user clicks on thread summary)
     */
    private fun navigateToEvent(event: Event, forceNavigateToThread: Boolean = false) {
        val roomId = event.roomId ?: return
        val rootThreadEventId = if (forceNavigateToThread) {
            event.eventId
        } else {
            event.getRootThreadEventId()
        }

        rootThreadEventId?.let {
            val threadTimelineArgs = ThreadTimelineArgs(
                    roomId = roomId,
                    displayName = fragmentArgs.roomDisplayName,
                    avatarUrl = fragmentArgs.roomAvatarUrl,
                    roomEncryptionTrustLevel = null,
                    rootThreadEventId = it
            )
            navigator.openThread(requireContext(), threadTimelineArgs, event.eventId)
        } ?: openRoom(roomId, event.eventId)
    }

    private fun openRoom(roomId: String, eventId: String?) {
        navigator.openRoom(
                context = requireContext(),
                roomId = roomId,
                eventId = eventId,
                trigger = ViewRoom.Trigger.MessageSearch
        )
    }

    override fun loadMore() {
        searchViewModel.handle(SearchAction.LoadMore)
    }
}
