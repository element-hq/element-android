/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import im.vector.app.features.notifications.ProcessedEvent.Type
import im.vector.app.test.fakes.FakeNotificationUtils
import im.vector.app.test.fakes.FakeRoomGroupMessageCreator
import im.vector.app.test.fakes.FakeSummaryGroupMessageCreator
import im.vector.app.test.fixtures.aNotifiableMessageEvent
import im.vector.app.test.fixtures.aSimpleNotifiableEvent
import im.vector.app.test.fixtures.anInviteNotifiableEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val MY_USER_ID = "user-id"
private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"

private val MY_AVATAR_URL: String? = null
private val AN_INVITATION_EVENT = anInviteNotifiableEvent(roomId = A_ROOM_ID)
private val A_SIMPLE_EVENT = aSimpleNotifiableEvent(eventId = AN_EVENT_ID)
private val A_MESSAGE_EVENT = aNotifiableMessageEvent(eventId = AN_EVENT_ID, roomId = A_ROOM_ID)

class NotificationFactoryTest {

    private val notificationUtils = FakeNotificationUtils()
    private val roomGroupMessageCreator = FakeRoomGroupMessageCreator()
    private val summaryGroupMessageCreator = FakeSummaryGroupMessageCreator()

    private val notificationFactory = NotificationFactory(
            notificationUtils.instance,
            roomGroupMessageCreator.instance,
            summaryGroupMessageCreator.instance
    )

    @Test
    fun `given a room invitation when mapping to notification then is Append`() = testWith(notificationFactory) {
        val expectedNotification = notificationUtils.givenBuildRoomInvitationNotificationFor(AN_INVITATION_EVENT, MY_USER_ID)
        val roomInvitation = listOf(ProcessedEvent(Type.KEEP, AN_INVITATION_EVENT))

        val result = roomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(
                OneShotNotification.Append(
                        notification = expectedNotification,
                        meta = OneShotNotification.Append.Meta(
                                key = A_ROOM_ID,
                                summaryLine = AN_INVITATION_EVENT.description,
                                isNoisy = AN_INVITATION_EVENT.noisy,
                                timestamp = AN_INVITATION_EVENT.timestamp
                        )
                )
        )
    }

    @Test
    fun `given a missing event in room invitation when mapping to notification then is Removed`() = testWith(notificationFactory) {
        val missingEventRoomInvitation = listOf(ProcessedEvent(Type.REMOVE, AN_INVITATION_EVENT))

        val result = missingEventRoomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(
                OneShotNotification.Removed(
                        key = A_ROOM_ID
                )
        )
    }

    @Test
    fun `given a simple event when mapping to notification then is Append`() = testWith(notificationFactory) {
        val expectedNotification = notificationUtils.givenBuildSimpleInvitationNotificationFor(A_SIMPLE_EVENT, MY_USER_ID)
        val roomInvitation = listOf(ProcessedEvent(Type.KEEP, A_SIMPLE_EVENT))

        val result = roomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(
                OneShotNotification.Append(
                        notification = expectedNotification,
                        meta = OneShotNotification.Append.Meta(
                                key = AN_EVENT_ID,
                                summaryLine = A_SIMPLE_EVENT.description,
                                isNoisy = A_SIMPLE_EVENT.noisy,
                                timestamp = AN_INVITATION_EVENT.timestamp
                        )
                )
        )
    }

    @Test
    fun `given a missing simple event when mapping to notification then is Removed`() = testWith(notificationFactory) {
        val missingEventRoomInvitation = listOf(ProcessedEvent(Type.REMOVE, A_SIMPLE_EVENT))

        val result = missingEventRoomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(
                OneShotNotification.Removed(
                        key = AN_EVENT_ID
                )
        )
    }

    @Test
    fun `given room with message when mapping to notification then delegates to room group message creator`() = testWith(notificationFactory) {
        val events = listOf(A_MESSAGE_EVENT)
        val expectedNotification = roomGroupMessageCreator.givenCreatesRoomMessageFor(events, A_ROOM_ID, MY_USER_ID, MY_AVATAR_URL)
        val roomWithMessage = mapOf(A_ROOM_ID to listOf(ProcessedEvent(Type.KEEP, A_MESSAGE_EVENT)))

        val result = roomWithMessage.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(expectedNotification)
    }

    @Test
    fun `given a room with no events to display when mapping to notification then is Empty`() = testWith(notificationFactory) {
        val events = listOf(ProcessedEvent(Type.REMOVE, A_MESSAGE_EVENT))
        val emptyRoom = mapOf(A_ROOM_ID to events)

        val result = emptyRoom.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(
                RoomNotification.Removed(
                        roomId = A_ROOM_ID
                )
        )
    }

    @Test
    fun `given a room with only redacted events when mapping to notification then is Empty`() = testWith(notificationFactory) {
        val redactedRoom = mapOf(A_ROOM_ID to listOf(ProcessedEvent(Type.KEEP, A_MESSAGE_EVENT.copy(isRedacted = true))))

        val result = redactedRoom.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(
                RoomNotification.Removed(
                        roomId = A_ROOM_ID
                )
        )
    }

    @Test
    fun `given a room with redacted and non redacted message events when mapping to notification then redacted events are removed`() = testWith(
            notificationFactory
    ) {
        val roomWithRedactedMessage = mapOf(
                A_ROOM_ID to listOf(
                        ProcessedEvent(Type.KEEP, A_MESSAGE_EVENT.copy(isRedacted = true)),
                        ProcessedEvent(Type.KEEP, A_MESSAGE_EVENT.copy(eventId = "not-redacted"))
                )
        )
        val withRedactedRemoved = listOf(A_MESSAGE_EVENT.copy(eventId = "not-redacted"))
        val expectedNotification = roomGroupMessageCreator.givenCreatesRoomMessageFor(withRedactedRemoved, A_ROOM_ID, MY_USER_ID, MY_AVATAR_URL)

        val result = roomWithRedactedMessage.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(expectedNotification)
    }
}

fun <T> testWith(receiver: T, block: T.() -> Unit) {
    receiver.block()
}
