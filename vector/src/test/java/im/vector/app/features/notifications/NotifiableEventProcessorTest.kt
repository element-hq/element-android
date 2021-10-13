/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.notifications

import im.vector.app.test.fakes.FakeAutoAcceptInvites
import im.vector.app.test.fakes.FakeOutdatedEventDetector
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val NOT_VIEWING_A_ROOM: String? = null

class NotifiableEventProcessorTest {

    private val outdatedDetector = FakeOutdatedEventDetector()
    private val autoAcceptInvites = FakeAutoAcceptInvites()

    private val eventProcessor = NotifiableEventProcessor(outdatedDetector.instance, autoAcceptInvites)

    @Test
    fun `given simple events when processing then keep simple events`() {
        val events = listOf(
                aSimpleNotifiableEvent(eventId = "event-1"),
                aSimpleNotifiableEvent(eventId = "event-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.KEEP to events[0],
                ProcessedType.KEEP to events[1]
        )
    }

    @Test
    fun `given invites are auto accepted when processing then remove invitations`() {
        autoAcceptInvites._isEnabled = true
        val events = listOf<NotifiableEvent>(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.REMOVE to events[0],
                ProcessedType.REMOVE to events[1]
        )
    }

    @Test
    fun `given invites are not auto accepted when processing then keep invitation events`() {
        autoAcceptInvites._isEnabled = false
        val events = listOf(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.KEEP to events[0],
                ProcessedType.KEEP to events[1]
        )
    }

    @Test
    fun `given out of date message event when processing then removes message event`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsOutOfDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.REMOVE to events[0],
        )
    }

    @Test
    fun `given in date message event when processing then keep message event`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsInDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.KEEP to events[0],
        )
    }

    @Test
    fun `given viewing the same room as message event when processing then removes message`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))

        val result = eventProcessor.process(events, currentRoomId = "room-1", renderedEventsList = emptyList())

        result shouldBeEqualTo listOf(
                ProcessedType.REMOVE to events[0],
        )
    }

    @Test
    fun `given events are different to rendered events when processing then removes difference`() {
        val events = listOf(aSimpleNotifiableEvent(eventId = "event-1"))
        val renderedEvents = listOf(
                ProcessedType.KEEP to events[0],
                ProcessedType.KEEP to anInviteNotifiableEvent(roomId = "event-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEventsList = renderedEvents)

        result shouldBeEqualTo listOf(
                ProcessedType.REMOVE to renderedEvents[1].second,
                ProcessedType.KEEP to renderedEvents[0].second
        )
    }
}

fun aSimpleNotifiableEvent(eventId: String) = SimpleNotifiableEvent(
        matrixID = null,
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        title = "title",
        description = "description",
        type = null,
        timestamp = 0,
        soundName = null,
        canBeReplaced = false,
        isRedacted = false
)

fun anInviteNotifiableEvent(roomId: String) = InviteNotifiableEvent(
        matrixID = null,
        eventId = "event-id",
        roomId = roomId,
        roomName = "a room name",
        editedEventId = null,
        noisy = false,
        title = "title",
        description = "description",
        type = null,
        timestamp = 0,
        soundName = null,
        canBeReplaced = false,
        isRedacted = false
)

fun aNotifiableMessageEvent(eventId: String, roomId: String) = NotifiableMessageEvent(
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        timestamp = 0,
        senderName = "sender-name",
        senderId = "sending-id",
        body = "message-body",
        roomId = roomId,
        roomName = "room-name",
        roomIsDirect = false,
        canBeReplaced = false,
        isRedacted = false
)
