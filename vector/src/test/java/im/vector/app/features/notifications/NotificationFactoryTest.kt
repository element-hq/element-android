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

import im.vector.app.test.fakes.FakeNotificationUtils
import im.vector.app.test.fakes.FakeRoomGroupMessageCreator
import im.vector.app.test.fakes.FakeSummaryGroupMessageCreator
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
        val roomInvitation = mapOf(A_ROOM_ID to AN_INVITATION_EVENT)

        val result = roomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(OneShotNotification.Append(
                notification = expectedNotification,
                meta = OneShotNotification.Append.Meta(
                        key = A_ROOM_ID,
                        summaryLine = AN_INVITATION_EVENT.description,
                        isNoisy = AN_INVITATION_EVENT.noisy
                ))
        )
    }

    @Test
    fun `given a missing event in room invitation when mapping to notification then is Removed`() = testWith(notificationFactory) {
        val missingEventRoomInvitation: Map<String, InviteNotifiableEvent?> = mapOf(A_ROOM_ID to null)

        val result = missingEventRoomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(OneShotNotification.Removed(
                key = A_ROOM_ID
        ))
    }

    @Test
    fun `given a simple event when mapping to notification then is Append`() = testWith(notificationFactory) {
        val expectedNotification = notificationUtils.givenBuildSimpleInvitationNotificationFor(A_SIMPLE_EVENT, MY_USER_ID)
        val roomInvitation = mapOf(AN_EVENT_ID to A_SIMPLE_EVENT)

        val result = roomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(OneShotNotification.Append(
                notification = expectedNotification,
                meta = OneShotNotification.Append.Meta(
                        key = AN_EVENT_ID,
                        summaryLine = A_SIMPLE_EVENT.description,
                        isNoisy = A_SIMPLE_EVENT.noisy
                ))
        )
    }

    @Test
    fun `given a missing simple event when mapping to notification then is Removed`() = testWith(notificationFactory) {
        val missingEventRoomInvitation: Map<String, SimpleNotifiableEvent?> = mapOf(AN_EVENT_ID to null)

        val result = missingEventRoomInvitation.toNotifications(MY_USER_ID)

        result shouldBeEqualTo listOf(OneShotNotification.Removed(
                key = AN_EVENT_ID
        ))
    }

    @Test
    fun `given room with message when mapping to notification then delegates to room group message creator`() = testWith(notificationFactory) {
        val events = listOf(A_MESSAGE_EVENT)
        val expectedNotification = roomGroupMessageCreator.givenCreatesRoomMessageFor(events, A_ROOM_ID, MY_USER_ID, MY_AVATAR_URL)
        val roomWithMessage = mapOf(A_ROOM_ID to events)

        val result = roomWithMessage.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(expectedNotification)
    }

    @Test
    fun `given a room with no events to display when mapping to notification then is Empty`() = testWith(notificationFactory) {
        val emptyRoom: Map<String, List<NotifiableMessageEvent>> = mapOf(A_ROOM_ID to emptyList())

        val result = emptyRoom.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(RoomNotification.Removed(
                roomId = A_ROOM_ID
        ))
    }

    @Test
    fun `given a room with only redacted events when mapping to notification then is Empty`() = testWith(notificationFactory) {
        val redactedRoom = mapOf(A_ROOM_ID to listOf(A_MESSAGE_EVENT.copy(isRedacted = true)))

        val result = redactedRoom.toNotifications(MY_USER_ID, MY_AVATAR_URL)

        result shouldBeEqualTo listOf(RoomNotification.Removed(
                roomId = A_ROOM_ID
        ))
    }
}

fun <T> testWith(receiver: T, block: T.() -> Unit) {
    receiver.block()
}
