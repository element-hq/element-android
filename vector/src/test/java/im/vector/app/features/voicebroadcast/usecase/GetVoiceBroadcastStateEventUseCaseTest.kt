/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "A_ROOM_ID"
private const val A_VOICE_BROADCAST_ID = "A_VOICE_BROADCAST_ID"

internal class GetVoiceBroadcastStateEventUseCaseTest {

    private val fakeSession = FakeSession()
    private val getVoiceBroadcastStateEventUseCase = GetVoiceBroadcastStateEventUseCase(fakeSession)

    @Test
    fun `given there is no event related to the given vb, when execute, then return null`() {
        // Given
        val aVoiceBroadcast = VoiceBroadcast(A_VOICE_BROADCAST_ID, A_ROOM_ID)
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEvent(A_VOICE_BROADCAST_ID) } returns null
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEventsRelatedTo(any(), any()) } returns emptyList()

        // When
        val result = getVoiceBroadcastStateEventUseCase.execute(aVoiceBroadcast)

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `given there are several related events related to the given vb, when execute, then return the most recent one`() {
        // Given
        val aVoiceBroadcast = VoiceBroadcast(A_VOICE_BROADCAST_ID, A_ROOM_ID)
        val aListOfTimelineEvents = listOf(
                givenAVoiceBroadcastEvent(eventId = A_VOICE_BROADCAST_ID, state = VoiceBroadcastState.STARTED, isRedacted = false, timestamp = 1L),
                givenAVoiceBroadcastEvent(eventId = "event_id_3", state = VoiceBroadcastState.STOPPED, isRedacted = false, timestamp = 3L),
                givenAVoiceBroadcastEvent(eventId = "event_id_2", state = VoiceBroadcastState.PAUSED, isRedacted = false, timestamp = 2L),
        )
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEventsRelatedTo(any(), any()) } returns aListOfTimelineEvents

        // When
        val result = getVoiceBroadcastStateEventUseCase.execute(aVoiceBroadcast)

        // Then
        result.shouldNotBeNull()
        result.root.eventId shouldBeEqualTo "event_id_3"
    }

    @Test
    fun `given there are several related events related to the given vb, when execute, then return the most recent one which is not redacted`() {
        // Given
        val aVoiceBroadcast = VoiceBroadcast(A_VOICE_BROADCAST_ID, A_ROOM_ID)
        val aListOfTimelineEvents = listOf(
                givenAVoiceBroadcastEvent(eventId = A_VOICE_BROADCAST_ID, state = VoiceBroadcastState.STARTED, isRedacted = false, timestamp = 1L),
                givenAVoiceBroadcastEvent(eventId = "event_id_2", state = VoiceBroadcastState.STOPPED, isRedacted = true, timestamp = 2L),
        )
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEventsRelatedTo(any(), any()) } returns aListOfTimelineEvents

        // When
        val result = getVoiceBroadcastStateEventUseCase.execute(aVoiceBroadcast)

        // Then
        result.shouldNotBeNull()
        result.root.eventId shouldBeEqualTo A_VOICE_BROADCAST_ID
    }

    @Test
    fun `given a not ended voice broadcast with a redacted start event, when execute, then return null`() {
        // Given
        val aVoiceBroadcast = VoiceBroadcast(A_VOICE_BROADCAST_ID, A_ROOM_ID)
        val aListOfTimelineEvents = listOf(
                givenAVoiceBroadcastEvent(eventId = A_VOICE_BROADCAST_ID, state = VoiceBroadcastState.STARTED, isRedacted = true, timestamp = 1L),
                givenAVoiceBroadcastEvent(eventId = "event_id_2", state = VoiceBroadcastState.PAUSED, isRedacted = false, timestamp = 2L),
                givenAVoiceBroadcastEvent(eventId = "event_id_3", state = VoiceBroadcastState.RESUMED, isRedacted = false, timestamp = 3L),
        )
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEventsRelatedTo(any(), any()) } returns aListOfTimelineEvents

        // When
        val result = getVoiceBroadcastStateEventUseCase.execute(aVoiceBroadcast)

        // Then
        result.shouldBeNull()
    }

    private fun givenAVoiceBroadcastEvent(
            eventId: String,
            state: VoiceBroadcastState,
            isRedacted: Boolean,
            timestamp: Long,
    ): TimelineEvent {
        val timelineEvent = mockk<TimelineEvent> {
            every { root.eventId } returns eventId
            every { root.type } returns VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
            every { root.content } returns mapOf("state" to state.value)
            every { root.isRedacted() } returns isRedacted
            every { root.originServerTs } returns timestamp
        }
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEvent(eventId) } returns timelineEvent
        return timelineEvent
    }
}
