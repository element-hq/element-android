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

import androidx.annotation.VisibleForTesting
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.factory.MessageItemFactoryHelper.annotateWithEdited
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollItem_
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.poll.PollState
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollType
import javax.inject.Inject

class PollItemFactory @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter,
) {

    fun create(
            pollContent: MessagePollContent,
            informationData: MessageInformationData,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            attributes: AbsMessageItem.Attributes,
    ): VectorEpoxyModel<*>? {
        val pollResponseSummary = informationData.pollResponseAggregatedSummary
        val pollState = createPollState(informationData, pollResponseSummary, pollContent)
        val pollCreationInfo = pollContent.getBestPollCreationInfo()
        val questionText = pollCreationInfo?.question?.getBestQuestion().orEmpty()
        val question = createPollQuestion(informationData, questionText, callback)
        val optionViewStates = pollCreationInfo?.answers?.mapToOptions(pollState, informationData)
        val totalVotesText = createTotalVotesText(pollState, pollResponseSummary)

        return PollItem_()
                .attributes(attributes)
                .eventId(informationData.eventId)
                .pollQuestion(question)
                .canVote(pollState.isVotable())
                .totalVotesText(totalVotesText)
                .optionViewStates(optionViewStates)
                .edited(informationData.hasBeenEdited)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .callback(callback)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createPollState(
            informationData: MessageInformationData,
            pollResponseSummary: PollResponseData?,
            pollContent: MessagePollContent,
    ): PollState = when {
        !informationData.sendState.isSent() -> PollState.Sending
        pollResponseSummary?.isClosed.orFalse() -> PollState.Ended
        pollContent.getBestPollCreationInfo()?.isUndisclosed().orFalse() -> PollState.Undisclosed
        pollResponseSummary?.myVote?.isNotEmpty().orFalse() -> PollState.Voted(pollResponseSummary?.totalVotes ?: 0)
        else -> PollState.Ready
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun List<PollAnswer>.mapToOptions(
            pollState: PollState,
            informationData: MessageInformationData,
    ) = map { answer ->
        val pollResponseSummary = informationData.pollResponseAggregatedSummary
        val winnerVoteCount = pollResponseSummary?.winnerVoteCount
        val optionId = answer.id ?: ""
        val optionAnswer = answer.getBestAnswer() ?: ""
        val voteSummary = pollResponseSummary?.votes?.get(answer.id)
        val voteCount = voteSummary?.total ?: 0
        val votePercentage = voteSummary?.percentage ?: 0.0
        val isMyVote = pollResponseSummary?.myVote == answer.id
        val isWinner = winnerVoteCount != 0 && voteCount == winnerVoteCount

        when (pollState) {
            PollState.Sending -> PollOptionViewState.PollSending(optionId, optionAnswer)
            PollState.Ready -> PollOptionViewState.PollReady(optionId, optionAnswer)
            is PollState.Voted -> PollOptionViewState.PollVoted(optionId, optionAnswer, voteCount, votePercentage, isMyVote)
            PollState.Undisclosed -> PollOptionViewState.PollUndisclosed(optionId, optionAnswer, isMyVote)
            PollState.Ended -> PollOptionViewState.PollEnded(optionId, optionAnswer, voteCount, votePercentage, isWinner)
        }
    }

    private fun createPollQuestion(
            informationData: MessageInformationData,
            question: String,
            callback: TimelineEventController.Callback?,
    ) = if (informationData.hasBeenEdited) {
        annotateWithEdited(stringProvider, colorProvider, dimensionConverter, question, callback, informationData)
    } else {
        question
    }.toEpoxyCharSequence()

    private fun createTotalVotesText(
            pollState: PollState,
            pollResponseSummary: PollResponseData?,
    ): String {
        val votes = pollResponseSummary?.totalVotes ?: 0
        return when {
            pollState is PollState.Ended -> stringProvider.getQuantityString(R.plurals.poll_total_vote_count_after_ended, votes, votes)
            pollState is PollState.Undisclosed -> ""
            pollState is PollState.Voted -> stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, votes, votes)
            votes == 0 -> stringProvider.getString(R.string.poll_no_votes_cast)
            else -> stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_not_voted, votes, votes)
        }
    }
}
