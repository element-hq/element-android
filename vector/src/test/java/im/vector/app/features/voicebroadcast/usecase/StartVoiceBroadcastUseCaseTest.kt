/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.recording.usecase.StartVoiceBroadcastUseCase
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeRoom
import im.vector.app.test.fakes.FakeRoomService
import im.vector.app.test.fakes.FakeSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeNull
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"
private const val A_USER_ID = "user_id"

class StartVoiceBroadcastUseCaseTest {

    private val fakeRoom = FakeRoom()
    private val fakeSession = FakeSession(fakeRoomService = FakeRoomService(fakeRoom))
    private val fakeVoiceBroadcastRecorder = mockk<VoiceBroadcastRecorder>(relaxed = true)
    private val fakeGetRoomLiveVoiceBroadcastsUseCase = mockk<GetRoomLiveVoiceBroadcastsUseCase>()
    private val startVoiceBroadcastUseCase = spyk(
            StartVoiceBroadcastUseCase(
                    session = fakeSession,
                    voiceBroadcastRecorder = fakeVoiceBroadcastRecorder,
                    playbackTracker = mockk(),
                    context = FakeContext().instance,
                    buildMeta = mockk(),
                    getRoomLiveVoiceBroadcastsUseCase = fakeGetRoomLiveVoiceBroadcastsUseCase,
                    stopVoiceBroadcastUseCase = mockk(),
                    pauseVoiceBroadcastUseCase = mockk(),
            )
    )

    @Before
    fun setup() {
        every { fakeRoom.roomId } returns A_ROOM_ID
        justRun { startVoiceBroadcastUseCase.assertHasEnoughPowerLevels(fakeRoom) }
        every { fakeVoiceBroadcastRecorder.recordingState } returns VoiceBroadcastRecorder.State.Idle
    }

    @Test
    fun `given a room id with potential several existing voice broadcast states when calling execute then the voice broadcast is started or not`() = runTest {
        val cases = VoiceBroadcastState.values()
                .flatMap { first ->
                    VoiceBroadcastState.values().map { second ->
                        Case(
                                voiceBroadcasts = listOf(VoiceBroadcast(fakeSession.myUserId, first), VoiceBroadcast(A_USER_ID, second)),
                                canStartVoiceBroadcast = first == VoiceBroadcastState.STOPPED && second == VoiceBroadcastState.STOPPED
                        )
                    }
                }
                .plus(Case(emptyList(), true))

        cases.forEach { case ->
            if (case.canStartVoiceBroadcast) {
                testVoiceBroadcastStarted(case.voiceBroadcasts)
            } else {
                testVoiceBroadcastNotStarted(case.voiceBroadcasts)
            }
        }
    }

    private suspend fun testVoiceBroadcastStarted(voiceBroadcasts: List<VoiceBroadcast>) {
        // Given
        setup()
        givenVoiceBroadcasts(voiceBroadcasts)
        val voiceBroadcastInfoContentInterceptor = slot<Content>()
        coEvery { fakeRoom.stateService().sendStateEvent(any(), any(), capture(voiceBroadcastInfoContentInterceptor)) } coAnswers { AN_EVENT_ID }

        // When
        startVoiceBroadcastUseCase.execute(A_ROOM_ID)

        // Then
        coVerify {
            fakeRoom.stateService().sendStateEvent(
                    eventType = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                    stateKey = fakeSession.myUserId,
                    body = any(),
            )
        }
        val voiceBroadcastInfoContent = voiceBroadcastInfoContentInterceptor.captured.toModel<MessageVoiceBroadcastInfoContent>()
        voiceBroadcastInfoContent?.voiceBroadcastState shouldBe VoiceBroadcastState.STARTED
        voiceBroadcastInfoContent?.relatesTo.shouldBeNull()
    }

    private suspend fun testVoiceBroadcastNotStarted(voiceBroadcasts: List<VoiceBroadcast>) {
        // Given
        setup()
        givenVoiceBroadcasts(voiceBroadcasts)

        // When
        startVoiceBroadcastUseCase.execute(A_ROOM_ID)

        // Then
        coVerify(exactly = 0) { fakeRoom.stateService().sendStateEvent(any(), any(), any()) }
    }

    private fun givenVoiceBroadcasts(voiceBroadcasts: List<VoiceBroadcast>) {
        val events = voiceBroadcasts.map {
            Event(
                    type = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                    stateKey = it.userId,
                    content = MessageVoiceBroadcastInfoContent(
                            voiceBroadcastStateStr = it.state.value
                    ).toContent()
            )
        }
                .mapNotNull { it.asVoiceBroadcastEvent() }
                .filter { it.content?.voiceBroadcastState != VoiceBroadcastState.STOPPED }
        every { fakeGetRoomLiveVoiceBroadcastsUseCase.execute(any()) } returns events
    }

    private data class VoiceBroadcast(val userId: String, val state: VoiceBroadcastState)
    private data class Case(val voiceBroadcasts: List<VoiceBroadcast>, val canStartVoiceBroadcast: Boolean)
}
