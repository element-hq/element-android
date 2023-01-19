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

package im.vector.app.features.roomprofile.polls.list.ui

import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

// TODO add unit tests
class PollSummaryMapper @Inject constructor(
        private val pollResponseDataFactory: PollResponseDataFactory,
) {

    fun map(timelineEvent: TimelineEvent): PollSummary {
        val content = timelineEvent.getVectorLastMessageContent()
        val pollResponseData = pollResponseDataFactory.create(timelineEvent)
        val eventId = timelineEvent.root.eventId.orEmpty()
        val creationTimestamp = timelineEvent.root.originServerTs ?: 0
        if (eventId.isNotEmpty() && creationTimestamp > 0 && content is MessagePollContent && pollResponseData != null) {
            return convertToPollSummary(
                    eventId = eventId,
                    creationTimestamp = creationTimestamp,
                    messagePollContent = content,
                    pollResponseData = pollResponseData
            )
        } else {
            throw IllegalStateException("expected MessagePollContent")
        }
    }

    private fun convertToPollSummary(
            eventId: String,
            creationTimestamp: Long,
            messagePollContent: MessagePollContent,
            pollResponseData: PollResponseData
    ): PollSummary {
        val pollCreationInfo = messagePollContent.getBestPollCreationInfo()
        val pollTitle = pollCreationInfo?.question?.getBestQuestion().orEmpty()
        return if (pollResponseData.isClosed) {
            val winnerVoteCount = pollResponseData.winnerVoteCount
            PollSummary.EndedPoll(
                    id = eventId,
                    creationTimestamp = creationTimestamp,
                    title = pollTitle,
                    totalVotes = pollResponseData.totalVotes,
                    // TODO mutualise this with PollItemViewStateFactory
                    winnerOptions = pollCreationInfo?.answers?.map { answer ->
                        val voteSummary = pollResponseData.getVoteSummaryOfAnOption(answer.id ?: "")
                        PollOptionViewState.PollEnded(
                                optionId = answer.id ?: "",
                                optionAnswer = answer.getBestAnswer() ?: "",
                                voteCount = voteSummary?.total ?: 0,
                                votePercentage = voteSummary?.percentage ?: 0.0,
                                isWinner = winnerVoteCount != 0 && voteSummary?.total == winnerVoteCount
                        )
                    } ?: emptyList()
            )
        } else {
            PollSummary.ActivePoll(
                    id = eventId,
                    creationTimestamp = creationTimestamp,
                    title = pollTitle,
            )
        }
    }
}
