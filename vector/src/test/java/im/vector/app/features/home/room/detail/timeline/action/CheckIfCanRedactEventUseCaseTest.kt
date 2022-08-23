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

package im.vector.app.features.home.room.detail.timeline.action

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class CheckIfCanRedactEventUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val checkIfCanRedactEventUseCase = CheckIfCanRedactEventUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given an event which can be redacted and owned by user when use case executes then the result is true`() {
        val canRedactEventTypes = listOf(EventType.MESSAGE, EventType.STICKER) +
                EventType.POLL_START + EventType.STATE_ROOM_BEACON_INFO

        canRedactEventTypes.forEach { eventType ->
            val event = givenAnEvent(
                    eventType = eventType,
                    senderId = fakeActiveSessionHolder.fakeSession.myUserId
            )

            val actionPermissions = givenActionPermissions(canRedact = false)

            val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

            result shouldBe true
        }
    }

    @Test
    fun `given redact permission and an event which can be redacted and sent by another user when use case executes then the result is true`() {
        val event = givenAnEvent(
                eventType = EventType.MESSAGE,
                senderId = "user-id"
        )

        val actionPermissions = givenActionPermissions(canRedact = true)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe true
    }

    @Test
    fun `given an event which cannot be redacted when use case executes then the result is false`() {
        val event = givenAnEvent(
                eventType = EventType.CALL_ANSWER,
                senderId = fakeActiveSessionHolder.fakeSession.myUserId
        )

        val actionPermissions = givenActionPermissions(canRedact = false)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe false
    }

    @Test
    fun `given missing redact permission and an event which can be redacted and sent by another user when use case executes then the result is false`() {
        val event = givenAnEvent(
                eventType = EventType.MESSAGE,
                senderId = "user-id"
        )

        val actionPermissions = givenActionPermissions(canRedact = false)

        val result = checkIfCanRedactEventUseCase.execute(event, actionPermissions)

        result shouldBe false
    }

    private fun givenAnEvent(eventType: String, senderId: String): TimelineEvent {
        val eventId = "event-id"
        return TimelineEvent(
                root = Event(
                        eventId = eventId,
                        type = eventType,
                        senderId = senderId
                ),
                localId = 123L,
                eventId = eventId,
                displayIndex = 1,
                ownedByThreadChunk = false,
                senderInfo = mockk()
        )
    }

    private fun givenActionPermissions(canRedact: Boolean): ActionPermissions {
        return ActionPermissions(canRedact = canRedact)
    }
}
