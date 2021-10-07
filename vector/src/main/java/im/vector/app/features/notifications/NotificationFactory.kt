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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import javax.inject.Inject

class NotificationFactory @Inject constructor(
        private val notificationUtils: NotificationUtils,
        private val roomGroupMessageCreator: RoomGroupMessageCreator,
        private val summaryGroupMessageCreator: SummaryGroupMessageCreator
) {

    fun Map<String, List<NotifiableMessageEvent>>.toNotifications(myUserDisplayName: String, myUserAvatarUrl: String?): List<RoomNotification> {
        return this.map { (roomId, events) ->
            when {
                events.hasNoEventsToDisplay() -> RoomNotification.Removed(roomId)
                else                          -> roomGroupMessageCreator.createRoomMessage(events, roomId, myUserDisplayName, myUserAvatarUrl)
            }
        }
    }

    private fun List<NotifiableMessageEvent>.hasNoEventsToDisplay() = isEmpty() || all { it.canNotBeDisplayed() }

    private fun NotifiableMessageEvent.canNotBeDisplayed() = isRedacted

    fun Map<String, InviteNotifiableEvent?>.toNotifications(myUserId: String): List<OneShotNotification> {
        return this.map { (roomId, event) ->
            when (event) {
                null -> OneShotNotification.Removed(key = roomId)
                else -> OneShotNotification.Append(
                        notificationUtils.buildRoomInvitationNotification(event, myUserId),
                        OneShotNotification.Append.Meta(key = roomId, summaryLine = event.description, isNoisy = event.noisy)
                )
            }
        }
    }

    @JvmName("toNotificationsSimpleNotifiableEvent")
    fun Map<String, SimpleNotifiableEvent?>.toNotifications(myUserId: String): List<OneShotNotification> {
        return this.map { (eventId, event) ->
            when (event) {
                null -> OneShotNotification.Removed(key = eventId)
                else -> OneShotNotification.Append(
                        notificationUtils.buildSimpleEventNotification(event, myUserId),
                        OneShotNotification.Append.Meta(key = eventId, summaryLine = event.description, isNoisy = event.noisy)
                )
            }
        }
    }

    fun createSummaryNotification(roomNotifications: List<RoomNotification>,
                                  invitationNotifications: List<OneShotNotification>,
                                  simpleNotifications: List<OneShotNotification>,
                                  useCompleteNotificationFormat: Boolean): Notification {
        return summaryGroupMessageCreator.createSummaryNotification(
                roomNotifications = roomNotifications.mapToMeta(),
                invitationNotifications = invitationNotifications.mapToMeta(),
                simpleNotifications = simpleNotifications.mapToMeta(),
                useCompleteNotificationFormat = useCompleteNotificationFormat
        )
    }
}

private fun List<RoomNotification>.mapToMeta() = filterIsInstance<RoomNotification.Message>().map { it.meta }

@JvmName("mapToMetaOneShotNotification")
private fun List<OneShotNotification>.mapToMeta() = filterIsInstance<OneShotNotification.Append>().map { it.meta }

sealed interface RoomNotification {
    data class Removed(val roomId: String) : RoomNotification
    data class Message(val notification: Notification, val shortcutInfo: ShortcutInfoCompat?, val meta: Meta) : RoomNotification {
        data class Meta(
                val summaryLine: CharSequence,
                val messageCount: Int,
                val latestTimestamp: Long,
                val roomId: String,
                val shouldBing: Boolean
        )
    }
}

sealed interface OneShotNotification {
    data class Removed(val key: String) : OneShotNotification
    data class Append(val notification: Notification, val meta: Meta) : OneShotNotification {
        data class Meta(
                val key: String,
                val summaryLine: CharSequence,
                val isNoisy: Boolean
        )
    }
}
