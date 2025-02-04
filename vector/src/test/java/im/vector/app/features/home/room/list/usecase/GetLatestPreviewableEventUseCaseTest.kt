/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants.VOICE_BROADCAST_CHUNK_KEY
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetRoomLiveVoiceBroadcastsUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeRoom
import im.vector.app.test.fakes.FakeVectorPreferences
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

private const val A_ROOM_ID = "a-room-id"

internal class GetLatestPreviewableEventUseCaseTest {

    private val fakeRoom = FakeRoom()
    private val fakeSessionHolder = FakeActiveSessionHolder()
    private val fakeRoomSummary = mockk<RoomSummary>()
    private val fakeGetRoomLiveVoiceBroadcastsUseCase = mockk<GetRoomLiveVoiceBroadcastsUseCase>()
    private val fakeVectorPreferences = FakeVectorPreferences()

    private val getLatestPreviewableEventUseCase = GetLatestPreviewableEventUseCase(
            fakeSessionHolder.instance,
            fakeGetRoomLiveVoiceBroadcastsUseCase,
            fakeVectorPreferences.instance,
    )

    @Before
    fun setup() {
        every { fakeSessionHolder.instance.getSafeActiveSession()?.getRoom(A_ROOM_ID) } returns fakeRoom
        every { fakeRoom.roomSummary() } returns fakeRoomSummary
        every { fakeRoom.roomId } returns A_ROOM_ID
        every { fakeRoom.timelineService().getTimelineEvent(any()) } answers {
            mockk(relaxed = true) {
                every { eventId } returns firstArg()
            }
        }
        fakeVectorPreferences.givenIsVoiceBroadcastEnabled(true)
    }

    @Test
    fun `given the latest event is a call invite and there is a live broadcast, when execute, returns the call event`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { root.type } returns EventType.MESSAGE
            every { root.getClearType() } returns EventType.CALL_INVITE
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns listOf(
                givenAVoiceBroadcastEvent("id1", VoiceBroadcastState.STARTED, "id1"),
                givenAVoiceBroadcastEvent("id2", VoiceBroadcastState.RESUMED, "id1"),
        ).mapNotNull { it.asVoiceBroadcastEvent() }

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result shouldBe aLatestPreviewableEvent
    }

    @Test
    fun `given the latest event is not a call invite and there is a live broadcast, when execute, returns the latest broadcast event`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { root.type } returns EventType.MESSAGE
            every { root.getClearType() } returns EventType.MESSAGE
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns listOf(
                givenAVoiceBroadcastEvent("id1", VoiceBroadcastState.STARTED, "vb_id1"),
                givenAVoiceBroadcastEvent("id2", VoiceBroadcastState.RESUMED, "vb_id2"),
        ).mapNotNull { it.asVoiceBroadcastEvent() }

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result?.eventId shouldBeEqualTo "vb_id2"
    }

    @Test
    fun `given there is no live broadcast, when execute, returns the latest event`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { root.type } returns EventType.MESSAGE
            every { root.getClearType() } returns EventType.MESSAGE
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns emptyList()

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result shouldBe aLatestPreviewableEvent
    }

    @Test
    fun `given there is no live broadcast and the latest event is a vb message, when execute, returns null`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { root.type } returns EventType.MESSAGE
            every { root.getClearType() } returns EventType.MESSAGE
            every { root.getClearContent() } returns mapOf(
                    MessageContent.MSG_TYPE_JSON_KEY to "m.audio",
                    VOICE_BROADCAST_CHUNK_KEY to "1",
                    "body" to "",
            )
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns emptyList()

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `given the latest event is an ended vb, when execute, returns the stopped event`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { eventId } returns "id1"
            every { root } returns givenAVoiceBroadcastEvent("id1", VoiceBroadcastState.STOPPED, "vb_id1")
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns emptyList()

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result?.eventId shouldBeEqualTo "id1"
    }

    @Test
    fun `given the latest event is a resumed vb, when execute, returns the started event`() {
        // Given
        val aLatestPreviewableEvent = mockk<TimelineEvent> {
            every { eventId } returns "id1"
            every { root } returns givenAVoiceBroadcastEvent("id1", VoiceBroadcastState.RESUMED, "vb_id1")
        }
        every { fakeRoomSummary.latestPreviewableEvent } returns aLatestPreviewableEvent
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(A_ROOM_ID) } returns emptyList()

        // When
        val result = getLatestPreviewableEventUseCase.execute(A_ROOM_ID)

        // Then
        result?.eventId shouldBeEqualTo "vb_id1"
    }

    private fun givenAVoiceBroadcastEvent(
            eventId: String,
            state: VoiceBroadcastState,
            voiceBroadcastId: String,
    ): Event = mockk {
        every { this@mockk.eventId } returns eventId
        every { getClearType() } returns VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
        every { type } returns VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
        every { content } returns mapOf(
                "state" to state.value,
                "m.relates_to" to mapOf(
                        "rel_type" to RelationType.REFERENCE,
                        "event_id" to voiceBroadcastId
                )
        )
    }
}
