/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import im.vector.app.features.home.room.detail.timeline.factory.PollItemViewStateFactory
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import im.vector.app.features.poll.PollItemViewState
import im.vector.app.features.roomprofile.polls.detail.domain.GetEndedPollEventIdUseCase
import im.vector.app.test.fixtures.RoomPollFixture
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val AN_EVENT_ID = "event-id"
private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_TIMESTAMP = 123L

internal class RoomPollDetailMapperTest {

    private val fakePollResponseDataFactory = mockk<PollResponseDataFactory>()
    private val fakePollItemViewStateFactory = mockk<PollItemViewStateFactory>()
    private val fakeGetEndedPollEventIdUseCase = mockk<GetEndedPollEventIdUseCase>()

    private val roomPollDetailMapper = RoomPollDetailMapper(
            pollResponseDataFactory = fakePollResponseDataFactory,
            pollItemViewStateFactory = fakePollItemViewStateFactory,
            getEndedPollEventIdUseCase = fakeGetEndedPollEventIdUseCase,
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
    fun `given a not ended poll event when mapping to model then result contains correct poll details`() {
        // Given
        val aPollItemViewState = givenAPollItemViewState()
        val aPollEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                isClosed = false,
                pollItemViewState = aPollItemViewState,
        )
        val expectedResult = RoomPollDetail(
                creationTimestamp = AN_EVENT_TIMESTAMP,
                isEnded = false,
                endedPollEventId = null,
                pollItemViewState = aPollItemViewState,
        )

        // When
        val result = roomPollDetailMapper.map(aPollEvent)

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given an ended poll event when mapping to model then result contains correct poll details`() {
        // Given
        val aPollItemViewState = givenAPollItemViewState()
        val aPollEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = AN_EVENT_TIMESTAMP,
                isClosed = true,
                pollItemViewState = aPollItemViewState,
        )
        val endedPollEventId = givenEndedPollEventId()
        val expectedResult = RoomPollDetail(
                creationTimestamp = AN_EVENT_TIMESTAMP,
                isEnded = true,
                endedPollEventId = endedPollEventId,
                pollItemViewState = aPollItemViewState,
        )

        // When
        val result = roomPollDetailMapper.map(aPollEvent)

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given missing data in event when mapping to model then result is null`() {
        // Given
        val aPollItemViewState = givenAPollItemViewState()
        val noIdPollEvent = givenAPollTimelineEvent(
                eventId = "",
                creationTimestamp = AN_EVENT_TIMESTAMP,
                isClosed = false,
                pollItemViewState = aPollItemViewState,
        )
        val noTimestampPollEvent = givenAPollTimelineEvent(
                eventId = AN_EVENT_ID,
                creationTimestamp = 0,
                isClosed = false,
                pollItemViewState = aPollItemViewState,
        )
        val notAPollEvent = RoomPollFixture.givenATimelineEvent(
                eventId = AN_EVENT_ID,
                roomId = "room-id",
                creationTimestamp = 0,
                content = mockk<MessageTextContent>(),
        )

        // When
        val result1 = roomPollDetailMapper.map(noIdPollEvent)
        val result2 = roomPollDetailMapper.map(noTimestampPollEvent)
        val result3 = roomPollDetailMapper.map(notAPollEvent)

        // Then
        result1 shouldBe null
        result2 shouldBe null
        result3 shouldBe null
    }

    private fun givenAPollItemViewState(): PollItemViewState {
        return PollItemViewState(
                question = "",
                votesStatus = "",
                canVote = true,
                optionViewStates = emptyList(),
        )
    }

    private fun givenAPollTimelineEvent(
            eventId: String,
            creationTimestamp: Long,
            isClosed: Boolean,
            pollItemViewState: PollItemViewState,
    ): TimelineEvent {
        val pollCreationInfo = RoomPollFixture.givenPollCreationInfo("pollTitle")
        val messageContent = RoomPollFixture.givenAMessagePollContent(pollCreationInfo)
        val timelineEvent = RoomPollFixture.givenATimelineEvent(eventId, A_ROOM_ID, creationTimestamp, messageContent)
        val pollResponseData = RoomPollFixture.givenAPollResponseData(isClosed, totalVotes = 1)
        every { fakePollResponseDataFactory.create(timelineEvent) } returns pollResponseData
        every {
            fakePollItemViewStateFactory.create(
                    pollContent = messageContent,
                    pollResponseData = pollResponseData,
                    isSent = true
            )
        } returns pollItemViewState

        return timelineEvent
    }

    private fun givenEndedPollEventId(): String {
        val eventId = "ended-poll-event-id"
        every {
            fakeGetEndedPollEventIdUseCase.execute(
                    startPollEventId = AN_EVENT_ID,
                    roomId = A_ROOM_ID,
            )
        } returns eventId
        return eventId
    }
}
