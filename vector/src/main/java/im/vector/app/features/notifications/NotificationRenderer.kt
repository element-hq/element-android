/*
 * Copyright 2019 New Vector Ltd
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

import android.content.Context
import androidx.annotation.WorkerThread
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_EVENT_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_INVITATION_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.ROOM_MESSAGES_NOTIFICATION_ID
import im.vector.app.features.notifications.NotificationDrawerManager.Companion.SUMMARY_NOTIFICATION_ID
import androidx.core.content.pm.ShortcutManagerCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRenderer @Inject constructor(private val notifiableEventProcessor: NotifiableEventProcessor,
                                               private val notificationDisplayer: NotificationDisplayer,
                                               private val notificationFactory: NotificationFactory,
                                               private val appContext: Context) {

    private var lastKnownEventList = -1

    @WorkerThread
    fun render(currentRoomId: String?,
               myUserId: String,
               myUserDisplayName: String,
               myUserAvatarUrl: String?,
               useCompleteNotificationFormat: Boolean,
               eventList: MutableList<NotifiableEvent>) {
        Timber.v("Render notification events - count: ${eventList.size}")
        val notificationEvents = notifiableEventProcessor.modifyAndProcess(eventList, currentRoomId)
        if (lastKnownEventList == notificationEvents.hashCode()) {
            Timber.d("Skipping notification update due to event list not changing")
        } else {
            processEvents(notificationEvents, myUserId, myUserDisplayName, myUserAvatarUrl, useCompleteNotificationFormat)
            lastKnownEventList = notificationEvents.hashCode()
        }
    }

    private fun processEvents(notificationEvents: ProcessedNotificationEvents,
                              myUserId: String,
                              myUserDisplayName: String,
                              myUserAvatarUrl: String?,
                              useCompleteNotificationFormat: Boolean) {
        val (roomEvents, simpleEvents, invitationEvents) = notificationEvents
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
            when (summaryNotification) {
                SummaryNotification.Removed -> {
                    Timber.d("Removing summary notification")
                    notificationDisplayer.cancelNotificationMessage(null, SUMMARY_NOTIFICATION_ID)
                }
            }

            roomNotifications.forEach { wrapper ->
                when (wrapper) {
                    is RoomNotification.Removed -> {
                        Timber.d("Removing room messages notification ${wrapper.roomId}")
                        notificationDisplayer.cancelNotificationMessage(wrapper.roomId, ROOM_MESSAGES_NOTIFICATION_ID)
                    }
                    is RoomNotification.Message -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating room messages notification ${wrapper.meta.roomId}")
                        wrapper.shortcutInfo?.let {
                            ShortcutManagerCompat.pushDynamicShortcut(appContext, it)
                        }
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
                    is OneShotNotification.Append  -> if (useCompleteNotificationFormat) {
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
                    is OneShotNotification.Append  -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating simple notification ${wrapper.meta.key}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.key, ROOM_EVENT_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            // Update summary last to avoid briefly displaying it before other notifications
            when (summaryNotification) {
                is SummaryNotification.Update -> {
                    Timber.d("Updating summary notification")
                    notificationDisplayer.showNotificationMessage(null, SUMMARY_NOTIFICATION_ID, summaryNotification.notification)
                }
            }
        }
    }
}
