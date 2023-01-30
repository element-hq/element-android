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
import im.vector.app.features.home.room.detail.timeline.item.PollVoteSummaryData
import im.vector.app.test.fixtures.PollFixture.A_POLL_CONTENT
import im.vector.app.test.fixtures.PollFixture.A_POLL_OPTION_IDS
import im.vector.app.test.fixtures.PollFixture.A_POLL_RESPONSE_DATA
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.PollType

internal class PollOptionViewStateFactoryTest {

    private val pollOptionViewStateFactory = PollOptionViewStateFactory()

    @Test
    fun `given poll data when creating ended poll options then correct options are returned`() {
        // Given
        val winnerVotesCount = 0
        val pollResponseData = A_POLL_RESPONSE_DATA.copy(
                isClosed = true,
                winnerVoteCount = winnerVotesCount,
        )
        val pollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()
        val expectedOptions = pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollEnded(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    voteCount = 0,
                    votePercentage = 0.0,
                    isWinner = false,
            )
        }

        // When
        val result = pollOptionViewStateFactory.createPollEndedOptions(
                pollCreationInfo = pollCreationInfo,
                pollResponseData = pollResponseData,
        )

        // Then
        result shouldBeEqualTo expectedOptions
    }

    @Test
    fun `given poll data when creating sending poll options then correct options are returned`() {
        // Given
        val pollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()
        val expectedOptions = pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollSending(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
            )
        }

        // When
        val result = pollOptionViewStateFactory.createPollSendingOptions(
                pollCreationInfo = pollCreationInfo,
        )

        // Then
        result shouldBeEqualTo expectedOptions
    }

    @Test
    fun `given poll data when creating undisclosed poll options then correct options are returned`() {
        // Given
        val pollResponseData = A_POLL_RESPONSE_DATA
        val pollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()
        val expectedOptions = pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollUndisclosed(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    isSelected = false,
            )
        }

        // When
        val result = pollOptionViewStateFactory.createPollUndisclosedOptions(
                pollCreationInfo = pollCreationInfo,
                pollResponseData = pollResponseData,
        )

        // Then
        result shouldBeEqualTo expectedOptions
    }

    @Test
    fun `given poll data when creating voted poll options then correct options are returned`() {
        // Given
        val pollResponseData = A_POLL_RESPONSE_DATA.copy(
                totalVotes = 1,
                myVote = A_POLL_OPTION_IDS[0],
                votes = mapOf(A_POLL_OPTION_IDS[0] to PollVoteSummaryData(total = 1, percentage = 1.0)),
        )
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE,
                ),
        )
        val pollCreationInfo = disclosedPollContent.getBestPollCreationInfo()
        val expectedOptions = pollCreationInfo?.answers?.mapIndexed { index, answer ->
            PollOptionViewState.PollVoted(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
                    voteCount = if (index == 0) 1 else 0,
                    votePercentage = if (index == 0) 1.0 else 0.0,
                    isSelected = index == 0,
            )
        }

        // When
        val result = pollOptionViewStateFactory.createPollVotedOptions(
                pollCreationInfo = pollCreationInfo,
                pollResponseData = pollResponseData,
        )

        // Then
        result shouldBeEqualTo expectedOptions
    }

    @Test
    fun `given poll data when creating ready poll options then correct options are returned`() {
        // Given
        val pollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()
        val expectedOptions = pollCreationInfo?.answers?.map { answer ->
            PollOptionViewState.PollReady(
                    optionId = answer.id.orEmpty(),
                    optionAnswer = answer.getBestAnswer().orEmpty(),
            )
        }

        // When
        val result = pollOptionViewStateFactory.createPollReadyOptions(
                pollCreationInfo = pollCreationInfo,
        )

        // Then
        result shouldBeEqualTo expectedOptions
    }
}
