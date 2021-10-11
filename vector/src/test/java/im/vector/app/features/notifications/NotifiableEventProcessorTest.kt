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
    fun `given simple events when processing then return without mutating`() {
        val (events, originalEvents) = createEventsList(
                aSimpleNotifiableEvent(eventId = "event-1"),
                aSimpleNotifiableEvent(eventId = "event-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM)

        result shouldBeEqualTo aProcessedNotificationEvents(
                simpleEvents = mapOf(
                        "event-1" to events[0] as SimpleNotifiableEvent,
                        "event-2" to events[1] as SimpleNotifiableEvent
                )
        )
        events shouldBeEqualTo originalEvents
    }

    @Test
    fun `given invites are auto accepted when processing then remove invitations`() {
        autoAcceptInvites._isEnabled = true
        val events = mutableListOf<NotifiableEvent>(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM)

        result shouldBeEqualTo aProcessedNotificationEvents(
                invitationEvents = mapOf(
                        "room-1" to null,
                        "room-2" to null
                )
        )
        events shouldBeEqualTo emptyList()
    }

    @Test
    fun `given invites are not auto accepted when processing then return without mutating`() {
        autoAcceptInvites._isEnabled = false
        val (events, originalEvents) = createEventsList(
                anInviteNotifiableEvent(roomId = "room-1"),
                anInviteNotifiableEvent(roomId = "room-2")
        )

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM)

        result shouldBeEqualTo aProcessedNotificationEvents(
                invitationEvents = mapOf(
                        "room-1" to originalEvents[0] as InviteNotifiableEvent,
                        "room-2" to originalEvents[1] as InviteNotifiableEvent
                )
        )
        events shouldBeEqualTo originalEvents
    }

    @Test
    fun `given out of date message event when processing then removes message`() {
        val (events) = createEventsList(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsOutOfDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM)

        result shouldBeEqualTo aProcessedNotificationEvents(
                roomEvents = mapOf(
                        "room-1" to emptyList()
                )
        )
        events shouldBeEqualTo emptyList()
    }

    @Test
    fun `given in date message event when processing then without mutating`() {
        val (events, originalEvents) = createEventsList(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))
        outdatedDetector.givenEventIsInDate(events[0])

        val result = eventProcessor.process(events, currentRoomId = NOT_VIEWING_A_ROOM)

        result shouldBeEqualTo aProcessedNotificationEvents(
                roomEvents = mapOf(
                        "room-1" to listOf(events[0] as NotifiableMessageEvent)
                )
        )
        events shouldBeEqualTo originalEvents
    }

    @Test
    fun `given viewing the same room as message event when processing then removes message`() {
        val (events) = createEventsList(aNotifiableMessageEvent(eventId = "event-1", roomId = "room-1"))

        val result = eventProcessor.process(events, currentRoomId = "room-1")

        result shouldBeEqualTo aProcessedNotificationEvents(
                roomEvents = mapOf(
                        "room-1" to emptyList()
                )
        )
        events shouldBeEqualTo emptyList()
    }
}

fun createEventsList(vararg event: NotifiableEvent): Pair<MutableList<NotifiableEvent>, List<NotifiableEvent>> {
    val mutableEvents = mutableListOf(*event)
    val immutableEvents = mutableEvents.toList()
    return mutableEvents to immutableEvents
}

fun aProcessedNotificationEvents(simpleEvents: Map<String, SimpleNotifiableEvent?> = emptyMap(),
                                 invitationEvents: Map<String, InviteNotifiableEvent?> = emptyMap(),
                                 roomEvents: Map<String, List<NotifiableMessageEvent>> = emptyMap()
) = GroupedNotificationEvents(
        roomEvents = roomEvents,
        simpleEvents = simpleEvents,
        invitationEvents = invitationEvents,
)

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
