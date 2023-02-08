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
import im.vector.app.features.home.room.detail.timeline.factory.PollOptionViewStateFactory
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val AN_EVENT_ID = "event-id"
private const val AN_EVENT_TIMESTAMP = 123L
private const val A_POLL_TITLE = "poll-title"

internal class PollSummaryMapperTest {

    private val fakePollResponseDataFactory = mockk<PollResponseDataFactory>()
    private val fakePollOptionViewStateFactory = mockk<PollOptionViewStateFactory>()

    private val pollSummaryMapper = PollSummaryMapper(
            pollResponseDataFactory = fakePollResponseDataFactory,
            pollOptionViewStateFactory = fakePollOptionViewStateFactory,
    )

    @Before
    fun setup() {
        mockkStatic("im.vector.app.core.extensions.TimelineEventKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a not ended poll event when mapping to model then result is active poll`() {
        // Given
        val pollStartedEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                pollTitle = A_POLL_TITLE,
                isClosed = false,
        )
        val expectedResult = PollSummary.ActivePoll(
                id = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                title = A_POLL_TITLE,
        )

        // When
        val result = pollSummaryMapper.map(pollStartedEvent)

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given an ended poll event when mapping to model then result is ended poll`() {
        // Given
        val totalVotes = 10
        val winnerOptions = listOf<PollOptionViewState.PollEnded>()
        val endedPollEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                pollTitle = A_POLL_TITLE,
                isClosed = true,
                totalVotes = totalVotes,
                winnerOptions = winnerOptions,
        )
        val expectedResult = PollSummary.EndedPoll(
                id = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                title = A_POLL_TITLE,
                totalVotes = totalVotes,
                winnerOptions = winnerOptions,
        )

        // When
        val result = pollSummaryMapper.map(endedPollEvent)

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given missing data in event when mapping to model then result is null`() {
        // Given
        val noIdPollEvent = givenAPollTimelineEvent(
                eventId = "",
                creationTimestamp = AN_EVENT_TIMESTAMP,
                pollTitle = A_POLL_TITLE,
                isClosed = false,
        )
        val noTimestampPollEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = 0,
                pollTitle = A_POLL_TITLE,
                isClosed = false,
        )
        val notAPollEvent = givenATimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = 0,
                content = mockk<MessageTextContent>()
        )

        // When
        val result1 = pollSummaryMapper.map(noIdPollEvent)
        val result2 = pollSummaryMapper.map(noTimestampPollEvent)
        val result3 = pollSummaryMapper.map(notAPollEvent)

        // Then
        result1 shouldBe null
        result2 shouldBe null
        result3 shouldBe null
    }

    private fun givenATimelineEvent(
            eventId: String,
            creationTimestamp: Long,
            content: MessageContent,
    ): TimelineEvent {
        val timelineEvent = mockk<TimelineEvent>()
        every { timelineEvent.root.eventId } returns eventId
        every { timelineEvent.root.originServerTs } returns creationTimestamp
        every { timelineEvent.getVectorLastMessageContent() } returns content
        return timelineEvent
    }

    private fun givenAPollTimelineEvent(
            eventId: String,
            creationTimestamp: Long,
            pollTitle: String,
            isClosed: Boolean,
            totalVotes: Int = 0,
            winnerOptions: List<PollOptionViewState.PollEnded> = emptyList(),
    ): TimelineEvent {
        val pollCreationInfo = givenPollCreationInfo(pollTitle)
        val messageContent = givenAMessagePollContent(pollCreationInfo)
        val timelineEvent = givenATimelineEvent(eventId, creationTimestamp, messageContent)
        val pollResponseData = givenAPollResponseData(isClosed, totalVotes)
        every { fakePollResponseDataFactory.create(timelineEvent) } returns pollResponseData
        every {
            fakePollOptionViewStateFactory.createPollEndedOptions(
                    pollCreationInfo,
                    pollResponseData
            )
        } returns winnerOptions

        return timelineEvent
    }

    private fun givenAMessagePollContent(pollCreationInfo: PollCreationInfo): MessagePollContent {
        return MessagePollContent(
                unstablePollCreationInfo = pollCreationInfo,
        )
    }

    private fun givenPollCreationInfo(pollTitle: String): PollCreationInfo {
        return PollCreationInfo(
                question = PollQuestion(unstableQuestion = pollTitle),
        )
    }

    private fun givenAPollResponseData(isClosed: Boolean, totalVotes: Int): PollResponseData {
        return PollResponseData(
                myVote = "",
                votes = emptyMap(),
                isClosed = isClosed,
                totalVotes = totalVotes,
        )
    }
}
