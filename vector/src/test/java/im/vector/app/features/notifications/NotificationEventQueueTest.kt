/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import im.vector.app.test.fixtures.aNotifiableMessageEvent
import im.vector.app.test.fixtures.aSimpleNotifiableEvent
import im.vector.app.test.fixtures.anInviteNotifiableEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class NotificationEventQueueTest {

    private val seenIdsCache = CircularCache.create<String>(5)

    @Test
    fun `given events when redacting some then marks matching event ids as redacted`() {
        val queue = givenQueue(
                listOf(
                        aSimpleNotifiableEvent(eventId = "redacted-id-1"),
                        aNotifiableMessageEvent(eventId = "redacted-id-2"),
                        anInviteNotifiableEvent(eventId = "redacted-id-3"),
                        aSimpleNotifiableEvent(eventId = "kept-id"),
                )
        )

        queue.markRedacted(listOf("redacted-id-1", "redacted-id-2", "redacted-id-3"))

        queue.rawEvents() shouldBeEqualTo listOf(
                aSimpleNotifiableEvent(eventId = "redacted-id-1", isRedacted = true),
                aNotifiableMessageEvent(eventId = "redacted-id-2", isRedacted = true),
                anInviteNotifiableEvent(eventId = "redacted-id-3", isRedacted = true),
                aSimpleNotifiableEvent(eventId = "kept-id", isRedacted = false),
        )
    }

    @Test
    fun `given invite event when leaving invited room and syncing then removes event`() {
        val queue = givenQueue(listOf(anInviteNotifiableEvent(roomId = "a-room-id")))
        val roomsLeft = listOf("a-room-id")

        queue.syncRoomEvents(roomsLeft = roomsLeft, roomsJoined = emptyList())

        queue.rawEvents() shouldBeEqualTo emptyList()
    }

    @Test
    fun `given invite event when joining invited room and syncing then removes event`() {
        val queue = givenQueue(listOf(anInviteNotifiableEvent(roomId = "a-room-id")))
        val joinedRooms = listOf("a-room-id")

        queue.syncRoomEvents(roomsLeft = emptyList(), roomsJoined = joinedRooms)

        queue.rawEvents() shouldBeEqualTo emptyList()
    }

    @Test
    fun `given message event when leaving message room and syncing then removes event`() {
        val queue = givenQueue(listOf(aNotifiableMessageEvent(roomId = "a-room-id")))
        val roomsLeft = listOf("a-room-id")

        queue.syncRoomEvents(roomsLeft = roomsLeft, roomsJoined = emptyList())

        queue.rawEvents() shouldBeEqualTo emptyList()
    }

    @Test
    fun `given events when syncing without rooms left or joined ids then does not change the events`() {
        val queue = givenQueue(
                listOf(
                        aNotifiableMessageEvent(roomId = "a-room-id"),
                        anInviteNotifiableEvent(roomId = "a-room-id")
                )
        )

        queue.syncRoomEvents(roomsLeft = emptyList(), roomsJoined = emptyList())

        queue.rawEvents() shouldBeEqualTo listOf(
                aNotifiableMessageEvent(roomId = "a-room-id"),
                anInviteNotifiableEvent(roomId = "a-room-id")
        )
    }

    @Test
    fun `given events then is not empty`() {
        val queue = givenQueue(listOf(aSimpleNotifiableEvent()))

        queue.isEmpty() shouldBeEqualTo false
    }

    @Test
    fun `given no events then is empty`() {
        val queue = givenQueue(emptyList())

        queue.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `given events when clearing and adding then removes previous events and adds only new events`() {
        val queue = givenQueue(listOf(aSimpleNotifiableEvent()))

        queue.clearAndAdd(listOf(anInviteNotifiableEvent()))

        queue.rawEvents() shouldBeEqualTo listOf(anInviteNotifiableEvent())
    }

    @Test
    fun `when clearing then is empty`() {
        val queue = givenQueue(listOf(aSimpleNotifiableEvent()))

        queue.clear()

        queue.rawEvents() shouldBeEqualTo emptyList()
    }

    @Test
    fun `given no events when adding then adds event`() {
        val queue = givenQueue(listOf())

        queue.add(aSimpleNotifiableEvent())

        queue.rawEvents() shouldBeEqualTo listOf(aSimpleNotifiableEvent())
    }

    @Test
    fun `given no events when adding already seen event then ignores event`() {
        val queue = givenQueue(listOf())
        val notifiableEvent = aSimpleNotifiableEvent()
        seenIdsCache.put(notifiableEvent.eventId)

        queue.add(notifiableEvent)

        queue.rawEvents() shouldBeEqualTo emptyList()
    }

    @Test
    fun `given replaceable event when adding event with same id then updates existing event`() {
        val replaceableEvent = aSimpleNotifiableEvent(canBeReplaced = true)
        val updatedEvent = replaceableEvent.copy(title = "updated title", isUpdated = true)
        val queue = givenQueue(listOf(replaceableEvent))

        queue.add(updatedEvent)

        queue.rawEvents() shouldBeEqualTo listOf(updatedEvent)
    }

    @Test
    fun `given non replaceable event when adding event with same id then ignores event`() {
        val nonReplaceableEvent = aSimpleNotifiableEvent(canBeReplaced = false)
        val updatedEvent = nonReplaceableEvent.copy(title = "updated title")
        val queue = givenQueue(listOf(nonReplaceableEvent))

        queue.add(updatedEvent)

        queue.rawEvents() shouldBeEqualTo listOf(nonReplaceableEvent)
    }

    @Test
    fun `given event when adding new event with edited event id matching the existing event id then updates existing event`() {
        val editedEvent = aSimpleNotifiableEvent(eventId = "id-to-edit")
        val updatedEvent = editedEvent.copy(eventId = "1", editedEventId = "id-to-edit", title = "updated title", isUpdated = true)
        val queue = givenQueue(listOf(editedEvent))

        queue.add(updatedEvent)

        queue.rawEvents() shouldBeEqualTo listOf(updatedEvent)
    }

    @Test
    fun `given event when adding new event with edited event id matching the existing event edited id then updates existing event`() {
        val editedEvent = aSimpleNotifiableEvent(eventId = "0", editedEventId = "id-to-edit")
        val updatedEvent = editedEvent.copy(eventId = "1", editedEventId = "id-to-edit", title = "updated title", isUpdated = true)
        val queue = givenQueue(listOf(editedEvent))

        queue.add(updatedEvent)

        queue.rawEvents() shouldBeEqualTo listOf(updatedEvent)
    }

    @Test
    fun `when clearing membership notification then removes invite events with matching room id`() {
        val roomId = "a-room-id"
        val queue = givenQueue(
                listOf(
                        anInviteNotifiableEvent(roomId = roomId),
                        aNotifiableMessageEvent(roomId = roomId)
                )
        )

        queue.clearMemberShipNotificationForRoom(roomId)

        queue.rawEvents() shouldBeEqualTo listOf(aNotifiableMessageEvent(roomId = roomId))
    }

    @Test
    fun `when clearing messages for room then removes message events with matching room id`() {
        val roomId = "a-room-id"
        val queue = givenQueue(
                listOf(
                        anInviteNotifiableEvent(roomId = roomId),
                        aNotifiableMessageEvent(roomId = roomId)
                )
        )

        queue.clearMessagesForRoom(roomId)

        queue.rawEvents() shouldBeEqualTo listOf(anInviteNotifiableEvent(roomId = roomId))
    }

    private fun givenQueue(events: List<NotifiableEvent>) = NotificationEventQueue(events.toMutableList(), seenEventIds = seenIdsCache)
}
