/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.app.Notification
import javax.inject.Inject

private typealias ProcessedMessageEvents = List<ProcessedEvent<NotifiableMessageEvent>>
private typealias ProcessedJitsiEvents = List<ProcessedEvent<NotifiableJitsiEvent>>

class NotificationFactory @Inject constructor(
        private val notificationUtils: NotificationUtils,
        private val roomGroupMessageCreator: RoomGroupMessageCreator,
        private val summaryGroupMessageCreator: SummaryGroupMessageCreator
) {

    fun Map<String, ProcessedJitsiEvents>.toNotifications(): List<JitsiNotification> {
        return map { (roomId, events) ->
            JitsiNotification.IncomingCall(
                    roomId = roomId,
                    eventId = events.firstOrNull()?.event?.eventId.orEmpty(),
                    roomName = events.firstOrNull()?.event?.roomName.orEmpty(),
                    notification = notificationUtils.buildIncomingJitsiCallNotification(
                            callId = events.firstOrNull()?.event?.eventId.orEmpty().ifEmpty { roomId },
                            signalingRoomId = roomId,
                            title = events.firstOrNull()?.event?.roomName.orEmpty(),
                            fromBg = true,
                    )
            )
        }
    }

    fun Map<String, ProcessedMessageEvents>.toNotifications(myUserDisplayName: String, myUserAvatarUrl: String?): List<RoomNotification> {
        return map { (roomId, events) ->
            when {
                events.hasNoEventsToDisplay() -> RoomNotification.Removed(roomId)
                else -> {
                    val messageEvents = events.onlyKeptEvents().filterNot { it.isRedacted }
                    roomGroupMessageCreator.createRoomMessage(messageEvents, roomId, myUserDisplayName, myUserAvatarUrl)
                }
            }
        }
    }

    private fun ProcessedMessageEvents.hasNoEventsToDisplay() = isEmpty() || all {
        it.type == ProcessedEvent.Type.REMOVE || it.event.canNotBeDisplayed()
    }

    private fun NotifiableMessageEvent.canNotBeDisplayed() = isRedacted

    @JvmName("toNotificationsInviteNotifiableEvent")
    fun List<ProcessedEvent<InviteNotifiableEvent>>.toNotifications(myUserId: String): List<OneShotNotification> {
        return map { (processed, event) ->
            when (processed) {
                ProcessedEvent.Type.REMOVE -> OneShotNotification.Removed(key = event.roomId)
                ProcessedEvent.Type.KEEP -> OneShotNotification.Append(
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
    fun List<ProcessedEvent<SimpleNotifiableEvent>>.toNotifications(myUserId: String): List<OneShotNotification> {
        return map { (processed, event) ->
            when (processed) {
                ProcessedEvent.Type.REMOVE -> OneShotNotification.Removed(key = event.eventId)
                ProcessedEvent.Type.KEEP -> OneShotNotification.Append(
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

    fun createSummaryNotification(
            roomNotifications: List<RoomNotification>,
            invitationNotifications: List<OneShotNotification>,
            simpleNotifications: List<OneShotNotification>,
            useCompleteNotificationFormat: Boolean
    ): SummaryNotification {
        val roomMeta = roomNotifications.filterIsInstance<RoomNotification.Message>().map { it.meta }
        val invitationMeta = invitationNotifications.filterIsInstance<OneShotNotification.Append>().map { it.meta }
        val simpleMeta = simpleNotifications.filterIsInstance<OneShotNotification.Append>().map { it.meta }
        return when {
            roomMeta.isEmpty() && invitationMeta.isEmpty() && simpleMeta.isEmpty() -> SummaryNotification.Removed
            else -> SummaryNotification.Update(
                    summaryGroupMessageCreator.createSummaryNotification(
                            roomNotifications = roomMeta,
                            invitationNotifications = invitationMeta,
                            simpleNotifications = simpleMeta,
                            useCompleteNotificationFormat = useCompleteNotificationFormat
                    )
            )
        }
    }
}

sealed interface RoomNotification {
    data class Removed(val roomId: String) : RoomNotification
    data class Message(val notification: Notification, val meta: Meta) : RoomNotification {
        data class Meta(
                val summaryLine: CharSequence,
                val messageCount: Int,
                val latestTimestamp: Long,
                val roomId: String,
                val shouldBing: Boolean
        )
    }
}

sealed interface JitsiNotification {
    data class IncomingCall(
            val roomId: String,
            val eventId: String,
            val roomName: String,
            val notification: Notification,
    ) : JitsiNotification
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
