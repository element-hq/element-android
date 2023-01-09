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

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
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
                givenAVoiceBroadcastEvent(eventId = "event_id_1", isRedacted = false, timestamp = 1L),
                givenAVoiceBroadcastEvent(eventId = "event_id_3", isRedacted = false, timestamp = 3L),
                givenAVoiceBroadcastEvent(eventId = "event_id_2", isRedacted = false, timestamp = 2L),
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
                givenAVoiceBroadcastEvent(eventId = "event_id_1", isRedacted = false, timestamp = 1L),
                givenAVoiceBroadcastEvent(eventId = "event_id_2", isRedacted = true, timestamp = 2L),
        )
        every { fakeSession.getRoom(A_ROOM_ID)?.timelineService()?.getTimelineEventsRelatedTo(any(), any()) } returns aListOfTimelineEvents

        // When
        val result = getVoiceBroadcastStateEventUseCase.execute(aVoiceBroadcast)

        // Then
        result.shouldNotBeNull()
        result.root.eventId shouldBeEqualTo "event_id_1"
    }

    private fun givenAVoiceBroadcastEvent(
            eventId: String,
            isRedacted: Boolean,
            timestamp: Long,
    ) = mockk<TimelineEvent>(relaxed = true) {
        every { root.eventId } returns eventId
        every { root.type } returns VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
        every { root.isRedacted() } returns isRedacted
        every { root.originServerTs } returns timestamp
    }
}
