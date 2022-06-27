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

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryData
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.poll.PollViewState
import im.vector.app.test.fakes.FakeStringProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
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

private val A_POLL_CONTENT = MessagePollContent(
        unstablePollCreationInfo = PollCreationInfo(
                question = PollQuestion(
                        unstableQuestion = "What is your favourite coffee?"
                ),
                kind = PollType.UNDISCLOSED_UNSTABLE,
                maxSelections = 1,
                answers = listOf(
                        PollAnswer(
                                id = "5ef5f7b0-c9a1-49cf-a0b3-374729a43e76",
                                unstableAnswer = "Double Espresso"
                        ),
                        PollAnswer(
                                id = "ec1a4db0-46d8-4d7a-9bb6-d80724715938",
                                unstableAnswer = "Macchiato"
                        ),
                        PollAnswer(
                                id = "3677ca8e-061b-40ab-bffe-b22e4e88fcad",
                                unstableAnswer = "Iced Coffee"
                        ),
                )
        )
)

class PollItemViewStateFactoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mvRxTestRule = MvRxTestRule(
            testDispatcher = testDispatcher // See https://github.com/airbnb/mavericks/issues/599
    )
    private lateinit var pollItemViewStateFactory: PollItemViewStateFactory

    private val stringProvider = FakeStringProvider()

    @Before
    fun setup() {
        // We are not going to test any UI related code
        pollItemViewStateFactory = PollItemViewStateFactory(
                stringProvider = stringProvider.instance,
        )
    }

    @Test
    fun `given a sending poll state then poll is not votable and option states are PollSending`() = runTest {
        val sendingPollInformationData = A_MESSAGE_INFORMATION_DATA.copy(sendState = SendState.SENDING)
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                informationData = sendingPollInformationData,
        )

        pollViewState shouldBeEqualTo PollViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                totalVotes = stringProvider.instance.getString(R.string.poll_no_votes_cast),
                canVote = false,
                optionViewStates = A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.map { answer ->
                    PollOptionViewState.PollSending(
                            optionId = answer.id ?: "",
                            optionAnswer = answer.getBestAnswer() ?: ""
                    )
                },
        )
    }

    /*
    @Test
    fun `given a sent poll state when poll is closed then PollState is Ended`() = runTest {
        val closedPollSummary = A_POLL_RESPONSE_DATA.copy(isClosed = true)

        pollItemViewStateFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = closedPollSummary,
                pollContent = A_POLL_CONTENT,
        ) shouldBe PollState.Ended
    }

    @Test
    fun `given a sent poll when undisclosed poll type is selected then PollState is Undisclosed`() = runTest {
        pollItemViewStateFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = A_POLL_RESPONSE_DATA,
                pollContent = A_POLL_CONTENT,
        ) shouldBe PollState.Undisclosed
    }

    @Test
    fun `given a sent poll when my vote exists then PollState is Voted`() = runTest {
        val votedPollData = A_POLL_RESPONSE_DATA.copy(
                totalVotes = 1,
                myVote = "5ef5f7b0-c9a1-49cf-a0b3-374729a43e76",
        )
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                )
        )

        pollItemViewStateFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = votedPollData,
                pollContent = disclosedPollContent,
        ) shouldBeEqualTo PollState.Voted(1)
    }

    @Test
    fun `given a sent poll when poll type is disclosed then PollState is Ready`() = runTest {
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                )
        )

        pollItemViewStateFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = A_POLL_RESPONSE_DATA,
                pollContent = disclosedPollContent,
        ) shouldBe PollState.Ready
    }

    @Test
    fun `given a sending poll then all option view states is PollSending`() = runTest {
        with(pollItemViewStateFactory) {
            A_POLL_CONTENT
                    .getBestPollCreationInfo()
                    ?.answers
                    ?.mapToOptions(PollState.Sending, A_MESSAGE_INFORMATION_DATA)
                    ?.forEachIndexed { index, pollOptionViewState ->
                        A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.get(index)?.let { option ->
                            pollOptionViewState shouldBeEqualTo PollOptionViewState.PollSending(option.id ?: "", option.getBestAnswer() ?: "")
                        }
                    }
        }
    }

    @Test
    fun `given a sent poll then all option view states is PollReady`() = runTest {
        with(pollItemViewStateFactory) {
            A_POLL_CONTENT
                    .getBestPollCreationInfo()
                    ?.answers
                    ?.mapToOptions(PollState.Sending, A_MESSAGE_INFORMATION_DATA)
                    ?.forEachIndexed { index, pollOptionViewState ->
                        A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.get(index)?.let { option ->
                            pollOptionViewState shouldBeEqualTo PollOptionViewState.PollSending(option.id ?: "", option.getBestAnswer() ?: "")
                        }
                    }
        }
    }

    @Test
    fun `given a sent poll when a vote is cast then all option view states is PollVoted`() = runTest {
        with(pollItemViewStateFactory) {
            A_POLL_CONTENT
                    .getBestPollCreationInfo()
                    ?.answers
                    ?.mapToOptions(PollState.Voted(1), A_MESSAGE_INFORMATION_DATA)
                    ?.forEachIndexed { index, pollOptionViewState ->
                        A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.get(index)?.let { option ->
                            val voteSummary = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.votes?.get(option.id)
                            pollOptionViewState shouldBeEqualTo PollOptionViewState.PollVoted(
                                    optionId = option.id ?: "",
                                    optionAnswer = option.getBestAnswer() ?: "",
                                    voteCount = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.totalVotes ?: 0,
                                    votePercentage = voteSummary?.percentage ?: 0.0,
                                    isSelected = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.myVote == option.id,
                            )
                        }
                    }
        }
    }

    @Test
    fun `given a sent poll when the poll is undisclosed then all option view states is PollUndisclosed`() = runTest {
        with(pollItemViewStateFactory) {
            A_POLL_CONTENT
                    .getBestPollCreationInfo()
                    ?.answers
                    ?.mapToOptions(PollState.Undisclosed, A_MESSAGE_INFORMATION_DATA)
                    ?.forEachIndexed { index, pollOptionViewState ->
                        A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.get(index)?.let { option ->
                            pollOptionViewState shouldBeEqualTo PollOptionViewState.PollUndisclosed(
                                    optionId = option.id ?: "",
                                    optionAnswer = option.getBestAnswer() ?: "",
                                    isSelected = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.myVote == option.id,
                            )
                        }
                    }
        }
    }

    @Test
    fun `given an ended poll then all option view states is Ended`() = runTest {
        with(pollItemViewStateFactory) {
            A_POLL_CONTENT
                    .getBestPollCreationInfo()
                    ?.answers
                    ?.mapToOptions(PollState.Ended, A_MESSAGE_INFORMATION_DATA)
                    ?.forEachIndexed { index, pollOptionViewState ->
                        A_POLL_CONTENT.getBestPollCreationInfo()?.answers?.get(index)?.let { option ->
                            val voteSummary = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.votes?.get(option.id)
                            val voteCount = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.totalVotes ?: 0
                            val winnerVoteCount = A_MESSAGE_INFORMATION_DATA.pollResponseAggregatedSummary?.winnerVoteCount ?: 0
                            pollOptionViewState shouldBeEqualTo PollOptionViewState.PollEnded(
                                    optionId = option.id ?: "",
                                    optionAnswer = option.getBestAnswer() ?: "",
                                    voteCount = voteCount,
                                    votePercentage = voteSummary?.percentage ?: 0.0,
                                    isWinner = winnerVoteCount != 0 && voteCount == winnerVoteCount,
                            )
                        }
                    }
        }
    }

    @Test
    fun `given a poll state when it is not Sending and not Ended then the poll is votable`() = runTest {
        val sendingPollState = PollState.Sending
        sendingPollState.isVotable() shouldBe false
        val readyPollState = PollState.Ready
        readyPollState.isVotable() shouldBe true
        val votedPollState = PollState.Voted(1)
        votedPollState.isVotable() shouldBe true
        val undisclosedPollState = PollState.Undisclosed
        undisclosedPollState.isVotable() shouldBe true
        var endedPollState = PollState.Ended
        endedPollState.isVotable() shouldBe false
    }
     */
}
