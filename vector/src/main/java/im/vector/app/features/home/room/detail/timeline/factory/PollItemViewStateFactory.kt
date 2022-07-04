/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.poll.PollViewState
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import javax.inject.Inject

class PollItemViewStateFactory @Inject constructor(
        private val stringProvider: StringProvider,
) {

    fun create(
            pollContent: MessagePollContent,
            informationData: MessageInformationData,
    ): PollViewState {
        val pollCreationInfo = pollContent.getBestPollCreationInfo()

        val question = pollCreationInfo?.question?.getBestQuestion().orEmpty()

        val pollResponseSummary = informationData.pollResponseAggregatedSummary
        val winnerVoteCount = pollResponseSummary?.winnerVoteCount
        val totalVotes = pollResponseSummary?.totalVotes ?: 0

        return when {
            !informationData.sendState.isSent() -> {
                createSendingPollViewState(question, pollCreationInfo)
            }
            informationData.pollResponseAggregatedSummary?.isClosed.orFalse() -> {
                createEndedPollViewState(question, pollCreationInfo, pollResponseSummary, totalVotes, winnerVoteCount)
            }
            pollContent.getBestPollCreationInfo()?.isUndisclosed().orFalse() -> {
                createUndisclosedPollViewState(question, pollCreationInfo, pollResponseSummary)
            }
            informationData.pollResponseAggregatedSummary?.myVote?.isNotEmpty().orFalse() -> {
                createVotedPollViewState(question, pollCreationInfo, pollResponseSummary, totalVotes)
            }
            else -> {
                createReadyPollViewState(question, pollCreationInfo, totalVotes)
            }
        }
    }

    private fun createSendingPollViewState(question: String, pollCreationInfo: PollCreationInfo?): PollViewState {
        return PollViewState(
                question = question,
                votesStatus = stringProvider.getString(R.string.poll_no_votes_cast),
                canVote = false,
                optionViewStates = pollCreationInfo?.answers?.map { answer ->
                    PollOptionViewState.PollSending(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: ""
                    )
                },
        )
    }

    private fun createEndedPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            pollResponseSummary: PollResponseData?,
            totalVotes: Int,
            winnerVoteCount: Int?,
    ): PollViewState {
        return PollViewState(
                question = question,
                votesStatus = stringProvider.getQuantityString(R.plurals.poll_total_vote_count_after_ended, totalVotes, totalVotes),
                canVote = false,
                optionViewStates = pollCreationInfo?.answers?.map { answer ->
                    val voteSummary = pollResponseSummary?.getVoteSummaryOfAnOption(answer.id ?: "")
                    PollOptionViewState.PollEnded(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            voteCount = voteSummary?.total ?: 0,
                            votePercentage = voteSummary?.percentage ?: 0.0,
                            isWinner = winnerVoteCount != 0 && voteSummary?.total == winnerVoteCount
                    )
                },
        )
    }

    private fun createUndisclosedPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            pollResponseSummary: PollResponseData?
    ): PollViewState {
        return PollViewState(
                question = question,
                votesStatus = stringProvider.getString(R.string.poll_undisclosed_not_ended),
                canVote = true,
                optionViewStates = pollCreationInfo?.answers?.map { answer ->
                    val isMyVote = pollResponseSummary?.myVote == answer.id
                    PollOptionViewState.PollUndisclosed(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            isSelected = isMyVote
                    )
                },
        )
    }

    private fun createVotedPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            pollResponseSummary: PollResponseData?,
            totalVotes: Int
    ): PollViewState {
        return PollViewState(
                question = question,
                votesStatus = stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, totalVotes, totalVotes),
                canVote = true,
                optionViewStates = pollCreationInfo?.answers?.map { answer ->
                    val isMyVote = pollResponseSummary?.myVote == answer.id
                    val voteSummary = pollResponseSummary?.getVoteSummaryOfAnOption(answer.id ?: "")
                    PollOptionViewState.PollVoted(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            voteCount = voteSummary?.total ?: 0,
                            votePercentage = voteSummary?.percentage ?: 0.0,
                            isSelected = isMyVote
                    )
                },
        )
    }

    private fun createReadyPollViewState(question: String, pollCreationInfo: PollCreationInfo?, totalVotes: Int): PollViewState {
        val totalVotesText = if (totalVotes == 0) {
            stringProvider.getString(R.string.poll_no_votes_cast)
        } else {
            stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_not_voted, totalVotes, totalVotes)
        }
        return PollViewState(
                question = question,
                votesStatus = totalVotesText,
                canVote = true,
                optionViewStates = pollCreationInfo?.answers?.map { answer ->
                    PollOptionViewState.PollReady(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: ""
                    )
                },
        )
    }
}
