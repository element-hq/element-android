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
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.PollVoteSummaryData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryData
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.poll.PollViewState
import im.vector.app.test.fakes.FakeStringProvider
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.send.SendState

private val A_MESSAGE_INFORMATION_DATA = MessageInformationData(
        eventId = "eventId",
        senderId = "senderId",
        ageLocalTS = 0,
        avatarUrl = "",
        sendState = SendState.SENT,
        messageLayout = TimelineMessageLayout.Default(showAvatar = true, showDisplayName = true, showTimestamp = true),
        reactionsSummary = ReactionsSummaryData(),
        sentByMe = true,
)

private val A_POLL_RESPONSE_DATA = PollResponseData(
        myVote = null,
        votes = emptyMap(),
)

private val A_POLL_OPTION_IDS = listOf("5ef5f7b0-c9a1-49cf-a0b3-374729a43e76", "ec1a4db0-46d8-4d7a-9bb6-d80724715938", "3677ca8e-061b-40ab-bffe-b22e4e88fcad")

private val A_POLL_CONTENT = MessagePollContent(
        unstablePollCreationInfo = PollCreationInfo(
                question = PollQuestion(
                        unstableQuestion = "What is your favourite coffee?"
                ),
                kind = PollType.UNDISCLOSED_UNSTABLE,
                maxSelections = 1,
                answers = listOf(
                        PollAnswer(
                                id = A_POLL_OPTION_IDS[0],
                                unstableAnswer = "Double Espresso"
                        ),
                        PollAnswer(
                                id = A_POLL_OPTION_IDS[1],
                                unstableAnswer = "Macchiato"
                        ),
                        PollAnswer(
                                id = A_POLL_OPTION_IDS[2],
                                unstableAnswer = "Iced Coffee"
                        ),
                )
        )
)

class PollItemViewStateFactoryTest {

    @Test
    fun `given a sending poll state then poll is not votable and option states are PollSending`() {
        val stringProvider = FakeStringProvider()
        val pollItemViewStateFactory = PollItemViewStateFactory(stringProvider.instance)

        val sendingPollInformationData = A_MESSAGE_INFORMATION_DATA.copy(sendState = SendState.SENDING)
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                informationData = sendingPollInformationData,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = stringProvider.instance.getString(R.string.poll_no_votes_cast),
                canVote = false,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.map { answer ->
                    PollOptionViewState.PollSending(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: ""
                    )
                },
        )
    }

    @Test
    fun `given a sent poll state when poll is closed then poll is not votable and option states are Ended`() {
        val stringProvider = FakeStringProvider()
        val pollItemViewStateFactory = PollItemViewStateFactory(stringProvider.instance)

        val closedPollSummary = A_POLL_RESPONSE_DATA.copy(isClosed = true)
        val closedPollInformationData = A_MESSAGE_INFORMATION_DATA.copy(pollResponseAggregatedSummary = closedPollSummary)

        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                informationData = closedPollInformationData,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = stringProvider.instance.getQuantityString(R.plurals.poll_total_vote_count_after_ended, 0, 0),
                canVote = false,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.map { answer ->
                    PollOptionViewState.PollEnded(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            voteCount = 0,
                            votePercentage = 0.0,
                            isWinner = false
                    )
                },
        )
    }

    @Test
    fun `given a sent poll when undisclosed poll type is selected then poll is votable and option states are PollUndisclosed`() {
        val stringProvider = FakeStringProvider()
        val pollItemViewStateFactory = PollItemViewStateFactory(stringProvider.instance)

        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                informationData = A_MESSAGE_INFORMATION_DATA,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = stringProvider.instance.getString(R.string.poll_undisclosed_not_ended),
                canVote = true,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.map { answer ->
                    PollOptionViewState.PollUndisclosed(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            isSelected = false
                    )
                },
        )
    }

    @Test
    fun `given a sent poll when my vote exists then poll is still votable and options states are PollVoted`() {
        val stringProvider = FakeStringProvider()
        val pollItemViewStateFactory = PollItemViewStateFactory(stringProvider.instance)

        val votedPollData = A_POLL_RESPONSE_DATA.copy(
                totalVotes = 1,
                myVote = A_POLL_OPTION_IDS[0],
                votes = mapOf(A_POLL_OPTION_IDS[0] to PollVoteSummaryData(total = 1, percentage = 1.0))
        )
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                ),
        )
        val votedInformationData = A_MESSAGE_INFORMATION_DATA.copy(pollResponseAggregatedSummary = votedPollData)

        val pollViewState = pollItemViewStateFactory.create(
                pollContent = disclosedPollContent,
                informationData = votedInformationData,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = stringProvider.instance.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, 1, 1),
                canVote = true,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.mapIndexed { index, answer ->
                    PollOptionViewState.PollVoted(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: "",
                            voteCount = if (index == 0) 1 else 0,
                            votePercentage = if (index == 0) 1.0 else 0.0,
                            isSelected = index == 0
                    )
                },
        )
    }

    @Test
    fun `given a sent poll when poll type is disclosed then poll is votable and option view states are PollReady`() {
        val stringProvider = FakeStringProvider()
        val pollItemViewStateFactory = PollItemViewStateFactory(stringProvider.instance)

        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                )
        )
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = disclosedPollContent,
                informationData = A_MESSAGE_INFORMATION_DATA,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = stringProvider.instance.getString(R.string.poll_no_votes_cast),
                canVote = true,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.map { answer ->
                    PollOptionViewState.PollReady(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: ""
                    )
                },
        )
    }
}
