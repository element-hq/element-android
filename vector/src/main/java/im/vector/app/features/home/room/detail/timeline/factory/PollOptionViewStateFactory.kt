/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import javax.inject.Inject

class PollOptionViewStateFactory @Inject constructor() {

    fun createPollEndedOptions(pollCreationInfo: PollCreationInfo?, pollResponseData: PollResponseData?): List<PollOptionViewState.PollEnded> {
        val winnerVoteCount = pollResponseData?.winnerVoteCount
        return pollCreationInfo?.answers?.map { answer ->
            val voteSummary = pollResponseData?.getVoteSummaryOfAnOption(answer.id ?: "")
            PollOptionViewState.PollEnded(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    voteCount = voteSummary?.total ?: 0,
                    votePercentage = voteSummary?.percentage ?: 0.0,
                    isWinner = winnerVoteCount != 0 && voteSummary?.total == winnerVoteCount
            )
        } ?: emptyList()
    }

    fun createPollSendingOptions(pollCreationInfo: PollCreationInfo?): List<PollOptionViewState.PollSending> {
        return pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollSending(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
            )
        } ?: emptyList()
    }

    fun createPollUndisclosedOptions(pollCreationInfo: PollCreationInfo?, pollResponseData: PollResponseData?): List<PollOptionViewState.PollUndisclosed> {
        return pollCreationInfo?.answers?.map { answer ->
            val isMyVote = pollResponseData?.myVote == answer.id
            PollOptionViewState.PollUndisclosed(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    isSelected = isMyVote
            )
        } ?: emptyList()
    }

    fun createPollVotedOptions(pollCreationInfo: PollCreationInfo?, pollResponseData: PollResponseData?): List<PollOptionViewState.PollVoted> {
        return pollCreationInfo?.answers?.map { answer ->
            val isMyVote = pollResponseData?.myVote == answer.id
            val voteSummary = pollResponseData?.getVoteSummaryOfAnOption(answer.id ?: "")
            PollOptionViewState.PollVoted(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    voteCount = voteSummary?.total ?: 0,
                    votePercentage = voteSummary?.percentage ?: 0.0,
                    isSelected = isMyVote
            )
        } ?: emptyList()
    }

    fun createPollReadyOptions(pollCreationInfo: PollCreationInfo?): List<PollOptionViewState.PollReady> {
        return pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollReady(
                    optionId = answer.id ?: "",
                    optionAnswer = answer.getBestAnswer() ?: ""
            )
        } ?: emptyList()
    }
}
