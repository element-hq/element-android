/*
 * Copyright (c) 2023 New Vector Ltd
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
