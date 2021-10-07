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

import androidx.annotation.WorkerThread
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRenderer @Inject constructor(private val notifiableEventProcessor: NotifiableEventProcessor,
                                               private val notificationDisplayer: NotificationDisplayer,
                                               private val notificationFactory: NotificationFactory) {

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

    private fun processEvents(notificationEvents: ProcessedNotificationEvents, myUserId: String, myUserDisplayName: String, myUserAvatarUrl: String?, useCompleteNotificationFormat: Boolean) {
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

            roomNotifications.forEach { wrapper ->
                when (wrapper) {
                    is RoomNotification.Removed -> notificationDisplayer.cancelNotificationMessage(wrapper.roomId, NotificationDrawerManager.ROOM_MESSAGES_NOTIFICATION_ID)
                    is RoomNotification.Message -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating room messages notification ${wrapper.meta.roomId}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.roomId, NotificationDrawerManager.ROOM_MESSAGES_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            invitationNotifications.forEach { wrapper ->
                when (wrapper) {
                    is OneShotNotification.Removed -> notificationDisplayer.cancelNotificationMessage(wrapper.key, NotificationDrawerManager.ROOM_INVITATION_NOTIFICATION_ID)
                    is OneShotNotification.Append  -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating invitation notification ${wrapper.meta.key}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.key, NotificationDrawerManager.ROOM_INVITATION_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            simpleNotifications.forEach { wrapper ->
                when (wrapper) {
                    is OneShotNotification.Removed -> notificationDisplayer.cancelNotificationMessage(wrapper.key, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID)
                    is OneShotNotification.Append  -> if (useCompleteNotificationFormat) {
                        Timber.d("Updating simple notification ${wrapper.meta.key}")
                        notificationDisplayer.showNotificationMessage(wrapper.meta.key, NotificationDrawerManager.ROOM_EVENT_NOTIFICATION_ID, wrapper.notification)
                    }
                }
            }

            when (summaryNotification) {
                SummaryNotification.Removed   -> {
                    Timber.d("Removing summary notification")
                    notificationDisplayer.cancelNotificationMessage(null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID)
                }
                is SummaryNotification.Update -> {
                    Timber.d("Updating summary notification")
                    notificationDisplayer.showNotificationMessage(null, NotificationDrawerManager.SUMMARY_NOTIFICATION_ID, summaryNotification.notification)
                }
            }
        }
    }
}
