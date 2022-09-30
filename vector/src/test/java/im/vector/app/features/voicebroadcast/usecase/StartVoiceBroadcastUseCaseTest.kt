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

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.STATE_ROOM_VOICE_BROADCAST_INFO
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.test.fakes.FakeRoom
import im.vector.app.test.fakes.FakeRoomService
import im.vector.app.test.fakes.FakeSession
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeNull
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class StartVoiceBroadcastUseCaseTest {

    private val fakeRoom = FakeRoom()
    private val fakeSession = FakeSession(fakeRoomService = FakeRoomService(fakeRoom))
    private val startVoiceBroadcastUseCase = StartVoiceBroadcastUseCase(fakeSession)

    @Test
    fun `given a room id with a potential existing voice broadcast state when calling execute then the voice broadcast is started or not`() = runTest {
        val cases = listOf<VoiceBroadcastState?>(null).plus(VoiceBroadcastState.values()).map {
            when (it) {
                VoiceBroadcastState.STARTED,
                VoiceBroadcastState.PAUSED,
                VoiceBroadcastState.RESUMED -> Case(it, false)
                VoiceBroadcastState.STOPPED,
                null -> Case(it, true)
            }
        }

        cases.forEach { case ->
            if (case.canStartVoiceBroadcast) {
                testVoiceBroadcastStarted(case.previousState)
            } else {
                testVoiceBroadcastNotStarted(case.previousState)
            }
        }
    }

    private suspend fun testVoiceBroadcastStarted(previousState: VoiceBroadcastState?) {
        // Given
        clearAllMocks()
        givenAVoiceBroadcastState(previousState)
        val voiceBroadcastInfoContentInterceptor = slot<Content>()
        coEvery { fakeRoom.stateService().sendStateEvent(any(), any(), capture(voiceBroadcastInfoContentInterceptor)) } coAnswers { AN_EVENT_ID }

        // When
        startVoiceBroadcastUseCase.execute(A_ROOM_ID)

        // Then
        coVerify {
            fakeRoom.stateService().sendStateEvent(
                    eventType = STATE_ROOM_VOICE_BROADCAST_INFO,
                    stateKey = fakeSession.myUserId,
                    body = any(),
            )
        }
        val voiceBroadcastInfoContent = voiceBroadcastInfoContentInterceptor.captured.toModel<MessageVoiceBroadcastInfoContent>()
        voiceBroadcastInfoContent?.voiceBroadcastState shouldBe VoiceBroadcastState.STARTED
        voiceBroadcastInfoContent?.relatesTo.shouldBeNull()
    }

    private suspend fun testVoiceBroadcastNotStarted(previousState: VoiceBroadcastState?) {
        // Given
        clearAllMocks()
        givenAVoiceBroadcastState(previousState)

        // When
        startVoiceBroadcastUseCase.execute(A_ROOM_ID)

        // Then
        coVerify(exactly = 0) { fakeRoom.stateService().sendStateEvent(any(), any(), any()) }
    }

    private fun givenAVoiceBroadcastState(state: VoiceBroadcastState?) {
        val event = state?.let {
            Event(
                    type = STATE_ROOM_VOICE_BROADCAST_INFO,
                    stateKey = fakeSession.myUserId,
                    content = MessageVoiceBroadcastInfoContent(
                            voiceBroadcastStateStr = state.value
                    ).toContent()
            )
        }
        fakeRoom.stateService().givenGetStateEvent(event)
    }

    private data class Case(val previousState: VoiceBroadcastState?, val canStartVoiceBroadcast: Boolean)
}
