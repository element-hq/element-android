/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import java.util.UUID
import javax.inject.Inject

class RoomPollDetailController @Inject constructor(
        val dateFormatter: VectorDateFormatter,
) : TypedEpoxyController<RoomPollDetailViewState>() {

    interface Callback {
        fun vote(pollEventId: String, optionId: String)
        fun goToTimelineEvent(eventId: String)
    }

    var callback: Callback? = null

    override fun buildModels(viewState: RoomPollDetailViewState?) {
        val pollDetail = viewState?.pollDetail ?: return
        val pollItemViewState = pollDetail.pollItemViewState
        val host = this

        roomPollDetailItem {
            id(viewState.pollId)
            eventId(viewState.pollId)
            formattedDate(host.dateFormatter.format(pollDetail.creationTimestamp, DateFormatKind.TIMELINE_DAY_DIVIDER))
            question(pollItemViewState.question)
            canVote(pollItemViewState.canVote)
            votesStatus(pollItemViewState.votesStatus)
            optionViewStates(pollItemViewState.optionViewStates.orEmpty())
            callback(host.callback)
        }

        buildGoToTimelineItem(targetEventId = pollDetail.endedPollEventId ?: viewState.pollId)
    }

    private fun buildGoToTimelineItem(targetEventId: String) {
        val host = this
        roomPollGoToTimelineItem {
            id(UUID.randomUUID().toString())
            clickListener {
                host.callback?.goToTimelineEvent(targetEventId)
            }
        }
    }
}
