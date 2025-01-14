/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.app.Notification
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeNotificationDisplayer
import im.vector.app.test.fakes.FakeNotificationFactory
import io.mockk.mockk
import org.junit.Test

private const val MY_USER_ID = "my-user-id"
private const val MY_USER_DISPLAY_NAME = "display-name"
private const val MY_USER_AVATAR_URL = "avatar-url"
private const val AN_EVENT_ID = "event-id"
private const val A_ROOM_ID = "room-id"
private const val USE_COMPLETE_NOTIFICATION_FORMAT = true

private val AN_EVENT_LIST = listOf<ProcessedEvent<NotifiableEvent>>()
private val A_PROCESSED_EVENTS = GroupedNotificationEvents(emptyMap(), emptyList(), emptyList())
private val A_SUMMARY_NOTIFICATION = SummaryNotification.Update(mockk())
private val A_REMOVE_SUMMARY_NOTIFICATION = SummaryNotification.Removed
private val A_NOTIFICATION = mockk<Notification>()
private val MESSAGE_META = RoomNotification.Message.Meta(
        summaryLine = "ignored", messageCount = 1, latestTimestamp = -1, roomId = A_ROOM_ID, shouldBing = false
)
private val ONE_SHOT_META = OneShotNotification.Append.Meta(key = "ignored", summaryLine = "ignored", isNoisy = false, timestamp = -1)

class NotificationRendererTest {

    private val context = FakeContext()
    private val notificationDisplayer = FakeNotificationDisplayer()
    private val notificationFactory = FakeNotificationFactory()

    private val notificationRenderer = NotificationRenderer(
            notificationDisplayer = notificationDisplayer.instance,
            notificationFactory = notificationFactory.instance,
            appContext = context.instance
    )

    @Test
    fun `given no notifications when rendering then cancels summary notification`() {
        givenNoNotifications()

        renderEventsAsNotifications()

        notificationDisplayer.verifySummaryCancelled()
        notificationDisplayer.verifyNoOtherInteractions()
    }

    @Test
    fun `given last room message group notification is removed when rendering then remove the summary and then remove message notification`() {
        givenNotifications(roomNotifications = listOf(RoomNotification.Removed(A_ROOM_ID)), summaryNotification = A_REMOVE_SUMMARY_NOTIFICATION)

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID)
            cancelNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_MESSAGES_NOTIFICATION_ID)
        }
    }

    @Test
    fun `given a room message group notification is removed when rendering then remove the message notification and update summary`() {
        givenNotifications(roomNotifications = listOf(RoomNotification.Removed(A_ROOM_ID)))

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_MESSAGES_NOTIFICATION_ID)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    @Test
    fun `given a room message group notification is added when rendering then show the message notification and update summary`() {
        givenNotifications(
                roomNotifications = listOf(
                        RoomNotification.Message(
                                A_NOTIFICATION,
                                MESSAGE_META
                        )
                )
        )

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            showNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_MESSAGES_NOTIFICATION_ID, A_NOTIFICATION)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    @Test
    fun `given last simple notification is removed when rendering then remove the summary and then remove simple notification`() {
        givenNotifications(simpleNotifications = listOf(OneShotNotification.Removed(AN_EVENT_ID)), summaryNotification = A_REMOVE_SUMMARY_NOTIFICATION)

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID)
            cancelNotificationMessage(tag = AN_EVENT_ID, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID)
        }
    }

    @Test
    fun `given a simple notification is removed when rendering then remove the simple notification and update summary`() {
        givenNotifications(simpleNotifications = listOf(OneShotNotification.Removed(AN_EVENT_ID)))

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = AN_EVENT_ID, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    @Test
    fun `given a simple notification is added when rendering then show the simple notification and update summary`() {
        givenNotifications(
                simpleNotifications = listOf(
                        OneShotNotification.Append(
                                A_NOTIFICATION,
                                ONE_SHOT_META.copy(key = AN_EVENT_ID)
                        )
                )
        )

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            showNotificationMessage(tag = AN_EVENT_ID, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID, A_NOTIFICATION)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    @Test
    fun `given last invitation notification is removed when rendering then remove the summary and then remove invitation notification`() {
        givenNotifications(invitationNotifications = listOf(OneShotNotification.Removed(A_ROOM_ID)), summaryNotification = A_REMOVE_SUMMARY_NOTIFICATION)

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID)
            cancelNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_INVITATION_NOTIFICATION_ID)
        }
    }

    @Test
    fun `given an invitation notification is removed when rendering then remove the invitation notification and update summary`() {
        givenNotifications(invitationNotifications = listOf(OneShotNotification.Removed(A_ROOM_ID)))

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            cancelNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_INVITATION_NOTIFICATION_ID)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    @Test
    fun `given an invitation notification is added when rendering then show the invitation notification and update summary`() {
        givenNotifications(
                simpleNotifications = listOf(
                        OneShotNotification.Append(
                                A_NOTIFICATION,
                                ONE_SHOT_META.copy(key = A_ROOM_ID)
                        )
                )
        )

        renderEventsAsNotifications()

        notificationDisplayer.verifyInOrder {
            showNotificationMessage(tag = A_ROOM_ID, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID, A_NOTIFICATION)
            showNotificationMessage(tag = null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, A_SUMMARY_NOTIFICATION.notification)
        }
    }

    private fun renderEventsAsNotifications() {
        notificationRenderer.render(
                myUserId = MY_USER_ID,
                myUserDisplayName = MY_USER_DISPLAY_NAME,
                myUserAvatarUrl = MY_USER_AVATAR_URL,
                useCompleteNotificationFormat = USE_COMPLETE_NOTIFICATION_FORMAT,
                eventsToProcess = AN_EVENT_LIST
        )
    }

    private fun givenNoNotifications() {
        givenNotifications(emptyList(), emptyList(), emptyList(), USE_COMPLETE_NOTIFICATION_FORMAT, A_REMOVE_SUMMARY_NOTIFICATION)
    }

    private fun givenNotifications(
            roomNotifications: List<RoomNotification> = emptyList(),
            invitationNotifications: List<OneShotNotification> = emptyList(),
            simpleNotifications: List<OneShotNotification> = emptyList(),
            useCompleteNotificationFormat: Boolean = USE_COMPLETE_NOTIFICATION_FORMAT,
            summaryNotification: SummaryNotification = A_SUMMARY_NOTIFICATION
    ) {
        notificationFactory.givenNotificationsFor(
                groupedEvents = A_PROCESSED_EVENTS,
                myUserId = MY_USER_ID,
                myUserDisplayName = MY_USER_DISPLAY_NAME,
                myUserAvatarUrl = MY_USER_AVATAR_URL,
                useCompleteNotificationFormat = useCompleteNotificationFormat,
                roomNotifications = roomNotifications,
                invitationNotifications = invitationNotifications,
                simpleNotifications = simpleNotifications,
                summaryNotification = summaryNotification
        )
    }
}
