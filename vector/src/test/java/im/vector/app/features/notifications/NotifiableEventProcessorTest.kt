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

import im.vector.app.features.notifications.ProcessedEvent.Type
import im.vector.app.test.fakes.FakeAutoAcceptInvites
import im.vector.app.test.fakes.FakeOutdatedEventDetector
import im.vector.app.test.fixtures.aNotifiableMessageEvent
import im.vector.app.test.fixtures.aSimpleNotifiableEvent
import im.vector.app.test.fixtures.anInviteNotifiableEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.EventType

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

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.KEEP to events[0],
                Type.KEEP to events[1]
        )
    }

    @Test
    fun `given redacted simple event when processing then remove redaction event`() {
        val events = listOf(aSimpleNotifiableEvent(eventId = "event-1", type = EventType.REDACTION))

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.REMOVE to events[0]
        )
    }

    @Test
    fun `given invites are auto accepted when processing then remove invitations`() {
        autoAcceptInvites._isEnabled = true
        val events = listOf<NotifiableEvent>(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.REMOVE to events[0],
                Type.REMOVE to events[1]
        )
    }

    @Test
    fun `given invites are not auto accepted when processing then keep invitation events`() {
        autoAcceptInvites._isEnabled = false
        val events = listOf(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.KEEP to events[0],
                Type.KEEP to events[1]
        )
    }

    @Test
    fun `given out of date message event when processing then removes message event`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsOutOfDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.REMOVE to events[0],
        )
    }

    @Test
    fun `given in date message event when processing then keep message event`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsInDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.KEEP to events[0],
        )
    }

    @Test
    fun `given viewing the same room as message event when processing then removes message`() {
        val events = listOf(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))

        val result = eventProcessor.process(events, currentRoomId = "room-1", renderedEvents = emptyList())

        result shouldBeEqualTo listOfProcessedEvents(
                Type.REMOVE to events[0],
        )
    }

    @Test
    fun `given events are different to rendered events when processing then removes difference`() {
        val events = listOf(aSimpleNotifiableEvent(eventId = "event-1"))
        val renderedEvents = listOf<ProcessedEvent<NotifiableEvent>>(
                ProcessedEvent(Type.KEEP, events[0]),
                ProcessedEvent(Type.KEEP, anInviteNotifiableEvent(roomId = "event-2"))
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM, renderedEvents = renderedEvents)

        result shouldBeEqualTo listOfProcessedEvents(
                Type.REMOVE to renderedEvents[1].event,
                Type.KEEP to renderedEvents[0].event
        )
    }

    private fun listOfProcessedEvents(vararg event: Pair<Type, NotifiableEvent>) = event.map {
        ProcessedEvent(it.first, it.second)
    }
}
