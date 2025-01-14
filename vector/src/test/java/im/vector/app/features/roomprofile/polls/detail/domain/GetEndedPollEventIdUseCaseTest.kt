/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.domain

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isPollEnd
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "room-id"
private const val A_START_POLL_EVENT_ID = "start-poll-id"
private const val AN_END_POLL_EVENT_ID = "end-poll-id"

internal class GetEndedPollEventIdUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val getEndedPollEventIdUseCase = GetEndedPollEventIdUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.api.session.events.model.EventKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given existing related end event when execute then result is id of the end event`() {
        // Given
        val timelineEvent = givenEvent(eventId = AN_END_POLL_EVENT_ID, isPollEnd = true)
        givenARelatedEvent(timelineEvent = timelineEvent)

        // When
        val result = getEndedPollEventIdUseCase.execute(A_ROOM_ID, A_START_POLL_EVENT_ID)

        // Then
        result shouldBe AN_END_POLL_EVENT_ID
    }

    @Test
    fun `given existing related event but not end poll event when execute then result is null`() {
        // Given
        val timelineEvent = givenEvent(eventId = AN_END_POLL_EVENT_ID, isPollEnd = false)
        givenARelatedEvent(timelineEvent = timelineEvent)

        // When
        val result = getEndedPollEventIdUseCase.execute(A_ROOM_ID, A_START_POLL_EVENT_ID)

        // Then
        result shouldBe null
    }

    @Test
    fun `given no existing related event when execute then result is null`() {
        // Given
        givenARelatedEvent(timelineEvent = null)

        // When
        val result = getEndedPollEventIdUseCase.execute(A_ROOM_ID, A_START_POLL_EVENT_ID)

        // Then
        result shouldBe null
    }

    @Test
    fun `given error occurred when execute then result is null`() {
        // Given
        givenErrorDuringRequest(error = Exception())

        // When
        val result = getEndedPollEventIdUseCase.execute(A_ROOM_ID, A_START_POLL_EVENT_ID)

        // Then
        result shouldBe null
    }

    private fun givenEvent(eventId: String, isPollEnd: Boolean): TimelineEvent {
        val timelineEvent = mockk<TimelineEvent>()
        val event = mockk<Event>()
        every { timelineEvent.root } returns event
        every { timelineEvent.eventId } returns eventId
        every { event.isPollEnd() } returns isPollEnd
        return timelineEvent
    }

    private fun givenARelatedEvent(timelineEvent: TimelineEvent?) {
        val result: List<TimelineEvent> = timelineEvent?.let { listOf(it) } ?: emptyList()
        every {
            fakeActiveSessionHolder.instance
                    .getActiveSession()
                    .roomService()
                    .getRoom(A_ROOM_ID)
                    ?.timelineService()
                    ?.getTimelineEventsRelatedTo(RelationType.REFERENCE, A_START_POLL_EVENT_ID)
        } returns result
    }

    private fun givenErrorDuringRequest(error: Exception) {
        every {
            fakeActiveSessionHolder.instance
                    .getActiveSession()
                    .roomService()
                    .getRoom(A_ROOM_ID)
                    ?.timelineService()
                    ?.getTimelineEventsRelatedTo(RelationType.REFERENCE, A_START_POLL_EVENT_ID)
        } throws error
    }
}
