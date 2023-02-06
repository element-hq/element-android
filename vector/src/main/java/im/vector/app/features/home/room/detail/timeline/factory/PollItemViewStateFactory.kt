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
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.poll.PollViewState
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import javax.inject.Inject

class PollItemViewStateFactory @Inject constructor(
        private val stringProvider: StringProvider,
        private val pollOptionViewStateFactory: PollOptionViewStateFactory,
) {

    fun create(
            pollContent: MessagePollContent,
            informationData: MessageInformationData,
    ): PollViewState {
        val pollCreationInfo = pollContent.getBestPollCreationInfo()

        val question = pollCreationInfo?.question?.getBestQuestion().orEmpty()

        val pollResponseSummary = informationData.pollResponseAggregatedSummary
        val totalVotes = pollResponseSummary?.totalVotes ?: 0

        return when {
            !informationData.sendState.isSent() -> {
                createSendingPollViewState(question, pollCreationInfo)
            }
            informationData.pollResponseAggregatedSummary?.isClosed.orFalse() -> {
                createEndedPollViewState(question, pollCreationInfo, pollResponseSummary, totalVotes)
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
                optionViewStates = pollOptionViewStateFactory.createPollSendingOptions(pollCreationInfo),
        )
    }

    private fun createEndedPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            pollResponseSummary: PollResponseData?,
            totalVotes: Int,
    ): PollViewState {
        val totalVotesText = if (pollResponseSummary?.hasEncryptedRelatedEvents.orFalse()) {
            stringProvider.getString(R.string.unable_to_decrypt_some_events_in_poll)
        } else {
            stringProvider.getQuantityString(R.plurals.poll_total_vote_count_after_ended, totalVotes, totalVotes)
        }
        return PollViewState(
                question = question,
                votesStatus = totalVotesText,
                canVote = false,
                optionViewStates = pollOptionViewStateFactory.createPollEndedOptions(pollCreationInfo, pollResponseSummary),
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
                optionViewStates = pollOptionViewStateFactory.createPollUndisclosedOptions(pollCreationInfo, pollResponseSummary),
        )
    }

    private fun createVotedPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            pollResponseSummary: PollResponseData?,
            totalVotes: Int
    ): PollViewState {
        val totalVotesText = if (pollResponseSummary?.hasEncryptedRelatedEvents.orFalse()) {
            stringProvider.getString(R.string.unable_to_decrypt_some_events_in_poll)
        } else {
            stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, totalVotes, totalVotes)
        }
        return PollViewState(
                question = question,
                votesStatus = totalVotesText,
                canVote = true,
                optionViewStates = pollOptionViewStateFactory.createPollVotedOptions(pollCreationInfo, pollResponseSummary),
        )
    }

    private fun createReadyPollViewState(
            question: String,
            pollCreationInfo: PollCreationInfo?,
            totalVotes: Int
    ): PollViewState {
        val totalVotesText = if (totalVotes == 0) {
            stringProvider.getString(R.string.poll_no_votes_cast)
        } else {
            stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_not_voted, totalVotes, totalVotes)
        }
        return PollViewState(
                question = question,
                votesStatus = totalVotesText,
                canVote = true,
                optionViewStates = pollOptionViewStateFactory.createPollReadyOptions(pollCreationInfo),
        )
    }
}
