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

import android.app.Notification
import androidx.core.app.NotificationCompat
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

/**
 * ======== Build summary notification =========
 * On Android 7.0 (API level 24) and higher, the system automatically builds a summary for
 * your group using snippets of text from each notification. The user can expand this
 * notification to see each separate notification.
 * To support older versions, which cannot show a nested group of notifications,
 * you must create an extra notification that acts as the summary.
 * This appears as the only notification and the system hides all the others.
 * So this summary should include a snippet from all the other notifications,
 * which the user can tap to open your app.
 * The behavior of the group summary may vary on some device types such as wearables.
 * To ensure the best experience on all devices and versions, always include a group summary when you create a group
 * https://developer.android.com/training/notify-user/group
 */
class SummaryGroupMessageCreator @Inject constructor(
        private val stringProvider: StringProvider,
        private val notificationUtils: NotificationUtils
) {

    fun createSummaryNotification(roomNotifications: List<RoomNotification.Message.Meta>,
                                  invitationNotifications: List<OneShotNotification.Append.Meta>,
                                  simpleNotifications: List<OneShotNotification.Append.Meta>,
                                  useCompleteNotificationFormat: Boolean): Notification {
        val summaryInboxStyle = NotificationCompat.InboxStyle().also { style ->
            roomNotifications.forEach { style.addLine(it.summaryLine) }
            invitationNotifications.forEach { style.addLine(it.summaryLine) }
            simpleNotifications.forEach { style.addLine(it.summaryLine) }
        }

        val summaryIsNoisy = roomNotifications.any { it.shouldBing } ||
                invitationNotifications.any { it.isNoisy } ||
                simpleNotifications.any { it.isNoisy }

        val messageCount = roomNotifications.fold(initial = 0) { acc, current -> acc + current.messageCount }

        val lastMessageTimestamp = roomNotifications.lastOrNull()?.latestTimestamp
                ?: invitationNotifications.lastOrNull()?.timestamp
                ?: simpleNotifications.last().timestamp

        // FIXME roomIdToEventMap.size is not correct, this is the number of rooms
        val nbEvents = roomNotifications.size + simpleNotifications.size
        val sumTitle = stringProvider.getQuantityString(R.plurals.notification_compat_summary_title, nbEvents, nbEvents)
        summaryInboxStyle.setBigContentTitle(sumTitle)
                // TODO get latest event?
                .setSummaryText(stringProvider.getQuantityString(R.plurals.notification_unread_notified_messages, nbEvents, nbEvents))
        return if (useCompleteNotificationFormat) {
            notificationUtils.buildSummaryListNotification(
                    summaryInboxStyle,
                    sumTitle,
                    noisy = summaryIsNoisy,
                    lastMessageTimestamp = lastMessageTimestamp
            )
        } else {
            processSimpleGroupSummary(
                    summaryIsNoisy,
                    messageCount,
                    simpleNotifications.size,
                    invitationNotifications.size,
                    roomNotifications.size,
                    lastMessageTimestamp
            )
        }
    }

    private fun processSimpleGroupSummary(summaryIsNoisy: Boolean,
                                          messageEventsCount: Int,
                                          simpleEventsCount: Int,
                                          invitationEventsCount: Int,
                                          roomCount: Int,
                                          lastMessageTimestamp: Long): Notification {
        // Add the simple events as message (?)
        val messageNotificationCount = messageEventsCount + simpleEventsCount

        val privacyTitle = if (invitationEventsCount > 0) {
            val invitationsStr = stringProvider.getQuantityString(R.plurals.notification_invitations, invitationEventsCount, invitationEventsCount)
            if (messageNotificationCount > 0) {
                // Invitation and message
                val messageStr = stringProvider.getQuantityString(R.plurals.room_new_messages_notification,
                        messageNotificationCount, messageNotificationCount)
                if (roomCount > 1) {
                    // In several rooms
                    val roomStr = stringProvider.getQuantityString(R.plurals.notification_unread_notified_messages_in_room_rooms,
                            roomCount, roomCount)
                    stringProvider.getString(
                            R.string.notification_unread_notified_messages_in_room_and_invitation,
                            messageStr,
                            roomStr,
                            invitationsStr
                    )
                } else {
                    // In one room
                    stringProvider.getString(
                            R.string.notification_unread_notified_messages_and_invitation,
                            messageStr,
                            invitationsStr
                    )
                }
            } else {
                // Only invitation
                invitationsStr
            }
        } else {
            // No invitation, only messages
            val messageStr = stringProvider.getQuantityString(R.plurals.room_new_messages_notification,
                    messageNotificationCount, messageNotificationCount)
            if (roomCount > 1) {
                // In several rooms
                val roomStr = stringProvider.getQuantityString(R.plurals.notification_unread_notified_messages_in_room_rooms, roomCount, roomCount)
                stringProvider.getString(R.string.notification_unread_notified_messages_in_room, messageStr, roomStr)
            } else {
                // In one room
                messageStr
            }
        }
        return notificationUtils.buildSummaryListNotification(
                style = null,
                compatSummary = privacyTitle,
                noisy = summaryIsNoisy,
                lastMessageTimestamp = lastMessageTimestamp
        )
    }
}
