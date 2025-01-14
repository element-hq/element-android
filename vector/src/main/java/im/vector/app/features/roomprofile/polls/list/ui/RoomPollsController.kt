/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.roomprofile.polls.RoomPollsViewState
import im.vector.lib.strings.CommonPlurals
import java.util.UUID
import javax.inject.Inject

class RoomPollsController @Inject constructor(
        val dateFormatter: VectorDateFormatter,
        val stringProvider: StringProvider,
) : TypedEpoxyController<RoomPollsViewState>() {

    interface Listener {
        fun onPollClicked(pollId: String)
        fun onLoadMoreClicked()
    }

    var listener: Listener? = null

    override fun buildModels(viewState: RoomPollsViewState?) {
        val polls = viewState?.polls
        if (polls.isNullOrEmpty() || viewState.isSyncing) {
            return
        }

        for (poll in polls) {
            when (poll) {
                is PollSummary.ActivePoll -> buildActivePollItem(poll)
                is PollSummary.EndedPoll -> buildEndedPollItem(poll)
            }
        }

        if (viewState.canLoadMore) {
            buildLoadMoreItem(viewState.isLoadingMore)
        }
    }

    private fun buildActivePollItem(poll: PollSummary.ActivePoll) {
        val host = this
        roomPollItem {
            id(poll.id)
            formattedDate(host.dateFormatter.format(poll.creationTimestamp, DateFormatKind.TIMELINE_DAY_DIVIDER))
            title(poll.title)
            clickListener {
                host.listener?.onPollClicked(poll.id)
            }
        }
    }

    private fun buildEndedPollItem(poll: PollSummary.EndedPoll) {
        val host = this
        roomPollItem {
            id(poll.id)
            formattedDate(host.dateFormatter.format(poll.creationTimestamp, DateFormatKind.TIMELINE_DAY_DIVIDER))
            title(poll.title)
            winnerOptions(poll.winnerOptions)
            totalVotesStatus(host.stringProvider.getQuantityString(CommonPlurals.poll_total_vote_count_after_ended, poll.totalVotes, poll.totalVotes))
            clickListener {
                host.listener?.onPollClicked(poll.id)
            }
        }
    }

    private fun buildLoadMoreItem(isLoadingMore: Boolean) {
        val host = this
        roomPollLoadMoreItem {
            id(UUID.randomUUID().toString())
            loadingMore(isLoadingMore)
            clickListener {
                host.listener?.onLoadMoreClicked()
            }
        }
    }
}
