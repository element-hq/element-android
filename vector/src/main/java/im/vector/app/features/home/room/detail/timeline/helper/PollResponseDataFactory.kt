/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.PollVoteSummaryData
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.isPollEnd
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

class PollResponseDataFactory @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun create(event: TimelineEvent): PollResponseData? {
        val pollResponseSummary = getPollResponseSummary(event)
        return pollResponseSummary?.let {
            PollResponseData(
                    myVote = it.aggregatedContent?.myVote,
                    isClosed = it.closedTime != null,
                    votes = it.aggregatedContent?.votesSummary?.mapValues { votesSummary ->
                        PollVoteSummaryData(
                                total = votesSummary.value.total,
                                percentage = votesSummary.value.percentage
                        )
                    },
                    winnerVoteCount = it.aggregatedContent?.winnerVoteCount ?: 0,
                    totalVotes = it.aggregatedContent?.totalVotes ?: 0,
                    hasEncryptedRelatedEvents = it.encryptedRelatedEventIds.isNotEmpty(),
            )
        }
    }

    private fun getPollResponseSummary(event: TimelineEvent): PollResponseAggregatedSummary? {
        return if (event.root.isPollEnd()) {
            val pollStartEventId = event.root.getRelationContent()?.eventId
            if (pollStartEventId.isNullOrEmpty()) {
                Timber.e("### Cannot render poll end event because poll start event id is null")
                null
            } else {
                activeSessionHolder
                        .getSafeActiveSession()
                        ?.roomService()
                        ?.getRoom(event.roomId)
                        ?.getTimelineEvent(pollStartEventId)
                        ?.annotations
                        ?.pollResponseSummary
            }
        } else {
            event.annotations?.pollResponseSummary
        }
    }
}
