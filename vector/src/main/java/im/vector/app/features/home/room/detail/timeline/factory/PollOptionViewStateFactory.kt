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
