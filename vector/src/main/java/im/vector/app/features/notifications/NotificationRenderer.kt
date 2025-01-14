/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import android.content.Context
import androidx.annotation.WorkerThread
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_EVENT_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_INVITATION_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_MESSAGES_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.SUMMARY_NOTIFICATION_ID
import timber.log.Timber
import javax.inject.Inject

class NotificationRenderer @Inject constructor(
        private val notificationDisplayer: NotificationDisplayer,
        private val notificationFactory: NotificationFactory,
        private val appContext: Context
) {

    @WorkerThread
    fun render(
            myUserId: String,
            myUserDisplayName: String,
            myUserAvatarUrl: String?,
            useCompleteNotificationFormat: Boolean,
            eventsToProcess: List<ProcessedEvent<NotifiableEvent>>
    ) {
        val (roomEvents, simpleEvents, invitationEvents) = eventsToProcess.groupByType()
        with(notificationFactory) {
            val roomNotifications = roomEvents.toNotifications(myUserDisplayName, myUserAvatarUrl)
            val invitationNotifications = invitationEvents.toNotifications(myUserId)
            val simpleNotifications = simpleEvents.toNotifications(myUserId)
            val summaryNotification = createSummaryNotification(
                    roomNotifications = roomNotifications,
                    invitationNotifications = invitationNotifications,
                    simpleNotifications = simpleNotifications,
                    useCompleteNotificationFormat = useCompleteNotificationFormat
            )

            // Remove summary first to avoid briefly displaying it after dismissing the last notification
            if (summaryNotification == SummaryNotification.Removed) {
                Timber.d("Removing summary notification")
                notificationDisplayer.cancelNotificationMessage(null, SUMMARY_NOTIFICATION_ID)
            }

            roomNotifications.forEach { wrapper ->
                when (wrapper) {
                    is RoomNotification.Removed -> {
                        Timber.d("Removing room messages notification ${wrapper.roomId}")
                        notificationDisplayer.cancelNotificationMessage(wrapper.roomId, ROOM_MESSAGES_NOTIFICATION_ID)
                    }
                    is RoomNotification.Message -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating room messages notification ${wrapper.meta.roomId}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.roomId, ROOM_MESSAGES_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            invitationNotifications.forEach { wrapper ->
                when (wrapper) {
                    is OneShotNotification.Removed -> {
                        Timber.d("Removing invitation notification ${wrapper.key}")
                        notificationDisplayer.cancelNotificationMessage(wrapper.key, ROOM_INVITATION_NOTIFICATION_ID)
                    }
                    is OneShotNotification.Append -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating invitation notification ${wrapper.meta.key}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.key, ROOM_INVITATION_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            simpleNotifications.forEach { wrapper ->
                when (wrapper) {
                    is OneShotNotification.Removed -> {
                        Timber.d("Removing simple notification ${wrapper.key}")
                        notificationDisplayer.cancelNotificationMessage(wrapper.key, ROOM_EVENT_NOTIFICATION_ID)
                    }
                    is OneShotNotification.Append -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating simple notification ${wrapper.meta.key}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.key, ROOM_EVENT_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            // Update summary last to avoid briefly displaying it before other notifications
            if (summaryNotification is SummaryNotification.Update) {
                Timber.d("Updating summary notification")
                notificationDisplayer.showNotificationMessage(null, SUMMARY_NOTIFICATION_ID, summaryNotification.notification)
            }
        }
    }
}

private fun List<ProcessedEvent<NotifiableEvent>>.groupByType(): GroupedNotificationEvents {
    val roomIdToEventMap: MutableMap<String, MutableList<ProcessedEvent<NotifiableMessageEvent>>> = LinkedHashMap()
    val simpleEvents: MutableList<ProcessedEvent<SimpleNotifiableEvent>> = ArrayList()
    val invitationEvents: MutableList<ProcessedEvent<InviteNotifiableEvent>> = ArrayList()
    forEach {
        when (val event = it.event) {
            is InviteNotifiableEvent -> invitationEvents.add(it.castedToEventType())
            is NotifiableMessageEvent -> {
                val roomEvents = roomIdToEventMap.getOrPut(event.roomId) { ArrayList() }
                roomEvents.add(it.castedToEventType())
            }
            is SimpleNotifiableEvent -> simpleEvents.add(it.castedToEventType())
        }
    }
    return GroupedNotificationEvents(roomIdToEventMap, simpleEvents, invitationEvents)
}

@Suppress("UNCHECKED_CAST")
private fun <T : NotifiableEvent> ProcessedEvent<NotifiableEvent>.castedToEventType(): ProcessedEvent<T> = this as ProcessedEvent<T>

data class GroupedNotificationEvents(
        val roomEvents: Map<String, List<ProcessedEvent<NotifiableMessageEvent>>>,
        val simpleEvents: List<ProcessedEvent<SimpleNotifiableEvent>>,
        val invitationEvents: List<ProcessedEvent<InviteNotifiableEvent>>
)
