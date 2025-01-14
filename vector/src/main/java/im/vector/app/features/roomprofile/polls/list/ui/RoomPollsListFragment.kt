/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentRoomPollsListBinding
import im.vector.app.features.roomprofile.polls.RoomPollsAction
import im.vector.app.features.roomprofile.polls.RoomPollsLoadingError
import im.vector.app.features.roomprofile.polls.RoomPollsType
import im.vector.app.features.roomprofile.polls.RoomPollsViewEvent
import im.vector.app.features.roomprofile.polls.RoomPollsViewModel
import im.vector.app.features.roomprofile.polls.RoomPollsViewState
import javax.inject.Inject

abstract class RoomPollsListFragment :
        VectorBaseFragment<FragmentRoomPollsListBinding>(),
        RoomPollsController.Listener {

    @Inject
    lateinit var roomPollsController: RoomPollsController

    @Inject
    lateinit var stringProvider: StringProvider

    @Inject
    lateinit var viewNavigator: RoomPollsListNavigator

    private val viewModel: RoomPollsViewModel by parentFragmentViewModel(RoomPollsViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPollsListBinding {
        return FragmentRoomPollsListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        setupList()
        setupLoadMoreButton()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents { viewEvent ->
            when (viewEvent) {
                RoomPollsViewEvent.LoadingError -> showErrorInSnackbar(RoomPollsLoadingError())
            }
        }
    }

    abstract fun getEmptyListTitle(canLoadMore: Boolean, nbLoadedDays: Int): String

    abstract fun getRoomPollsType(): RoomPollsType

    private fun setupList() = withState(viewModel) { viewState ->
        roomPollsController.listener = this
        views.roomPollsList.configureWith(roomPollsController)
        views.roomPollsEmptyTitle.text = getEmptyListTitle(
                canLoadMore = viewState.canLoadMore,
                nbLoadedDays = viewState.nbSyncedDays,
        )
    }

    private fun setupLoadMoreButton() {
        views.roomPollsLoadMoreWhenEmpty.onClick {
            onLoadMoreClicked()
        }
    }

    override fun onDestroyView() {
        cleanUpList()
        super.onDestroyView()
    }

    private fun cleanUpList() {
        views.roomPollsList.cleanup()
        roomPollsController.listener = null
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        val filteredPolls = when (getRoomPollsType()) {
            RoomPollsType.ACTIVE -> viewState.polls.filterIsInstance(PollSummary.ActivePoll::class.java)
            RoomPollsType.ENDED -> viewState.polls.filterIsInstance(PollSummary.EndedPoll::class.java)
        }
        val updatedViewState = viewState.copy(polls = filteredPolls)
        renderList(updatedViewState)
        renderSyncingView(updatedViewState)
    }

    private fun renderSyncingView(viewState: RoomPollsViewState) {
        views.roomPollsSyncingTitle.isVisible = viewState.isSyncing
        views.roomPollsSyncingProgress.isVisible = viewState.isSyncing
    }

    private fun renderList(viewState: RoomPollsViewState) {
        roomPollsController.setData(viewState)
        views.roomPollsEmptyTitle.text = getEmptyListTitle(
                canLoadMore = viewState.canLoadMore,
                nbLoadedDays = viewState.nbSyncedDays,
        )
        views.roomPollsEmptyTitle.isVisible = !viewState.isSyncing && viewState.hasNoPolls()
        views.roomPollsLoadMoreWhenEmpty.isVisible = viewState.hasNoPollsAndCanLoadMore()
        views.roomPollsLoadMoreWhenEmpty.isEnabled = !viewState.isLoadingMore
        views.roomPollsLoadMoreWhenEmptyProgress.isVisible = viewState.hasNoPollsAndCanLoadMore() && viewState.isLoadingMore
    }

    override fun onPollClicked(pollId: String) = withState(viewModel) {
        viewNavigator.goToPollDetails(
                context = requireContext(),
                pollId = pollId,
                roomId = it.roomId,
                isEnded = getRoomPollsType() == RoomPollsType.ENDED,
        )
    }

    override fun onLoadMoreClicked() {
        viewModel.handle(RoomPollsAction.LoadMorePolls)
    }
}
