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
import javax.inject.Inject

private typealias ProcessedMessageEvent = Pair<ProcessedType, NotifiableMessageEvent>

class NotificationFactory @Inject constructor(
        private val notificationUtils: NotificationUtils,
        private val roomGroupMessageCreator: RoomGroupMessageCreator,
        private val summaryGroupMessageCreator: SummaryGroupMessageCreator
) {

    fun Map<String, List<ProcessedMessageEvent>>.toNotifications(myUserDisplayName: String, myUserAvatarUrl: String?): List<RoomNotification> {
        return map { (roomId, events) ->
            when {
                events.hasNoEventsToDisplay() -> RoomNotification.Removed(roomId)
                else                          -> {
                    val messageEvents = events.filter { it.first == ProcessedType.KEEP }.map { it.second }
                    roomGroupMessageCreator.createRoomMessage(messageEvents, roomId, myUserDisplayName, myUserAvatarUrl)
                }
            }
        }
    }

    private fun List<Pair<ProcessedType, NotifiableMessageEvent>>.hasNoEventsToDisplay() = isEmpty() || all {
        it.first == ProcessedType.REMOVE || it.second.canNotBeDisplayed()
    }

    private fun NotifiableMessageEvent.canNotBeDisplayed() = isRedacted

    fun List<Pair<ProcessedType, InviteNotifiableEvent>>.toNotifications(myUserId: String): List<OneShotNotification> {
        return map { (processed, event) ->
            when (processed) {
                ProcessedType.REMOVE -> OneShotNotification.Removed(key = event.roomId)
                ProcessedType.KEEP   -> OneShotNotification.Append(
                        notificationUtils.buildRoomInvitationNotification(event, myUserId),
                        OneShotNotification.Append.Meta(
                                key = event.roomId,
                                summaryLine = event.description,
                                isNoisy = event.noisy,
                                timestamp = event.timestamp
                        )
                )
            }
        }
    }

    @JvmName("toNotificationsSimpleNotifiableEvent")
    fun List<Pair<ProcessedType, SimpleNotifiableEvent>>.toNotifications(myUserId: String): List<OneShotNotification> {
        return map { (processed, event) ->
            when (processed) {
                ProcessedType.REMOVE -> OneShotNotification.Removed(key = event.eventId)
                ProcessedType.KEEP   -> OneShotNotification.Append(
                        notificationUtils.buildSimpleEventNotification(event, myUserId),
                        OneShotNotification.Append.Meta(
                                key = event.eventId,
                                summaryLine = event.description,
                                isNoisy = event.noisy,
                                timestamp = event.timestamp
                        )
                )
            }
        }
    }

    fun createSummaryNotification(roomNotifications: List<RoomNotification>,
                                  invitationNotifications: List<OneShotNotification>,
                                  simpleNotifications: List<OneShotNotification>,
                                  useCompleteNotificationFormat: Boolean): SummaryNotification {
        val roomMeta = roomNotifications.filterIsInstance<RoomNotification.Message>().map { it.meta }
        val invitationMeta = invitationNotifications.filterIsInstance<OneShotNotification.Append>().map { it.meta }
        val simpleMeta = simpleNotifications.filterIsInstance<OneShotNotification.Append>().map { it.meta }
        return when {
            roomMeta.isEmpty() && invitationMeta.isEmpty() && simpleMeta.isEmpty() -> SummaryNotification.Removed
            else                                                                   -> SummaryNotification.Update(
                    summaryGroupMessageCreator.createSummaryNotification(
                            roomNotifications = roomMeta,
                            invitationNotifications = invitationMeta,
                            simpleNotifications = simpleMeta,
                            useCompleteNotificationFormat = useCompleteNotificationFormat
                    ))
        }
    }
}

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
                val isNoisy: Boolean,
                val timestamp: Long,
        )
    }
}

sealed interface SummaryNotification {
    object Removed : SummaryNotification
    data class Update(val notification: Notification) : SummaryNotification
}
