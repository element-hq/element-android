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
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryData
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.poll.PollState
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
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

class PollItemFactoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mvRxTestRule = MvRxTestRule(
            testDispatcher = testDispatcher // See https://github.com/airbnb/mavericks/issues/599
    )

    private lateinit var pollItemFactory: PollItemFactory

    @Before
    fun setup() {
        // We are not going to test any UI related code
        pollItemFactory = PollItemFactory(
                stringProvider = mockk(),
                avatarSizeProvider = mockk(),
                colorProvider = mockk(),
                dimensionConverter = mockk(),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a sending poll state then PollState is Sending`() = runTest {
        val sendingPollInformationData = A_MESSAGE_INFORMATION_DATA.copy(sendState = SendState.SENDING)
        pollItemFactory.createPollState(
                informationData = sendingPollInformationData,
                pollResponseSummary = A_POLL_RESPONSE_DATA,
                pollContent = A_POLL_CONTENT,
        ) shouldBe PollState.Sending
    }

    @Test
    fun `given a sent poll state when poll is closed then PollState is Ended`() = runTest {
        val closedPollSummary = A_POLL_RESPONSE_DATA.copy(isClosed = true)

        pollItemFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = closedPollSummary,
                pollContent = A_POLL_CONTENT,
        ) shouldBe PollState.Ended
    }

    @Test
    fun `given a sent poll when undisclosed poll type is selected then PollState is Undisclosed`() = runTest {
        pollItemFactory.createPollState(
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

        pollItemFactory.createPollState(
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

        pollItemFactory.createPollState(
                informationData = A_MESSAGE_INFORMATION_DATA,
                pollResponseSummary = A_POLL_RESPONSE_DATA,
                pollContent = disclosedPollContent,
        ) shouldBe PollState.Ready
    }
}
